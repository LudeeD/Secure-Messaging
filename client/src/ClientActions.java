import java.util.*;
import java.lang.Thread;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.google.gson.*;
import com.google.gson.stream.*;
import java.security.*;

class ClientActions{

    Socket server;
    JsonReader in;
    OutputStream out;
    BufferedReader br;
    CCOperations cc;
    CryOperations cry;
    DHSession session = null;
    boolean santos = false;
    UserDescription currUser;
    String expectedNonce;

    ClientActions ( Socket c ) {
        server = c;
        try {
            cc  = new CCOperations();           // Try to initialize CC operation
            cry = new CryOperations();          // Initialize Cry Operations

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
        //System.out.println("Reading Response..");
        try {
            JsonElement data = new JsonParser().parse( in );
            if (session == null && data.isJsonObject()){
                System.out.println(data.getAsJsonObject());
            }
            if (data.isJsonObject()) {
                System.out.println(cry.processPayloadRecv(data.getAsJsonObject(),session.getSharedSecret(), expectedNonce));
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
                                    "\"mac\":\""+result[2]+"\","+
                                    "\"nonce\":\""+result[3]+"\""+
                            "}";

                expectedNonce = result[3];
            }

            //System.out.println( "Send cmd: " + msg );
            out.write ( msg.getBytes( StandardCharsets.UTF_8 ) );
            //System.out.println("Sent!!!!");
        }catch (Exception e){
            System.err.print ( "Error while sending cmd to socket "+e);
        }
    }

    boolean
    executeOpt( int opt ){

        if (opt < 1 || opt > 9) {
            System.err.println ( "Invalid command");
            return false;
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

            if (id.length()==0)
                sendCommand("\"type\":\""+type+"\"", false);
            else
                sendCommand("\"type\":\""+type+"\",\"id\":\""+id+"\"", false);

            JsonObject payload = new JsonParser().parse( in ).getAsJsonObject();
            JsonObject data = cry.processPayloadRecv(payload , session.getSharedSecret(), expectedNonce);
            JsonArray result = data.getAsJsonArray("data");

            UserDescription oneUser;
            String uuid, status;
            int idOneUser;
            System.out.println("=== Users ===\n id - uuid\n");
            for ( JsonElement user : result ){
                JsonObject u = user.getAsJsonObject();
                oneUser = new UserDescription(u,null);
                idOneUser = oneUser.getId();
                uuid = oneUser.getUUID();
                System.out.printf("%2d - %44s\n", idOneUser, uuid);
                status = String.valueOf(oneUser.isValid(cc));
                System.out.printf("Final Status: %s\n\n", status);
            }


            return false;
        }

        //  3- List new messages received by a user
        if (opt == 3) {
            String type = "new";
            String id = "" + currUser.getId();
            sendCommand("\"type\":\""+type+"\",\"id\":\""+id+"\"", false);
            return true;
        }

        // 4- List all messages received by a user
        if (opt == 4) {
            String type = "all";
            String id = ""+currUser.getId();
            sendCommand("\"type\":\""+type+"\",\"id\":\""+id+"\"", false);
            return true;
        }

        // 5 - Send message to a user
        if (opt == 5) {
            String type = "send";
            String srcid = ""+currUser.getId();
            String dst = "";
            String msg = "";
            String copy = "";
            try{
                System.out.print("dst: ");
                dst = br.readLine();
                System.out.print("msg: ");
                msg = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return false;
            }

            byte[] keyAES;
            String msgEnc;
            String aesKeyEnc;
            String msgFinal;
            String msgCopyFinal;
            String msgFinalSign;
            String msgCopyFinalSign;
            String aesKeyEncSrc;
            String subType= "list";
            String pubk;
            try{
                //for dst
                keyAES = cry.generateKeyAES();
                msgEnc = cry.encrAES(msg, keyAES);
                sendCommand("\"type\":\""+subType+"\",\"id\":\""+dst+"\"",false);
                JsonObject data = new JsonParser().parse( in ).getAsJsonObject();
                JsonObject  jobject = cry.processPayloadRecv(data.getAsJsonObject(),session.getSharedSecret(), expectedNonce);
                JsonArray jarray = jobject.getAsJsonArray("data");
                jobject = jarray.get(0).getAsJsonObject();
                pubk = jobject.get("pubk").getAsString();
                aesKeyEnc = cry.encrRSA(pubk,keyAES);
                //for source
                keyAES = cry.generateKeyAES();
                copy = cry.encrAES(msg, keyAES);
                aesKeyEncSrc = cry.encrRSA(currUser.getPublicKeyString(),keyAES);
                //sign message
                //for dst
                msgFinal = aesKeyEnc.concat("\n").concat(msgEnc);
                msgFinalSign = cc.sign(msgFinal);
                msgFinal = msgFinal.concat("\n").concat(msgFinalSign);
                //for source
                msgCopyFinal = aesKeyEncSrc.concat("\n").concat(copy);
                // #Todo Sign the encrypted Data or the Clear Text?
                msgCopyFinalSign = cc.sign(msgCopyFinal);
                msgCopyFinal = msgCopyFinal.concat("\n").concat(msgCopyFinalSign);

            }catch(Exception e){
                System.err.print("Error Sending Message" + e);
                return false;
            }

            sendCommand("\"type\":\""+type+"\",\"src\":\""+srcid+"\",\"dst\":\""+dst+"\",\"msg\":\""+msgFinal+"\",\"copy\":\""+msgCopyFinal+"\"",false);
            return true;
        }
        // 6- Receive a message from a user message box
        if (opt == 6) {
            String type = "recv";
            String id = ""+currUser.getId();
            String srcId = "";
            String msg = "";
            String subType= "list";
            String[] receivedResult;
            String pubk;
            try{
                System.out.print("msg id: ");
                msg = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return false;
            }
            sendCommand("\"type\":\""+type+"\",\"id\":\""+id+"\",\"msg\":\""+msg+"\"", false);
            try{
                JsonObject  data = new JsonParser().parse( in ).getAsJsonObject();
                JsonObject  jobject = cry.processPayloadRecv(data.getAsJsonObject(),session.getSharedSecret(), expectedNonce);
                JsonArray   jarray = jobject.getAsJsonArray("result");
                receivedResult = jarray.get(1).getAsString().split("\n");

                srcId=jarray.get(0).getAsString();
                sendCommand("\"type\":\""+subType+"\",\"id\":\""+srcId+"\"",false);
                data = new JsonParser().parse( in ).getAsJsonObject();
                jobject = cry.processPayloadRecv(data, session.getSharedSecret(), expectedNonce);
                JsonObject result = jobject.getAsJsonArray("data").get(0).getAsJsonObject();
                UserDescription senderUser = new UserDescription(result,null);

                if(!cc.verifySign(receivedResult[0].concat("\n").concat(receivedResult[1]), receivedResult[2],senderUser.getCertKey())){
                    System.err.println("Warning, Problems with the Signature");
                }

                cry.set_noncereceipt(receivedResult[3],msg);

                //System.out.println( "AES KEY (base 64) " + receivedResult[0]);
                byte[] aesKey = cry.decrRSA(receivedResult[0], currUser.getPrivateKey());
                //System.out.println( "MESSAGE (base 64) " + receivedResult[0]);
                String decrMsg = cry.decrAES(receivedResult[1], aesKey);
                System.out.println("Text of Message: "+decrMsg);
                //save signMessage
                //to use in receipts
                cry.set_readMessageSign(cc.sign(decrMsg),msg);
                //#######################//
            }catch(Exception e){
                System.err.print("Error receiving Message" + e);
                return false;
            }

            return false;
        }
        // 7-  Send receipt for a message
        if (opt == 7) {
            String type = "receipt";
            String id = ""+currUser.getId();
            String msg = "";
            String receipt = "";
            System.out.print("msg id: ");
            try{
                msg = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return false;
            }
            //System.out.print("calculating receipt... ");
            //SIGNATURE+MESSSAGE
            receipt=cry.get_readMessageSign(msg);
            //System.out.println("Receipt: "+receipt);
            sendCommand("\"type\":\""+type+"\",\"id\":\""+id+"\",\"msg\":\""+msg+"\",\"receipt\":\""+receipt+"\",\"nonce\":\""+cry.get_noncereceipt(msg)+"\"", false);
            return false;
        }

        // 8-  List messages sent and their receipts
        if (opt == 8) {
            String type = "status";
            String id = ""+currUser.getId();
            String msg = "";
            System.out.print("msg id: ");
            try{
                msg = br.readLine();
            }catch(Exception e){
                System.err.print("Error reading Line");
                return false;
            }

            sendCommand("\"type\":\""+type+"\",\"id\":\""+id+"\",\"msg\":\""+msg+"\"", false);

            verifyReceipts();

            return false;
        }

        sendCommand( "\"Unknown request\"", true);
        return false;
    }

    void
    verifyReceipts(){
        JsonObject data = new JsonParser().parse( in ).getAsJsonObject();
        data = cry.processPayloadRecv(data , session.getSharedSecret(), expectedNonce);
        JsonObject result = data.getAsJsonObject("result");

        String message = result.get("msg").getAsString();
        String[] receivedResult = message.split("\n");

        byte[] aesKey = cry.decrRSA(receivedResult[0], currUser.getPrivateKey());
        String decrMsg = cry.decrAES(receivedResult[1], aesKey);
        System.out.println("=== Status of Message ===");
        System.out.println("Text of Message: " + decrMsg);

        JsonArray receipts = result.get("receipts").getAsJsonArray();
        System.out.printf("\n");
        for ( JsonElement rec : receipts ){
            JsonObject r = rec.getAsJsonObject();
            System.out.println(new Date(r.get("date").getAsLong()));

            sendCommand("\"type\":\"list\",\"id\":\""+r.get("id").getAsString()+"\"",false);
            JsonObject datia = new JsonParser().parse( in ).getAsJsonObject();
            datia = cry.processPayloadRecv(datia, session.getSharedSecret(), expectedNonce);
            JsonObject resulta = datia.getAsJsonArray("data").get(0).getAsJsonObject();
            UserDescription senderUser = new UserDescription(resulta,null);

            System.out.println("Veryifying Sender of Receipt");
            if( !senderUser.isValid( cc ) ){
                System.out.println("Something went wrong validating the receipt sender");
                return;
            }

            System.out.print("Valid Receipt: ");
            String recas = r.get("receipt").getAsString();
            System.out.println(cc.verifySign(decrMsg, recas, senderUser.getCertKey()));
            System.out.printf("\n");
        }
    }

    boolean
    kindOfLogin(String uuid, boolean newUser, PublicKey pubK, PrivateKey privK ){

        String id = "";
        String type;
        JsonObject payload;
        JsonObject data;

        if( newUser ){
            // Issue A CREATE and build UserDescription currUser
            String ln = System.getProperty("line.separator");
            type = "create";
            String pubk = "Something went wrong";
            String cert = "Something went wrong";
            String sign = "Something went wrong";
            try{
                uuid = uuid;
                pubk = Base64.getEncoder().encodeToString(pubK.getEncoded());
                cert = cc.getCertString();
                String toSign = uuid+ln+pubk+ln+cert;
                sign = cc.sign(toSign);
            }catch(Exception e){
                System.err.print("Error Creating User" + e);
                System.exit(1);
            }

            sendCommand(    "\"type\":\""+type+"\","+
                            "\"uuid\":\""+uuid+"\","+
                            "\"pubk\":\""+pubk+"\","+
                            "\"cert\":\""+cert+"\","+
                            "\"signature\":\""+sign+"\"", false);

            //System.out.println("Sent new User Creatin");
            payload = new JsonParser().parse( in ).getAsJsonObject();
            data = cry.processPayloadRecv(payload , session.getSharedSecret(), expectedNonce);
            //System.out.println(data);
            id = data.get( "result" ).getAsString();
        }

        //System.out.println("Going to list");
        // Issue a LIST and build UserDescription currUser
        type = "list";

        if (id.length()==0)
            sendCommand("\"type\":\""+type+"\"", false);
        else
            sendCommand("\"type\":\""+type+"\",\"id\":\""+id+"\"", false);

        payload = new JsonParser().parse( in ).getAsJsonObject();
        data = cry.processPayloadRecv(payload , session.getSharedSecret(), expectedNonce);
        JsonArray result = data.getAsJsonArray("data");

        for ( JsonElement user : result ){
            JsonObject u = user.getAsJsonObject();
            if( u.get("uuid").getAsString().equals(uuid)){
                currUser = new UserDescription(u, privK);
                return true;
            }
        }

        System.err.println("No User Exists");
        return false;
    }

    void
    establishSession(){
        //System.out.print("Establishing Session...");
        String type = "session";
        String ln = System.getProperty("line.separator");
        String pubk = "Something is not right";
        String cert = "Something is not right";
        String sign = "Something is not right";
        try{
            session = new DHSession();
            pubk = session.getStringPubKey();
            cert = cc.getCertString();
            String toSign = pubk+ln+cert;
            sign = cc.sign(toSign);
        }catch(Exception e){
            System.err.print("Error Establishing Session " + e);
            System.exit(1);
        }

        sendCommand(    "\"type\":\""+type+"\","+
                        "\"pubk\":\""+pubk+"\","+
                        "\"cert\":\""+cert+"\","+
                        "\"signature\":\""+sign+"\"", true);

        try{
            // TODO verify the response of server for success
            JsonObject data = new JsonParser().parse( in ).getAsJsonObject();
            pubk = data.get( "pubk" ).getAsString();
            session.generateSecret(pubk);
        }catch(Exception e){
            System.err.print("Error Establishing Session 2" + e);
            System.exit(1);
        }
        //System.out.print("OK\n");
    }

    // Print Menu Options
    void
    printMenu(){
        System.out.printf(  "\nMenu Options:\n"+
                        "#==============================================#\n"+
                        "| 2- List Existing Users                       |\n"+
                        "| 3- List New messages                         |\n"+
                        "| 4- List All messages                         |\n"+
                        "| 5- Send message                              |\n"+
                        "| 6- Receive a message                         |\n"+
                        "| 7- Send receipt for a message                |\n"+
                        "| 8- List messages sent and their receipts     |\n"+
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
                        "| new one or  load from existing files         |\n"+
                        "|  2 - Load keys                               |\n"+
                        "|  1 - Create New                              |\n"+
                        "|  0 - Exit Program                            |\n"+
                        "#==============================================|\n"+
                        "opt -> "
                );
    }

    void
    printKindOfLogin( String uuid ){
        System.out.printf(  "\nKind of Login\n"+
                        "#==============================================#\n"+
                        "| Welcome,                                     |\n"+
                        "| %44s |\n"+
                        "|                                              |\n"+
                        "|  2 - New User                                |\n"+
                        "|  1 - Existing User                           |\n"+
                        "|  0 - Exit Program                            |\n"+
                        "#==============================================|\n"+
                        "opt -> ", uuid
                );
    }

    void
    printEstablishSession(){
        System.out.printf(  "\nEstablish Session\n"+
                        "#==============================================#\n"+
                        "| A encrypted session is going to be made      |\n"+
                        "| between this client and the Server           |\n"+
                        "|                                              |\n"+
                        "|  1 - Continue                                |\n"+
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

        if ( !cc.provider ){
            printNoCCWarning();
            opt = getOpt(0,1);
            serverClose();
        }

        // Establish Session
        printEstablishSession();
        opt = getOpt(0,1);
        if( opt == 0 ) serverClose();
        establishSession();

        // Kind Of Login
        boolean sucess_login = false;
        while(!sucess_login){
            String uuid = cc.getUUID();
            printKindOfLogin(uuid);
            int opt_login = getOpt(0,2);
            if( opt_login == 0 ) serverClose();

            printKeyWarnings();
            opt = getOpt(0,4);
            String path;
            PublicKey pubK = null;
            PrivateKey privK = null;
            switch (opt){
                case 0: serverClose();
                        break;
                case 1: System.out.println("Keys path (e.g ./teste");
                        path = getFileName();
                        KeyPair kp = cry.generateKey(path);
                        pubK = kp.getPublic();
                        privK = kp.getPrivate();
                        break;
                case 2: System.out.println("Public Key path(e.g ./teste.pub)");
                        path = getFileName();
                        pubK = (PublicKey)cry.readKey(true, path);
                        System.out.println("Private Key path(e.g ./teste.key)");
                        path = getFileName();
                        privK = (PrivateKey)cry.readKey(false, path);
                        break;
                case 3: pubK  = (PublicKey)cry.readKey(true, "./user1.pub");
                        privK = (PrivateKey)cry.readKey(false, "./user1.key");
                        break;
                case 4: pubK  = (PublicKey)cry.readKey(true, "./user2.pub");
                        privK = (PrivateKey)cry.readKey(false, "./user2.key");
                        break;
            }

            if (opt_login == 2) sucess_login=kindOfLogin(uuid, true, pubK, privK);
            else sucess_login=kindOfLogin(uuid, false, pubK, privK);
        }
        while (true) {
            printMenu();
            opt = getOpt(0,9);
            if ( opt == 0) serverClose();
            if (executeOpt(opt)) readResponse();
        }
    }

}
