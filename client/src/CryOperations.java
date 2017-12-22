import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.*;
import java.nio.file.*;
import com.google.gson.*;
import java.io.*;


class CryOperations{

    CryOperations () {
        System.out.println("Cryptography Operations!");
    }

    void
    generateKey() throws Exception{
        System.out.print("Generating RSA Key Pair...");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();

        PublicKey pk = keyPair.getPublic();
        PrivateKey pr = keyPair.getPrivate();

        writeKey(pk, "./luis.pub");
        writeKey(pr, "./luis.key");

        System.out.print("OK\n");
        //PublicKey pk1 = (PublicKey) readKey(true, "./luis.pub");
        //PrivateKey pr1 = (PrivateKey)readKey(false, "./luis.key");

        //System.out.println(pk.equals(pk1));
        //System.out.println(pr.equals(pr1));
    }

    void
    writeKey(Key k, String path) throws Exception{
        Files.write(Paths.get(path), k.getEncoded());
    }

    Key
    readKey(boolean pub, String path) throws Exception{
        System.out.println("Reading key from file...");

        Path p = Paths.get(path);
        byte[] bytes = Files.readAllBytes(p);

        KeyFactory kf = KeyFactory.getInstance("RSA");

        if (pub){
            X509EncodedKeySpec ks = new X509EncodedKeySpec(bytes);
            return kf.generatePublic(ks);
        }else{
            PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(bytes);
            return kf.generatePrivate(ks);
        }
    }

    String
    getKeyString(boolean pub, String path) throws Exception{
        Key k = readKey(pub, path);
        return Base64.getEncoder().encodeToString(k.getEncoded());
    }

    String[]
    processPayloadSend(String payload, byte[] sessionKey) throws Exception{
        System.out.println("Process Payload To Send");
        // TODO 256 bit its too long for java, additional files are required
        byte[] keyForMac = Arrays.copyOfRange(sessionKey, 0, sessionKey.length/2);
        byte[] keyForEnc = Arrays.copyOfRange(sessionKey, (sessionKey.length/4) * 3, sessionKey.length);

        System.out.print("Encrypting Payload...");
        byte[] encpayload = null;
        byte[] iv = null;
        try{
            SecretKeySpec sks = new SecretKeySpec(keyForEnc, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, sks);
            iv = cipher.getIV();
            encpayload = cipher.doFinal(payload.getBytes());
        }catch(Exception e){
            System.out.println("Error Encrypting Payload\n"+e);
            return null;
        }
        System.out.print("OK\n");
        System.out.print("Generate Mac...");
        Mac sha256HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec keyMAC = new SecretKeySpec(keyForMac, "HmacSHA256");
        sha256HMAC.init(keyMAC);
        byte[] mac = sha256HMAC.doFinal(encpayload);
        System.out.print("OK\n");

        String[] result = new String[3];
        result[0] = Base64.getEncoder().encodeToString(encpayload);
        result[1] = Base64.getEncoder().encodeToString(iv);
        result[2] = Base64.getEncoder().encodeToString(mac);
        return result;
    }


    JsonObject
    processPayloadRecv( JsonObject payload,  byte[] sessionKey ) throws Exception{
        System.out.println("Process Payload Received");

        JsonElement elem = payload.get("type");

        if(elem.getAsString().equals("session")){
            System.out.print("Session Establishment Process");
            return payload;
        }


        elem = payload.get("payload");
        byte[] keyForMac = Arrays.copyOfRange(sessionKey, 0, sessionKey.length/2);
        Mac sha256HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec keyMAC = new SecretKeySpec(keyForMac, "HmacSHA256");
        System.out.println("Mac Key:"+Base64.getEncoder().encodeToString(keyMAC.getEncoded()));
        sha256HMAC.init(keyMAC);
        byte[] mac = sha256HMAC.doFinal(Base64.getDecoder().decode(elem.getAsString()));

        //System.out.println("Calculated " +Base64.getEncoder().encodeToString(mac) );
        //System.out.println("Received " + payload.get("mac").getAsString());

        elem = payload.get("mac");
        if (!Base64.getEncoder().encodeToString(mac).equals(elem.getAsString())){
            System.out.println("Something Went wrong on the Mac");
            return null;
        }
        System.out.print("OK\n");

        System.out.print("Decrypting...");
        byte[] keyForEnc = Arrays.copyOfRange(sessionKey, (sessionKey.length/4) * 3, sessionKey.length);
        elem = payload.get("iv");
        Cipher decryCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(Base64.getDecoder().decode(elem.getAsString()));
        SecretKeySpec sks = new SecretKeySpec(keyForEnc, "AES");
        decryCipher.init(Cipher.DECRYPT_MODE, sks, ivParameterSpec);

        elem = payload.get("payload");
        byte[] fin = decryCipher.doFinal(Base64.getDecoder().decode(elem.getAsString()));
        JsonElement data = new JsonParser().parse(new InputStreamReader(new ByteArrayInputStream(fin)));
        if (data.isJsonObject()) {
            System.out.print("OK\n");
            return data.getAsJsonObject();
        }
        return null;
    }
    // Test Function Only
    public static void
    promptEnterKey(){
        System.out.println("Press \"ENTER\" to continue...");
        try {
            int read = System.in.read(new byte[2]);
            String ESC = "\033[";
            System.out.print(ESC + "2J");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
