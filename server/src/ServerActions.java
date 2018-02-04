import java.util.*;
import java.lang.Thread;
import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.google.gson.*;
import com.google.gson.stream.*;
import java.security.*;
import java.security.spec.*;
import java.security.cert.*;
import java.io.*;

class ServerActions implements Runnable {

    boolean registered = false;

    Socket client;
    JsonReader in;
    OutputStream out;
    ServerControl registry;
    DHSession session = null;
    CryServerOperations cry;
    String currUUID;
    String currNonce;

    ServerActions ( Socket c, ServerControl r ) throws Exception{
        client = c;
        registry = r;
        cry = new CryServerOperations();

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
        System.out.println("Reading Command...");
        try {
            JsonElement data = new JsonParser().parse( in );
            if (session == null && data.isJsonObject()){
                return data.getAsJsonObject();
            }
            if (data.isJsonObject()) {
                currNonce = data.getAsJsonObject().get("nonce").getAsString();
                System.out.println("currNonce " + currNonce);
                JsonObject r = cry.processPayloadRecv(data.getAsJsonObject(),session.getSharedSecret());
                return r;
            }
            System.err.print ( "Error while reading command from socket (not a JSON object), connection will be shutdown\n" );
            return null;
        } catch (Exception e) {
            System.err.print ( "Error while reading JSON command from socket, connection will be shutdown\n" );
            return null;
        }

    }

    void
    sendResult ( String result, String error, boolean sessionInit ) {
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
        System.out.println( "Send result: " + msg );
        try{
            if( !sessionInit ){
                String[] results = cry.processPayloadSend(msg, session.getSharedSecret());

                msg  = "{\"type\":\"payload\","+
                        "\"payload\":\""+results[0]+"\","+
                        "\"iv\":\""+results[1]+"\"," +
                        "\"mac\":\""+results[2]+"\","+
                        "\"nonce\":\""+currNonce+"\""+
                        "}";
            }
            System.out.println( "Send nonce: " + currNonce );
            out.write ( msg.getBytes( StandardCharsets.UTF_8 ) );
            System.out.println("Sent!!!");
        }catch (Exception e){
            System.err.print ( "Error while sending response to socket");
        }
    }

    void
    executeCommand ( JsonObject data ) {
        JsonElement cmd = data.get( "type" );
        UserDescription me;

        if (cmd == null) {
            System.err.println ( "Invalid command in request: " + data );
            return;
        }
        // SESSION

        if(cmd.getAsString().equals( "session" )){
            String ln = System.getProperty("line.separator");
            String cert = "";
            String sign = "";
            String pubk;
            System.out.println("Establish Session (Server)");
            try{
                pubk = data.get( "pubk" ).getAsString();
                session = new DHSession(pubk, data.get("subtype").getAsString());

                currUUID = data.get( "cert" ).getAsString();
                CertificateFactory fact = CertificateFactory.getInstance("X.509");
                X509Certificate cer = (X509Certificate) fact.generateCertificate(
                    new ByteArrayInputStream(currUUID.getBytes()));
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(cer.getEncoded());
                currUUID =  Base64.getEncoder().encodeToString(hash);

                pubk = session.getStringPubKey();
                cert = cry.getCertString();
                String toSign = pubk+ln+cert;
                sign = cry.sign(toSign);

                session.generateSecret();
            }catch(Exception e){
                System.err.print("Error Establishing Session " + e);
                return;
            }
            sendResult(    "\"Result\":\"Success\","+
                            "\"pubk\":\""+pubk+"\","+
                            "\"cert\":\""+cert+"\","+
                            "\"signature\":\""+sign+"\"",null, true);

            return;
        }

        // CREATE

        if (cmd.getAsString().equals( "create" )) {
            JsonElement uuid = data.get( "uuid" );

            if (uuid == null) {
                System.err.print ( "No \"uuid\" field in \"create\" request: " + data );
                sendResult( null, "\"wrong request format\"" , false);
                return;
            }

            if (registry.userExists( uuid.getAsString() )) {
                System.err.println ( "User already exists: " + data );
                sendResult( null, "\"uuid already exists\"" , false);
                return;
            }

            data.remove ( "type" );
            me = registry.addUser( data );

            sendResult( "\"result\":\"" + me.id + "\"", null , false);
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

            sendResult( "\"data\":" + (list == null ? "[]" : list), null , false);
            return;
        }

        // NEW

        if (cmd.getAsString().equals( "new" )) {
            JsonElement id = data.get( "id" );
            int user = id == null ? -1 : id.getAsInt();

            if (id == null || user <= 0) {
                System.err.print ( "No valid \"id\" field in \"new\" request: " + data );
                sendResult( null, "\"wrong request format\"" , false);
                return;
            }

            sendResult( "\"result\":" + registry.userNewMessages( user ), null , false);
            return;
        }

        // ALL

        if (cmd.getAsString().equals( "all" )) {
            JsonElement id = data.get( "id" );
            int user = id == null ? -1 : id.getAsInt();

            if (id == null || user <= 0) {
                System.err.print ( "No valid \"id\" field in \"new\" request: " + data );
                sendResult( null, "\"wrong request format\"" , false);
                return;
            }

            sendResult( "\"result\":[" + registry.userAllMessages( user ) + "," +
                        registry.userSentMessages( user ) + "]", null , false);
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
                sendResult( null, "\"wrong request format\"" , false);
                return;
            }

            int srcId = src.getAsInt();
            int dstId = dst.getAsInt();

            if (registry.userExists( srcId ) == false) {
                System.err.print ( "Unknown source id for \"send\" request: " + data );
                sendResult( null, "\"wrong parameters\"" , false);
                return;
            }

            if (registry.userExists( dstId ) == false) {
                System.err.print ( "Unknown destination id for \"send\" request: " + data );
                sendResult( null, "\"wrong parameters\"" , false);
                return;
            }

            // Save message and copy

            String response = registry.sendMessage( srcId, dstId,
                                                    msg.getAsString(),
                                                    copy.getAsString() );

            sendResult( "\"result\":" + response, null , false);
            return;
        }

        // RECV

        if (cmd.getAsString().equals( "recv" )) {
            JsonElement id = data.get( "id" );
            JsonElement msg = data.get( "msg" );
            String nonceToReceipt = cry.generateNonce();

            System.out.println("NONCE    ->  "+Base64.getDecoder().decode(nonceToReceipt));
            System.out.println("NONCESize    ->  "+Base64.getDecoder().decode(nonceToReceipt).length);
            //WRITE NONCE ON message



            if (id == null || msg == null) {
                System.err.print ( "Badly formated \"recv\" request: " + data );
                sendResult( null, "\"wrong request format\"" , false);
                return;
            }

            int fromId = id.getAsInt();

            if (registry.userExists( fromId ) == false) {
                System.err.print ( "Unknown source id for \"recv\" request: " + data );
                sendResult( null, "\"wrong parameters\"" , false);
                return;
            }

            if (registry.messageExists( fromId, msg.getAsString() ) == false &&
                registry.messageExists( fromId, "_" + msg.getAsString() ) == false) {
                System.err.println ( "Unknown message for \"recv\" request: " + data );
                sendResult( null, "\"wrong parameters\"" , false);
                return;
            }

            //write nonce in the file
            registry.registerNonce(fromId, msg.getAsString(),nonceToReceipt);


            // Read message

            JsonElement f = registry.getUser(fromId);
            if (!currUUID.equals(f.getAsJsonObject().get( "uuid" ).getAsString()))
                sendResult( null, "\"You cannot acess this messagebox\"" , false);

            String response = registry.recvMessage( fromId, msg.getAsString() );

            sendResult( "\"result\":" + response, null , false);
            return;
        }

        // RECEIPT

        if (cmd.getAsString().equals( "receipt" )) {
            JsonElement id = data.get( "id" );
            JsonElement msg = data.get( "msg" );
            JsonElement receipt = data.get( "receipt" );
            JsonElement nonce = data.get( "nonce" );
            if (id == null || msg == null || receipt == null) {
                System.err.print ( "Badly formated \"receipt\" request: " + data );
                //sendResult( null, "\"wrong request format\"" , false);
                return;
            }

            int fromId = id.getAsInt();

            if (registry.messageWasRed( fromId, msg.getAsString() ) == false) {
                System.err.print ( "Unknown, or not yet red, message for \"receipt\" request: " + data );
                //sendResult( null, "\"wrong parameters\"" , false);
                return;
            }

            if(registry.compareNonce(nonce.getAsString(),fromId,msg.getAsString())==false){
              System.err.print ("Nonce is not valid " );
              //sendResult( null, "\"wrong nonce\"" , false);
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
                sendResult( null, "\"wrong request format\"" , false);
                return;
            }

            int fromId = id.getAsInt();

            if (registry.copyExists( fromId, msg.getAsString() ) == false) {
                System.err.print ( "Unknown message for \"status\" request: " + data );
                sendResult( null, "\"wrong parameters\"" , false);
                return;
            }

            // Get receipts

            String response = registry.getReceipts( fromId, msg.getAsString() );

            sendResult( "\"result\":" + response, null , false);
            return;
        }

        sendResult( null, "\"Unknown request\"" , false);
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
