import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.*;
import java.nio.file.*;
<<<<<<< HEAD
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.Base64;
import javax.xml.bind.DatatypeConverter;
=======
import com.google.gson.*;
import java.io.*;

>>>>>>> 020da1f9da51b8952b9686aa2ec38578065d20eb

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

    byte[]
    generateKeyAES() throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeySpecException{
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        Key key = keyGen.generateKey();
        System.out.println(key.getEncoded());

        return key.getEncoded();
      }

      byte[]
      encrAES(String msg,byte[] key) throws Throwable {
        SecretKeyFactory skf;
        Cipher c;

        // Generating IV.
       int ivSize = 16;
       byte[] iv = new byte[ivSize];
       SecureRandom random = new SecureRandom();
       random.nextBytes(iv);
       IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
       //convert key
       Key originalKey = new SecretKeySpec(key, 0, key.length, "AES");

       // ciphering
        c  = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE,originalKey,ivParameterSpec);
        byte[] msgenc = c.doFinal(msg.getBytes());

          // Combine IV and encrypted part.
         byte[] finalMsgEnc = new byte[ivSize + msgenc.length];
         System.arraycopy(iv, 0, finalMsgEnc, 0, ivSize);
         System.arraycopy(msgenc, 0, finalMsgEnc, ivSize, msgenc.length);

        return Base64.getEncoder().encode(finalMsgEnc);

      }

      String
      decrAES(byte[] msgEncEnconded,byte[] key) throws Throwable {
        int ivSize = 16;
        byte[] msgEnc = Base64.getDecoder().decode(msgEncEnconded);

        Key originalKey = new SecretKeySpec(key, 0, key.length, "AES");

        // Extract IV.
        byte[] iv = new byte[ivSize];
        System.arraycopy(msgEnc, 0, iv, 0, iv.length);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);


        int encryptedSize = msgEnc.length - ivSize;
        byte[] encryptedBytes = new byte[encryptedSize];
        System.arraycopy(msgEnc, ivSize, encryptedBytes, 0, encryptedSize);


        Cipher cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipherDecrypt.init(Cipher.DECRYPT_MODE, originalKey, ivParameterSpec);
        byte[] decrypted = cipherDecrypt.doFinal(encryptedBytes);

        return new String(decrypted);

      }
      byte[]
      encrRSA(String pubK,byte[] toenc) throws NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeySpecException, NoSuchPaddingException,InvalidKeyException,BadPaddingException{

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(pubK.getBytes()));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey pubKey = keyFactory.generatePublic(keySpec);

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");

        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        byte[] cipherText = cipher.doFinal(toenc);
        return Base64.getEncoder().encode(cipherText);
      }

      byte[]
      decrRSA(String privKey,byte[] todec) throws NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeySpecException, NoSuchPaddingException,InvalidKeyException,BadPaddingException{

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(privKey.getBytes());
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");

        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] plainText = cipher.doFinal(todec);
        return plainText;
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
