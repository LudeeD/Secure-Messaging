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
        if (cmd != null) {
            msg += cmd;
        }

        msg += "}\n";

        try{
            if( !sessionInit ){
                String[] result = cry.processPayloadSend(msg, session.getSharedSecret());

                msg  = "{"+         "\"type\":\"payload\","+
                                    "\"payload\":\""+result[0]+"\","+
                                    "\"iv\":\""+result[1]+"\"," +
                                    "\"mac\":\""+result[2]+"\""+
                            "}";
            }

            System.out.println( "Send cmd: " + msg );
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
            String cert = "";
            String sign = "";
            try{
                session = new DHSession();
                pubk = session.getStringPubKey();
                //cert = cc.getCertString();
                //String toSign = pubk+ln+cert;
                //sign = cc.sign(toSign);
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
            String cert;
            String sign = "";
            try{
                uuid = cc.getUUID();
                pubk = cry.getKeyString(true, "./luis.pub");
                cert = cc.getCertString();
                String toSign = uuid+ln+pubk+ln+cert;
                //sign = cc.sign(toSign);
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
                  System.out.print(msgEnc);
                  sendCommand("\"type\":\""+subType+"\",\"id\":\""+dst+"\"",false);
                  try{
                    JsonObject data = new JsonParser().parse( in ).getAsJsonObject();
                    //  pubk = data.get( "pubk" ).getAsString();
                    JsonObject  jobject = data.getAsJsonObject();
                    JsonArray jarray = jobject.getAsJsonArray("data");
                    jobject = jarray.get(0).getAsJsonObject();
                    pubk = jobject.get("pubk").getAsString();
                    aesKeyEnc = cry.encrRSA(pubk,keyAES);
                    System.out.print(aesKeyEnc);
                  }catch(Exception e){
                      System.err.print("Error Establishing Session" + e);
                      return false;
                  }

            }catch(Throwable e){
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
                        "9- Connect to Server\n"+
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
            if (executeOpt(opt)) readResponse();
        }

    }

}
