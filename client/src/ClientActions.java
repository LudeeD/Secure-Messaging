import java.lang.Thread;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.google.gson.*;
import com.google.gson.stream.*;

class ClientActions{

    Socket server;
    JsonReader in;
    OutputStream out;
    BufferedReader br;
    CCOperations cc;

    ClientActions ( Socket c ) {
        server = c;
        try {
            in = new JsonReader(
                    new InputStreamReader ( c.getInputStream(), "UTF-8") );
            out = c.getOutputStream();
            br = new BufferedReader(new InputStreamReader(System.in));
            CCOperations cc = new CCOperations();
            cc.printAlias();
        } catch (Exception e) {
            System.err.print( "Cannot use socket: " + e );
            return;
        }
    }

    void
    readResponse () {
        try {
            JsonElement data = new JsonParser().parse( in );
            if (data.isJsonObject()) {
                System.out.print(data.toString());
                return;
            }
            System.err.print ( "Error while reading response from socket (not a JSON object), connection will be shutdown\n" );
            return;
        } catch (Exception e) {
            System.err.print ( "Error while reading JSON response from socket, connection will be shutdown\n"+e);
            return;
        }
    }

    void
    sendCommand ( String cmd ) {
        String msg = "{";

        // Usefull result

        if (cmd != null) {
            msg += cmd;
        }

        msg += "}\n";

        try {
            System.out.print( "Send cmd: " + msg );
            out.write ( msg.getBytes( StandardCharsets.UTF_8 ) );
        } catch (Exception e ) {
            System.err.print ( "Error while sending cmd to socket");
        }
    }

    void
    executeOpt( int opt ){

        if (opt < 1 || opt > 8) {
            System.err.println ( "Invalid command");
            return;
        }

        // 1- Create new User
        if (opt == 1) {
            String type = "create";
            String uuid = "";
            System.out.print("uuid: ");
            try{
                uuid = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return;
            }
            sendCommand("\"type\":\""+type+"\",\"uuid\":\""+uuid+"\"");
            return;
        }

        // 2- List users’ messages boxes
        if (opt == 2) {
            String type = "list";
            String id = "";
            System.out.print("id: ");
            try{
                id = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return;
            }
            System.err.print("xD: "+id);
            if (id.length()==0)
                sendCommand("\"type\":\""+type+"\"");
            else
                sendCommand("\"type\":\""+type+"\",\"id\":\""+id+"\"");
            return;
        }

        //  3- List new messages received by a user
        if (opt == 3) {
            String type = "new";
            String id = "";
            System.out.print("id: ");
            try{
                id = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return;
            }
            sendCommand("\"type\":\""+type+"\",\"id\":\""+id+"\"");
            return;
        }
        // 4- List all messages received by a user
        if (opt == 4) {
            String type = "all";
            String id = "";
            System.out.print("id: ");
            try{
                id = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return;
            }
            sendCommand("\"type\":\""+type+"\",\"id\":\""+id+"\"");
            return;
        }
        // 5 - Send message to a user
        if (opt == 5) {
            String type = "send";
            String srcid = "";
            String dst = "";
            String msg = "";
            String copy = "";
            System.out.print("srcid: ");
            try{
                srcid = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return;
            }
            System.out.print("dst: ");
            try{
                dst = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return;
            }

            System.out.print("msg: ");
            try{
                msg = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return;
            }
            copy=msg;
            sendCommand("\"type\":\""+type+"\",\"src\":\""+srcid+"\",\"dst\":\""+dst+"\",\"msg\":\""+msg+"\",\"copy\":\""+copy+"\"");
            return;
        }
        // 6- Receive a message from a user message box
        if (opt == 6) {
            String type = "recv";
            String id = "";
            String msg = "";
            System.out.print("id: ");
            try{
                id = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return;
            }
            System.out.print("msg id: ");
            try{
                msg = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return;
            }
            sendCommand("\"type\":\""+type+"\",\"id\":\""+id+"\",\"msg\":\""+msg+"\"");
            return;
        }
        // 7-  Send receipt for a message
        if (opt == 7) {
            String type = "receipt";
            String id = "";
            String msg = "";
            String receipt = "";
            System.out.print("id: ");
            try{
                id = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return;
            }
            System.out.print("msg id: ");
            try{
                msg = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return;
            }
            System.out.print("calculating receipt... ");
            receipt="teste";
            sendCommand("\"type\":\""+type+"\",\"id\":\""+id+"\",\"msg\":\""+msg+"\",\"receipt\":\""+receipt+"\"");
            return;
        }
        // 8-  List messages sent and their receipts
        if (opt == 8) {
            String type = "status";
            String id = "";
            String msg = "";
            System.out.print("id: ");
            try{
                id = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return;
            }
            System.out.print("msg id: ");
            try{
                msg = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return;
            }

            sendCommand("\"type\":\""+type+"\",\"id\":\""+id+"\",\"msg\":\""+msg+"\"");
            return;
        }

        sendCommand( "\"Unknown request\"" );
        return;
    }


    // Print Menu Options
    void
    printMenu(){
        System.out.printf(  "\nOptions:\n"+
                        "==============================================\n"+
                        "1- Create a user message box\n"+
                        "2- List users’ messages boxes\n"+
                        "3- List new messages received by a user\n"+
                        "4- List all messages received by a user\n"+
                        "5- Send message to a user\n"+
                        "6- Receive a message from a user message box\n"+
                        "7- Send receipt for a message\n"+
                        "8- List messages sent and their receipts\n"+
                        "0- Close Conection\n"+
                        "opt -> "
                );
        return;
    }


    // Get Client Option
    int
    getOpt(){
        try{
            printMenu();
            return Integer.parseInt(br.readLine());
        }catch(Exception e){
        }
        return 0;
    }

    // Main Client Loop
    public void
    run () {
        while (true) {
            int opt = getOpt();
            if ( opt == 0){
                try {
                    server.close();
                }catch (Exception e){}
                return;
            }
            executeOpt(opt);
            if (opt != 7) readResponse();
        }

    }

}
