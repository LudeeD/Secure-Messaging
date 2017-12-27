import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Date;
import com.google.gson.*;
import java.io.RandomAccessFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;

class ServerControl {
    ConcurrentSkipListSet<UserDescription> users = null;
    final String mboxesPath = "mboxes";
    final String receiptsPath = "receipts";
    final String descFilename = "description";
    File mboxesDir = null;
    File receiptsDir = null;

    ServerControl () {
        users = new ConcurrentSkipListSet<UserDescription>();

        // Create mboxes directory, if not found

        mboxesDir = new File( mboxesPath );

        if (mboxesDir.exists() == false) {
            try {
                mboxesDir.mkdir();
            } catch (Exception e) {
                System.err.println( "Cannot create directory " + mboxesPath + ": " + e );
                System.exit ( 1 );
            }
        }

        // Create receipts directory, if not found

        receiptsDir = new File( receiptsPath );

        if (receiptsDir.exists() == false) {
            try {
                receiptsDir.mkdir();
            } catch (Exception e) {
                System.err.println( "Cannot create directory " + receiptsPath + ": " + e );
                System.exit ( 1 );
            }
        }

        // Load data for each and every user

        for (File file: mboxesDir.listFiles()) {
            if (file.isDirectory()) { // Users have a directory of their own
                int id;
                JsonElement description = null;

                try {
                    id = Integer.parseUnsignedInt( file.getName() );
                } catch (Exception e ) {
                    continue; // Not a user directory
                }

                // Read JSON description from file

                String path = mboxesPath + "/" + file.getName() + "/" + descFilename;

                try {
                    description = new JsonParser().parse( readFromFile( path ) );
                } catch (Exception e) {
                    System.err.println( "Cannot load user description from " + path + ": " + e );
                    System.exit ( 1 );
                }

                // Add user to the internal structure

                users.add( new UserDescription ( id, description ) );
            }
        }
    }

    private void
    saveOnFile ( String path, String data ) throws Exception {
        FileWriter f = new FileWriter( path );
        f.write( data );
        f.flush();
        f.close();
    }
    //CREATED BY US
    private void
    addToFile(String path, String text) throws Exception {
      FileWriter f = new FileWriter( path, true ); //add to file
      f.write( text );
      f.flush();
      f.close();
    }

    private void
    changeNonce(String path, String text) throws Exception {
      RandomAccessFile file = new RandomAccessFile(path, "rw");
      long length = file.length() - 1;
      byte b;
      do {
          length -= 1;
          file.seek(length);
          b = file.readByte();
      } while(b != 10);
      file.setLength(length+1);
      file.close();

      FileWriter file1 = new FileWriter( path, true ); //add to file
      file1.write( text );
      file1.flush();
      file1.close();
    }

    private String
    readFromFile ( String path ) throws Exception {
        FileInputStream f = new FileInputStream( path );
        byte [] buffer = new byte[f.available()];
        f.read( buffer );
        f.close();

        return new String( buffer, StandardCharsets.UTF_8 );
    }
    //CREATED BY US
    private String
    readNonce(String path) throws Exception {

      FileInputStream in = new FileInputStream(path);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));

      String strLine = null, tmp;

      while ((tmp = br.readLine()) != null)
      {
        strLine = tmp;
      }

      String nonce = strLine;

      in.close();
      return nonce;
    }
    //CREATED BY US
    void
    registerNonce(int id, String msg, String nonce) {
      String path = userMessageBox( id ) + "/";
      if (msg.charAt( 0 ) == '_') { // Already red
          path += msg;
          try{
            changeNonce(path, nonce);
          }catch(Exception e){
            System.err.print("Error adding nonce to file" + e);
          }
      } else {
          File f = new File( path + "_" + msg );
          if (f.exists()) {         // Already red
              path += "_" + msg;
              try{
                changeNonce(path, nonce);
              }catch(Exception e){
                System.err.print("Error adding nonce to file" + e);
              }
          }
          else { //no red
            path += msg;
            try{
              addToFile(path, "\n".concat(nonce));
            }catch(Exception e){
              System.err.print("Error adding nonce to file" + e);
            }

          }
      }


    }
    //CREATED BY US
    boolean compareNonce(String nonce,int id, String msg){
      String path = userMessageBox( id ) + "/";
      String nonceFromFile="";
      if (msg.charAt( 0 ) == '_') { // Already red
          path += msg;
          try{
            nonceFromFile = readNonce(path);
          }catch(Exception e){
            System.err.print("Error reading nonce from file" + e);
          }
      } else {
          File f = new File( path + "_" + msg );
          if (f.exists()) {         // Already red
              path += "_" + msg;
              try{
                nonceFromFile = readNonce(path);
              }catch(Exception e){
                System.err.print("Error reading nonce from file" + e);
              }
          }
          else { //no red
            path += msg;
            try{
              nonceFromFile = readNonce(path);
            }catch(Exception e){
              System.err.print("Error reading nonce from file" + e);
            }
          }
          System.out.println(path);
      }

      if(nonceFromFile.equals(nonce)){
        return true;
      }
      return false;
    }

    boolean
    messageWasRed ( int id, String message ) {
        if (message.charAt( 0 ) == '_') {
            return (new File( userMessageBox( id ) + "/" + message )).exists();
        } else {
            return (new File( userMessageBox( id ) + "/_" + message )).exists();
        }
    }

    boolean
    messageExists ( int id, String message ) {
        return (new File( userMessageBox( id ) + "/" + message )).exists();
    }

    boolean
    copyExists ( int id, String message ) {
        return (new File( userReceiptBox( id ) + "/" + message )).exists();
    }

    synchronized boolean
    userExists ( int id ) {
        return users.contains( new UserDescription( id ) );
    }

    synchronized boolean
    userExists ( String uuid ) {
        for (UserDescription u: users) {
            if (u.uuid.equals( uuid )) {
                return true;
            }
        }

        return false;
    }

    synchronized JsonElement
    getUser ( int id ) {
        for (UserDescription u: users) {
            if (u.id == id ) {
                return u.description;
            }
        }

        return null;
    }

    synchronized UserDescription
    addUser ( JsonElement description ) {
        int id;
        String path = null;

        // Find a free user id

        for (id = 1; userExists( id ); id++) {}

        System.out.println ( "Add user \"" + id + "\": " + description );

        // Add it to the users' internal list

        UserDescription user = new UserDescription( id, description );
        users.add( user );

        // Create message box, recepit box and save description

        try {
            (new File( userMessageBox( id ) )).mkdir();
            (new File( userReceiptBox( id ) )).mkdir();
        } catch (Exception e) {
            System.err.println( "Cannot create directory " + path + ": " + e );
            System.exit ( 1 );
        }

        try {
            path = mboxesPath + "/" + Integer.toString( id ) + "/" + descFilename;
            saveOnFile( path, description.toString() );
        } catch (Exception e ) {
            System.err.println( "Cannot create description file " + path + ": " + e );
            System.exit ( 1 );
        }

        return user;
    }

    synchronized String
    listUsers ( int id ) {
        if (id == 0) {
            System.out.println( "Looking for all connected users" );
        } else {
            System.out.println( "Looking for \"" + id + "\"" );
        }

        if (id != 0) {
            JsonElement user = getUser( id );
            if (user != null) {
                return "[" + user + "]";
            }
            return null;
        } else {
            String list = null;
            for (UserDescription u: users) {
                if (list == null) {
                    list = "[" + u.description;
                } else {
                    list += "," + u.description;
                }
            }

            if (list == null) {
                list = "[]";
            } else {
                list += "]";
            }
            return list;
        }
    }

    String
    userAllMessages ( int id ) {
        return "[" + userMessages( userMessageBox( id ), "_?+[0-9]+_[0-9]+" ) + "]";
    }

    String
    userNewMessages ( int id ) {
        return "[" + userMessages( userMessageBox( id ), "[0-9]+_[0-9]+" ) + "]";
    }

    String
    userSentMessages ( int id ) {
        return "[" + userMessages( userReceiptBox( id ), "[0-9]+_[0-9]+" ) + "]";
    }

    private String
    userMessages ( String path, String pattern ) {
        File mbox = new File( path );
        Pattern msgPattern = Pattern.compile( pattern );
        String result = "";

        System.out.println( "Look for files at " + path + " with pattern " + pattern );

        try {
            for (File file: mbox.listFiles()) {
                System.out.println( "\tFound file " + file.getName() );
                Matcher m = msgPattern.matcher( file.getName() );
                if (m.matches()) {
                    if (result.length() > 0) {
                        result += ',';
                    }
                    result += "\"" + file.getName() + "\"";
                }
            }
        } catch (Exception e) {
            System.err.println( "Error while listing messages in directory " + mbox.getName() + ": " + e );
        }

        return result;
    }

    private int
    newFile ( String path, String basename ) {
        for (int i = 1;; i++) {
            File file1 = new File( path + basename + i );
            File file2 = new File( path + "_" + basename + i );
            if (file1.exists() == false && file2.exists() == false) {
                return i;
            }
        }
    }

    String
    sendMessage ( int src, int dst, String msg, String receipt ) {
        int nr = 0;
        String result;
        String path = null;

        try {
            path = userMessageBox( dst ) + "/";
            nr = newFile( path, src + "_" );
            saveOnFile ( path + src + "_" + nr, msg );

            result = "[\"" + src + "_" + nr + "\"";

            path = userReceiptBox( src ) + "/" + dst + "_";
            saveOnFile ( path + nr, receipt );
        } catch (Exception e) {
            System.err.println( "Cannot create message or copy file " + path + nr + ": " + e );
            return "[\"\",\"\"]";
        }

        return result + ",\"" + dst + "_" + nr + "\"]";
    }

    String readMsgFile ( int id, String msg ) throws Exception {
        String path = userMessageBox( id ) + "/";

        if (msg.charAt( 0 ) == '_') { // Already red
            path += msg;
        } else {
            File f = new File( path + "_" + msg );
            if (f.exists()) {         // Already red
                path += "_" + msg;
            }
            else { // Rename before reading
                try {
                    f = new File( path + msg );
                    path += "_" + msg;
                    f.renameTo ( new File ( path ) );
                } catch (Exception e) {
                    System.err.println( "Cannot rename message file to " + path + ": " + e );
                    path += msg; // Fall back to the non-renamed file
                }
            }
        }

        return readFromFile ( path );
    }

    String
    recvMessage ( int id, String msg ) {
        String result = "[";

        // Extract message sender id

        Pattern p = Pattern.compile( "_?+([0-9]+)_[0-9]+" );
        Matcher m = p.matcher( msg );

        if (m.matches() == false) {
            System.err.println( "Internal error, wrong message file name (" + msg + ") format!" );
            System.exit ( 2 );
        }

        result += m.group( 1 ) + ",";

        // Read message

        try {
            result += "\"" + readMsgFile( id, msg ) + "\"";
        } catch(Exception e) {
            System.err.println( "Cannot read message " + msg + " from user " + id + ": " + e );
            result += "\"\"";
        }

        return result + "]";
    }

    String
    userMessageBox ( int id ) {
        return mboxesPath + "/" + Integer.toString( id );
    }

    String
    userReceiptBox ( int id ) {
        return receiptsPath + "/" + Integer.toString( id );
    }

    void
    storeReceipt ( int id, String msg, String receipt ) {
        Pattern p = Pattern.compile( "_?+([0-9]+)_([0-9])" );
        Matcher m = p.matcher( msg );

        if (m.matches() == false) {
            System.err.println( "Internal error, wrong message file name (" + msg + ") format!" );
            System.exit ( 2 );
        }

        String path = userReceiptBox( Integer.parseInt( m.group( 1 ) ) ) + "/_" + id + "_" + m.group( 2 ) + "_" + System.currentTimeMillis();

        try {
            saveOnFile ( path, receipt );
        } catch (Exception e) {
            System.err.println( "Cannot create receipt file " + path + ": " + e );
        }

    }

    String
    getReceipts ( int id, String msg ) {
        Pattern p = Pattern.compile( "_(([0-9])+_[0-9])_([0-9]+)" );
        File dir = new File( userReceiptBox( id ) );
        String result;
        String receipt;
        String copy;
        int receipts = 0;

        try {
            copy = readFromFile( userReceiptBox( id ) + "/" + msg );
        } catch(Exception e) {
            System.err.println( "Cannot read a copy file: " + e );
            copy = "";
        }

        result = "{\"msg\":\"" + copy + "\",\"receipts\":[";

        for (File f: dir.listFiles()) {
            Matcher m = p.matcher ( f.getName() );
            if (m.matches() && m.group( 1 ).equals( msg )) {
                if (receipts != 0) {
                    result += ",";
                }

                try {
                    receipt = readFromFile( userReceiptBox( id ) + "/" + f.getName() );
                } catch(Exception e) {
                    System.err.println( "Cannot read a receipt file: " + e );
                    receipt = "";
                }

                result += "{\"date\":" + m.group( 3 ) + ",\"id\":" + m.group( 2 ) + ",";
                result += "\"receipt\":\"" + receipt  + "\"}";
                receipts++;
            }
        }

        return result + "]}";
    }

}
