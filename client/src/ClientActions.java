import java.util.*;
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
    CryOperations cry;
    DHSession session = null;
    boolean santos = false;

    ClientActions ( Socket c ) {
        server = c;
        try {
            cc = new CCOperations();
            cry = new CryOperations();
            //cry.generateKey();

            in = new JsonReader(
                    new InputStreamReader ( c.getInputStream(), "UTF-8") );
            out = c.getOutputStream();

            br = new BufferedReader(new InputStreamReader(System.in));
        } catch (Exception e) {
            System.err.print( "Error initializing ClientActions: " + e );
            return;
        }
    }

    void
    readResponse () {
    System.out.println("Reading Response..");
        try {
            JsonElement data = new JsonParser().parse( in );
            if (session == null && data.isJsonObject()){
                System.out.println(data.getAsJsonObject());
            }
            if (data.isJsonObject()) {
                System.out.println(cry.processPayloadRecv(data.getAsJsonObject(),session.getSharedSecret()));
            }
            //System.err.print ( "Error while reading command from socket (not a JSON object), connection will be shutdown\n" );
        } catch (Exception e) {
            System.err.print ( "Error while reading JSON command from socket, connection will be shutdown\n" );
        }

   }


    void
    sendCommand ( String cmd, boolean sessionInit ) {
        String msg = "{";
        if (cmd != null) msg += cmd;
        msg += "}\n";

        //System.out.println(msg);
        try{
            if( !sessionInit ){
                String[] result = cry.processPayloadSend(msg, session.getSharedSecret());

                msg  = "{"+         "\"type\":\"payload\","+
                                    "\"payload\":\""+result[0]+"\","+
                                    "\"iv\":\""+result[1]+"\"," +
                                    "\"mac\":\""+result[2]+"\""+
                            "}";
            }

            //System.out.println( "Send cmd: " + msg );
            out.write ( msg.getBytes( StandardCharsets.UTF_8 ) );
            System.out.println("Sent!!!!");
        }catch (Exception e){
            System.err.print ( "Error while sending cmd to socket");
        }
    }

    boolean
    executeOpt( int opt ){

        if (opt < 1 || opt > 9) {
            System.err.println ( "Invalid command");
            return false;
        }

        if (opt == 9) {
            System.out.println("Connect to Server");
            String type = "session";
            String ln = System.getProperty("line.separator");
            String pubk;
            String cert = "O Santos não tem leitor de CC";
            String sign = "O Santos Não tem leitor de CC";
            try{
                session = new DHSession();
                pubk = session.getStringPubKey();
                if(!santos) cert = cc.getCertString();
                String toSign = pubk+ln+cert;
                if(!santos) sign = cc.sign(toSign);
            }catch(Exception e){
                System.err.print("Error Establishing Session " + e);
                return false;
            }

            sendCommand(    "\"type\":\""+type+"\","+
                            "\"pubk\":\""+pubk+"\","+
                            "\"cert\":\""+cert+"\","+
                            "\"signature\":\""+sign+"\"", true);

            try{
                JsonObject data = new JsonParser().parse( in ).getAsJsonObject();
                pubk = data.get( "pubk" ).getAsString();
                session.generateSecret(pubk);
            }catch(Exception e){
                System.err.print("Error Establishing Session 2" + e);
                return false;
            }
            return false;
        }

        // 1- Create new User
        if (opt == 1) {
            System.out.println("Create new user...");
            String ln = System.getProperty("line.separator");
            String type = "create";
            String uuid;
            String pubk;
            String cert = "O Santos não tem leitor de CC";
            String sign = "O Santos não tem leitor de CC";
            try{
                if (!santos) uuid = cc.getUUID();
                else uuid = Integer.toString(new Random().nextInt());
                pubk = cry.getKeyString(true);
                if (!santos) cert = cc.getCertString();
                String toSign = uuid+ln+pubk+ln+cert;
                if (!santos) sign = cc.sign(toSign);
            }catch(Exception e){
                System.err.print("Error Creating User");
                return false;
            }

            sendCommand(    "\"type\":\""+type+"\","+
                            "\"uuid\":\""+uuid+"\","+
                            "\"pubk\":\""+pubk+"\","+
                            "\"cert\":\""+cert+"\","+
                            "\"signature\":\""+sign+"\"", false);
            return true;
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
                return false;
            }
            System.err.print("xD: "+id);
            if (id.length()==0)
                sendCommand("\"type\":\""+type+"\"", false);
            else
                sendCommand("\"type\":\""+type+"\",\"id\":\""+id+"\"", false);
            return true;
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
                return false;
            }
            sendCommand("\"type\":\""+type+"\",\"id\":\""+id+"\"", false);
            return true;
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
                return false;
            }
            sendCommand("\"type\":\""+type+"\",\"id\":\""+id+"\"", false);
            return true;
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
                return false;
            }
            System.out.print("dst: ");
            try{
                dst = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return false;
            }

            System.out.print("msg: ");
            try{
                msg = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return false;
            }
            copy=msg;
            byte[] keyAES;
            byte[] msgEnc;
            byte[] aesKeyEnc;
            String subType= "list";
            String pubk;
            try{
                keyAES = cry.generateKeyAES();
                msgEnc = cry.encrAES(msg, keyAES);
                sendCommand("\"type\":\""+subType+"\",\"id\":\""+dst+"\"",false);
                JsonObject data = new JsonParser().parse( in ).getAsJsonObject();
                JsonObject  jobject = cry.processPayloadRecv(data.getAsJsonObject(),session.getSharedSecret());
                JsonArray jarray = jobject.getAsJsonArray("data");
                jobject = jarray.get(0).getAsJsonObject();
                pubk = jobject.get("pubk").getAsString();
                aesKeyEnc = cry.encrRSA(pubk,keyAES);
            }catch(Exception e){
                System.err.print("Error Sending Message" + e);
                return false;
            }

            sendCommand("\"type\":\""+type+"\",\"src\":\""+srcid+"\",\"dst\":\""+dst+"\",\"msg\":\""+aesKeyEnc +"\n"+msgEnc+"\n"+"\",\"copy\":\""+aesKeyEnc +"\n"+msgEnc+"\n"+"\"",false);
            return true;
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
                return false;
            }
            System.out.print("msg id: ");
            try{
                msg = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return false;
            }
            sendCommand("\"type\":\""+type+"\",\"id\":\""+id+"\",\"msg\":\""+msg+"\"", false);
            return true;
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
                return false;
            }
            System.out.print("msg id: ");
            try{
                msg = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return false;
            }
            System.out.print("calculating receipt... ");
            receipt="teste";
            sendCommand("\"type\":\""+type+"\",\"id\":\""+id+"\",\"msg\":\""+msg+"\",\"receipt\":\""+receipt+"\"", false);
            return false;
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
                return false;
            }
            System.out.print("msg id: ");
            try{
                msg = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return false;
            }

            sendCommand("\"type\":\""+type+"\",\"id\":\""+id+"\",\"msg\":\""+msg+"\"", false);
            return true;
        }

        sendCommand( "\"Unknown request\"", true);
        return false;
    }


    // Print Menu Options
    void
    printMenu(){
        System.out.printf(  "\nMenu Options:\n"+
                        "#==============================================#\n"+
                        "| 1- Create a user message box                 |\n"+
                        "| 2- List users’ messages boxes                |\n"+
                        "| 3- List new messages received by a user      |\n"+
                        "| 4- List all messages received by a user      |\n"+
                        "| 5- Send message to a user                    |\n"+
                        "| 6- Receive a message from a user message box |\n"+
                        "| 7- Send receipt for a message                |\n"+
                        "| 8- List messages sent and their receipts     |\n"+
                        "| 9- Connect to Server                         |\n"+
                        "| 0- Close Conection                           |\n"+
                        "#==============================================|\n"+
                        "opt -> "
                );
        return;
    }


    void
    printNoCCWarning(){
        System.out.printf(  "\nWarning!\n"+
                        "#==============================================#\n"+
                        "| No Portuguese Citizen Card Detected, this    |\n"+
                        "| program will not work without the citizen    |\n"+
                        "| card.                                        |\n"+
                        "| Please connect a CC and try again            |\n"+
                        "|  1 - Sou o Santos e não tenho leitor         |\n"+
                        "|  0 - Exit Program and try again              |\n"+
                        "#==============================================|\n"+
                        "opt -> "
                );
    }

    void
    printKeyWarnings(){
        System.out.printf(  "\nNote\n"+
                        "#==============================================#\n"+
                        "| Regarding other cryptographic actions, RSA   |\n"+
                        "| keys are needeed, would you like to create   |\n"+
                        "| new one ore load from existing files         |\n"+
                        "|  2 - Load keys                               |\n"+
                        "|  1 - Create New                              |\n"+
                        "|  0 - Exit Program                            |\n"+
                        "#==============================================|\n"+
                        "opt -> "
                );
    }

    String
    getFileName(){
        try{
            System.out.printf("File path\npath :");
            return br.readLine();
        }catch (Exception e){
            System.err.println("Error Reading path String");
            return null;
        }
    }

    int
    getOpt(int min, int max){
        int opt;
        try{
            opt = Integer.parseInt(br.readLine());
            while( opt < min || opt > max ){
                System.out.println("Invalid Option");
                System.out.print("opt -> ");
                opt = Integer.parseInt(br.readLine());
            }
            return opt;
        }catch(Exception e){
            return 0;
        }
    }

    void
    serverClose(){
        try {
            server.close();
        }catch (Exception e){
            System.err.println("Error Closing Connection to server");
        }
        System.exit(0);
    }

    // Main Client Loop
    public void
    run () {
        int opt;
        while ( !cc.provider ){
            printNoCCWarning();
            opt = getOpt(0, 1);
            if (opt == 0) serverClose();
            if (opt == 1) {
                santos = true;
                break;
            }
        }
        printKeyWarnings();
        opt = getOpt(0,3);
        if (opt == 0) serverClose();
        if (opt == 1){
            System.out.println("Keys path, e.g ./teste will generate ./teste.pub ans ./teste.key");
            String path = getFileName();
            cry.generateKey(path);
        }
        if (opt == 2){
            System.out.println("Public Key path(e.g ./teste.pub)");
            String path = getFileName();
            cry.readKey(true, path);
            System.out.println("Private Key path(e.g ./teste.key)");
            path = getFileName();
            cry.readKey(false, path);
        }
        if (opt == 3){
            cry.readKey(true, "./teste.pub");
            cry.readKey(false, "./teste.key");
        }
        while (true) {
            printMenu();
            opt = getOpt(0,9);
            if ( opt == 0) serverClose();
            if (executeOpt(opt)) readResponse();
        }
    }

}
