import java.lang.Thread;
import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.google.gson.*;
import com.google.gson.stream.*;

class ServerActions implements Runnable {

    boolean registered = false;

    Socket client;
    JsonReader in;
    OutputStream out;
    ServerControl registry;

    ServerActions ( Socket c, ServerControl r ) {
        client = c;
        registry = r;

        try {
            in = new JsonReader( new InputStreamReader ( c.getInputStream(), "UTF-8") );
            out = c.getOutputStream();
        } catch (Exception e) {
            System.err.print( "Cannot use client socket: " + e );
            Thread.currentThread().interrupt();
        }
    }

    JsonObject
    readCommand () {
        try {
            JsonElement data = new JsonParser().parse( in );
            if (data.isJsonObject()) {
                return data.getAsJsonObject();
            }
            System.err.print ( "Error while reading command from socket (not a JSON object), connection will be shutdown\n" );
            return null;
        } catch (Exception e) {
            System.err.print ( "Error while reading JSON command from socket, connection will be shutdown\n" );
            return null;
        }

    }

    void
    sendResult ( String result, String error ) {
        String msg = "{";

        // Usefull result

        if (result != null) {
            msg += result;
        }

        // error message

        if (error != null) {
            msg += "\"error\":" + error;
        }

        msg += "}\n";

        try {
            System.out.print( "Send result: " + msg );
            out.write ( msg.getBytes( StandardCharsets.UTF_8 ) );
        } catch (Exception e ) {}
    }

    void
    executeCommand ( JsonObject data ) {
        JsonElement cmd = data.get( "type" );
        UserDescription me;

        if (cmd == null) {
            System.err.println ( "Invalid command in request: " + data );
            return;
        }

        // CREATE

        if (cmd.getAsString().equals( "create" )) {
            JsonElement uuid = data.get( "uuid" );

            if (uuid == null) {
                System.err.print ( "No \"uuid\" field in \"create\" request: " + data );
                sendResult( null, "\"wrong request format\"" );
                return;
            }

            if (registry.userExists( uuid.getAsString() )) {
                System.err.println ( "User already exists: " + data );
                sendResult( null, "\"uuid already exists\"" );
                return;
            }

            data.remove ( "type" );
            me = registry.addUser( data );

            sendResult( "\"result\":\"" + me.id + "\"", null );
            return;
        }

        // LIST

        if (cmd.getAsString().equals( "list" )) {
            String list;
            int user = 0; // 0 means all users
            JsonElement id = data.get( "id" );

            if (id != null) {
                user = id.getAsInt();
            }

            System.out.println( "List " + (user == 0 ? "all users" : "user ") + user );

            list = registry.listUsers( user );

            sendResult( "\"data\":" + (list == null ? "[]" : list), null );
            return;
        }

        // NEW

        if (cmd.getAsString().equals( "new" )) {
            JsonElement id = data.get( "id" );
            int user = id == null ? -1 : id.getAsInt();

            if (id == null || user <= 0) {
                System.err.print ( "No valid \"id\" field in \"new\" request: " + data );
                sendResult( null, "\"wrong request format\"" );
                return;
            }

            sendResult( "\"result\":" + registry.userNewMessages( user ), null );
            return;
        }

        // ALL

        if (cmd.getAsString().equals( "all" )) {
            JsonElement id = data.get( "id" );
            int user = id == null ? -1 : id.getAsInt();

            if (id == null || user <= 0) {
                System.err.print ( "No valid \"id\" field in \"new\" request: " + data );
                sendResult( null, "\"wrong request format\"" );
                return;
            }

            sendResult( "\"result\":[" + registry.userAllMessages( user ) + "," +
                        registry.userSentMessages( user ) + "]", null );
            return;
        }

        // SEND

        if (cmd.getAsString().equals( "send" )) {
            JsonElement src = data.get( "src" );
            JsonElement dst = data.get( "dst" );
            JsonElement msg = data.get( "msg" );
            JsonElement copy = data.get( "copy" );

            if (src == null || dst == null || msg == null || copy == null) {
                System.err.print ( "Badly formated \"send\" request: " + data );
                sendResult( null, "\"wrong request format\"" );
                return;
            }

            int srcId = src.getAsInt();
            int dstId = dst.getAsInt();

            if (registry.userExists( srcId ) == false) {
                System.err.print ( "Unknown source id for \"send\" request: " + data );
                sendResult( null, "\"wrong parameters\"" );
                return;
            }

            if (registry.userExists( dstId ) == false) {
                System.err.print ( "Unknown destination id for \"send\" request: " + data );
                sendResult( null, "\"wrong parameters\"" );
                return;
            }

            // Save message and copy

            String response = registry.sendMessage( srcId, dstId,
                                                    msg.getAsString(),
                                                    copy.getAsString() );

            sendResult( "\"result\":" + response, null );
            return;
        }

        // RECV

        if (cmd.getAsString().equals( "recv" )) {
            JsonElement id = data.get( "id" );
            JsonElement msg = data.get( "msg" );

            if (id == null || msg == null) {
                System.err.print ( "Badly formated \"recv\" request: " + data );
                sendResult( null, "\"wrong request format\"" );
                return;
            }

            int fromId = id.getAsInt();

            if (registry.userExists( fromId ) == false) {
                System.err.print ( "Unknown source id for \"recv\" request: " + data );
                sendResult( null, "\"wrong parameters\"" );
                return;
            }

            if (registry.messageExists( fromId, msg.getAsString() ) == false &&
                registry.messageExists( fromId, "_" + msg.getAsString() ) == false) {
                System.err.println ( "Unknown message for \"recv\" request: " + data );
                sendResult( null, "\"wrong parameters\"" );
                return;
            }

            // Read message

            String response = registry.recvMessage( fromId, msg.getAsString() );

            sendResult( "\"result\":" + response, null );
            return;
        }

        // RECEIPT

        if (cmd.getAsString().equals( "receipt" )) {
            JsonElement id = data.get( "id" );
            JsonElement msg = data.get( "msg" );
            JsonElement receipt = data.get( "receipt" );

            if (id == null || msg == null || receipt == null) {
                System.err.print ( "Badly formated \"receipt\" request: " + data );
                sendResult( null, "\"wrong request format\"" );
                return;
            }

            int fromId = id.getAsInt();

            if (registry.messageWasRed( fromId, msg.getAsString() ) == false) {
                System.err.print ( "Unknown, or not yet red, message for \"receipt\" request: " + data );
                sendResult( null, "\"wrong parameters\"" );
                return;
            }

            // Store receipt

            registry.storeReceipt( fromId, msg.getAsString(), receipt.getAsString() );
            return;
        }

        // STATUS

        if (cmd.getAsString().equals( "status" )) {
            JsonElement id = data.get( "id" );
            JsonElement msg = data.get( "msg" );

            if (id == null || msg == null) {
                System.err.print ( "Badly formated \"status\" request: " + data );
                sendResult( null, "\"wrong request format\"" );
                return;
            }

            int fromId = id.getAsInt();

            if (registry.copyExists( fromId, msg.getAsString() ) == false) {
                System.err.print ( "Unknown message for \"status\" request: " + data );
                sendResult( null, "\"wrong parameters\"" );
                return;
            }

            // Get receipts

            String response = registry.getReceipts( fromId, msg.getAsString() );

            sendResult( "\"result\":" + response, null );
            return;
        }

        sendResult( null, "\"Unknown request\"" );
        return;
    }

    public void
    run () {
        while (true) {
            JsonObject cmd = readCommand();
            if (cmd == null) {
                try {
                    client.close();
                } catch (Exception e) {}
                return;
            }
            executeCommand ( cmd );
        }

    }

}

