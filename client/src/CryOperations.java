import java.util.*;
import java.io.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.*;
import java.nio.file.*;
import java.util.Base64;
import com.google.gson.*;



class CryOperations{

    private HashMap<String, String> nonces = new HashMap<String, String>();
    private HashMap<String, String> readMessagesSign = new HashMap<String, String>();

    CryOperations () {
        System.out.println("Cryptography Operations!");
    }

    void
    set_noncereceipt(String nonce, String msg_id){
      this.nonces.put(msg_id,nonce);
    }

    String
    get_noncereceipt(String msg_id){
        return this.nonces.get(msg_id);
    }

    void
    set_readMessageSign(String sing, String msg_id){
        System.out.println("Sign " + sing + "Msg_Id" + msg_id);
        this.readMessagesSign.put(msg_id,sing);
    }

    String
    get_readMessageSign(String msg_id){
        System.out.println("Msg_Id" + msg_id);
        return this.readMessagesSign.get(msg_id);
    }

    KeyPair
    generateKey(String path){
        try{
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair keyPair = kpg.generateKeyPair();
            if (path == null){
                System.out.println("Creating Default with Name teste");
                path = "./teste";
            }
            writeKey(keyPair.getPublic(), path + ".pub");
            writeKey(keyPair.getPrivate(), path + ".key");
            return keyPair;
        }catch (Exception e){
            System.err.print("Error Generating Key: "+e);
            System.exit(1);
        }
        return null;
    }

    void
    writeKey(Key k, String path){
        try{
            Files.write(Paths.get(path), k.getEncoded());
        }catch (Exception e){
            System.err.print("Error Writing Key: "+e);
        }
    }

    Key
    readKey(boolean pub, String path){
        try{
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
        }catch (Exception e){
            System.err.print("Error Reading Key: "+e);
            return null;
        }
    }


    //String
    //getKeyString(boolean pub){
    //    try {
    //        if(pub) return Base64.getEncoder().encodeToString(pk.getEncoded());
    //        else return Base64.getEncoder().encodeToString(pr.getEncoded());
    //    }catch (Exception e){
    //        System.err.println("Error getting Base64 encoded Key " + e);
    //        return null;
    //    }
    //}

    byte[]
    generateKeyAES(){
        try{
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            //keyGen.init(256);
            keyGen.init(128);
            Key key = keyGen.generateKey();
            System.out.println(key.getEncoded());
            return key.getEncoded();
        }catch (Exception e){
            System.err.println("Error generating AES Key " + e);
            return null;
        }
    }

    String
    encrAES(String msg,byte[] key){
        try{
            SecretKeyFactory skf;
            Cipher c;

            // Generating IV.
            System.out.print("Generating IV...");
            int ivSize = 16;
            byte[] iv = new byte[ivSize];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            System.out.print("OK\n");

            //convert key
            System.out.print("Setting Key...");
            Key originalKey = new SecretKeySpec(key, 0, key.length, "AES");
            System.out.print("OK\n");

            // ciphering
            System.out.print("Ciphering...");
            c  = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.ENCRYPT_MODE,originalKey,ivParameterSpec);
            byte[] msgenc = c.doFinal(msg.getBytes());
            System.out.print("OK\n");

            // Combine IV and encrypted part.
            System.out.print("Combine IV and ecrypted part...");
            byte[] finalMsgEnc = new byte[ivSize + msgenc.length];
            System.arraycopy(iv, 0, finalMsgEnc, 0, ivSize);
            System.arraycopy(msgenc, 0, finalMsgEnc, ivSize, msgenc.length);
            System.out.print("OK\n");

            return Base64.getEncoder().encodeToString(finalMsgEnc);
        }catch (Exception e){
            System.err.println("Error enc AES" + e);
            return null;
        }
    }

    String
    decrAES(String msgEncEnconded,byte[] key){
        try{
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
        }catch( Exception e ){
            System.err.println("Error enc AES" + e);
            return null;
        }
    }

    String
    encrRSA(String pubK, byte[] toenc){
        try{
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(pubK.getBytes()));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey pubKey = keyFactory.generatePublic(keySpec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");

            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            byte[] cipherText = cipher.doFinal(toenc);
            return Base64.getEncoder().encodeToString(cipherText);
        }catch (Exception e){
            System.err.println("Error enc RSA" + e);
            return null;
        }
    }

    byte[]
    decrRSA(String todec, PrivateKey pr){
        try{
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, pr);
            byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(todec));
            return plainText;
        }catch (Exception e){
            System.err.println("Error decr RSA" + e);
            return null;
        }
     }
     boolean verigySign(String data,  String signature, String pubK){
          try{
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(pubK.getBytes()));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey pubKey = keyFactory.generatePublic(keySpec);

            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initVerify(pubKey);
            sign.update(data.getBytes());
            return sign.verify(Base64.getDecoder().decode(signature));
        }catch(Exception e){
            System.err.println("Error Verifying Singature : "+e);
            return false;
        }
     }

    String sign(String toSign, PrivateKey pr){
        try{
           Signature sign = Signature.getInstance("SHA256withRSA");
           sign.initSign(pr);
           sign.update(toSign.getBytes());
           byte[] signature = sign.sign();
           return Base64.getEncoder().encodeToString(signature);
       }catch(Exception e){
           System.err.println("Error Signing Message : "+e);
           return null;
       }
    }

    String[]
    processPayloadSend(String payload, byte[] sessionKey){
        try{
            System.out.print("Encrypting Payload...");
            byte[] keyForMac = Arrays.copyOfRange(sessionKey, 0, sessionKey.length/2);
            byte[] keyForEnc = Arrays.copyOfRange(sessionKey, sessionKey.length/2, sessionKey.length);
            byte[] encpayload = null;
            byte[] iv = null;
            SecretKeySpec sks = new SecretKeySpec(keyForEnc, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, sks);
            iv = cipher.getIV();
            encpayload = cipher.doFinal(payload.getBytes());
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
        }catch( Exception e ){
            System.err.println("Error Processing Payload to Send " + e);
            return null;
        }
    }


    JsonObject
    processPayloadRecv( JsonObject payload,  byte[] sessionKey ){
        try{
            JsonElement elem = payload.get("type");
            if(elem.getAsString().equals("session")){
                System.out.print("Session Establishment Process");
                return payload;
            }


            elem = payload.get("payload");
            byte[] keyForMac = Arrays.copyOfRange(sessionKey, 0, sessionKey.length/2);
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec keyMAC = new SecretKeySpec(keyForMac, "HmacSHA256");
            System.out.print("Mac...");
            sha256HMAC.init(keyMAC);
            byte[] mac = sha256HMAC.doFinal(Base64.getDecoder().decode(elem.getAsString()));

            elem = payload.get("mac");
            if (!Base64.getEncoder().encodeToString(mac).equals(elem.getAsString())){
                System.out.println("Something Went wrong on the Mac");
                return null;
            }
            System.out.print("OK\n");

            System.out.print("Decrypting...");
            byte[] keyForEnc = Arrays.copyOfRange(sessionKey, sessionKey.length/2, sessionKey.length);
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
        }catch (Exception e){
            System.err.println("Error Processing Payload Received " + e);
            return null;
        }
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
