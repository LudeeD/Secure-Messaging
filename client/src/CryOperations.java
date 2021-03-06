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

    SecureRandom random = new SecureRandom();

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
        //System.out.println("Sign " + sing + "Msg_Id" + msg_id);
        this.readMessagesSign.put(msg_id,sing);
    }

    String
    get_readMessageSign(String msg_id){
        //System.out.println("Msg_Id" + msg_id);
        return this.readMessagesSign.get(msg_id);
    }

    KeyPair
    generateKey(String path, String passphrase, String salt){
        try{
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt.getBytes(), 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair keyPair = kpg.generateKeyPair();
            if (path == null){
                System.out.println("Creating Default with Name teste");
                path = "./teste";
            }
            writeKey(keyPair.getPublic(), path + ".pub", null);
            writeKey(keyPair.getPrivate(), path + ".key", secret);
            return keyPair;
        }catch (Exception e){
            System.err.print("Error Generating Key: "+e);
            System.exit(1);
        }
        return null;
    }

    void
    writeKey(Key k, String path, SecretKey secret){
        try{
            if (secret == null) Files.write(Paths.get(path), k.getEncoded());
            else{
                String key = Base64.getEncoder().encodeToString(k.getEncoded());
                String encr = encrAESKey(key,secret.getEncoded());
                List<String> w = new ArrayList<String>();
                w.add(encr);
                Files.write(Paths.get(path), w);
            }
        }catch (Exception e){
            System.err.print("Error Writing Key: "+e);
        }
    }

    Key
    readKey(boolean pub, String path, String passphrase, String salt){
        try{
            Path p = Paths.get(path);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            if (pub){
                byte[] bytes = Files.readAllBytes(p);
                X509EncodedKeySpec ks = new X509EncodedKeySpec(bytes);
                return kf.generatePublic(ks);
            }else{
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt.getBytes(), 65536, 256);
                SecretKey tmp = factory.generateSecret(spec);
                SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

                String encr = Files.readAllLines(p).get(0);
                String decr = decrAESKey(encr, secret.getEncoded());
                byte[] bytes = Base64.getDecoder().decode(decr);
                PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(bytes);
                return kf.generatePrivate(ks);
            }
        }catch (Exception e){
            System.err.print("Error Reading Keys");
            System.exit(1);
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
    generateKeyAES(String mode){
        try{
            KeyGenerator keyGen;
            Key key = null;
            if(mode.equals("AES")){
                keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256);
                //keyGen.init(128);
                key = keyGen.generateKey();
                //System.out.println(key.getEncoded());
            }
            if(mode.equals("3DES")){
                keyGen = KeyGenerator.getInstance("DESede");
                keyGen.init(168);
                key = keyGen.generateKey();
            }
            return key.getEncoded();
        }catch (Exception e){
            System.err.println("Error generating AES Key " + e);
            return null;
        }
    }

    String
    encrAESKey(String msg,byte[] key){
        try{
            SecretKeyFactory skf;
            Cipher c;
            int ivSize = 0;
            byte[] iv;
            SecureRandom random = new SecureRandom();
            IvParameterSpec ivParameterSpec;
            Key originalKey;
            byte[] msgenc;
            byte[] finalMsgEnc = null;
            //System.out.print("Generating IV...");
            ivSize = 16;
            iv = new byte[ivSize];
            random.nextBytes(iv);
            ivParameterSpec = new IvParameterSpec(iv);
            //System.out.print("OK\n");

            //System.out.print("Setting Key...");
            originalKey = new SecretKeySpec(key, 0, key.length, "AES");
            //System.out.print("OK\n");

            //System.out.print("Ciphering...");
            c  = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.ENCRYPT_MODE,originalKey,ivParameterSpec);
            msgenc = c.doFinal(msg.getBytes());
            //System.out.print("OK\n");

            //System.out.print("Combine IV and ecrypted part...");
            finalMsgEnc = new byte[ivSize + msgenc.length];
            System.arraycopy(iv, 0, finalMsgEnc, 0, ivSize);
            System.arraycopy(msgenc, 0, finalMsgEnc, ivSize, msgenc.length);
            //System.out.print("OK\n");
            return Base64.getEncoder().encodeToString(finalMsgEnc);
        }catch (Exception e){
            System.err.println("Error enc AES" + e);
            return null;
        }
    }
    
    String
    encrAES(String msg,byte[] key, String mode){
        try{
            SecretKeyFactory skf;
            Cipher c;
            int ivSize = 0;
            byte[] iv;
            SecureRandom random = new SecureRandom();
            IvParameterSpec ivParameterSpec;
            Key originalKey;
            byte[] msgenc;
            byte[] finalMsgEnc = null;
            if(mode.equals("AES")){
                //System.out.print("Generating IV...");
                ivSize = 16;
                iv = new byte[ivSize];
                random.nextBytes(iv);
                ivParameterSpec = new IvParameterSpec(iv);
                //System.out.print("OK\n");

                //System.out.print("Setting Key...");
                originalKey = new SecretKeySpec(key, 0, key.length, "AES");
                //System.out.print("OK\n");

                //System.out.print("Ciphering...");
                c  = Cipher.getInstance("AES/CBC/PKCS5Padding");
                c.init(Cipher.ENCRYPT_MODE,originalKey,ivParameterSpec);
                msgenc = c.doFinal(msg.getBytes());
                //System.out.print("OK\n");

                //System.out.print("Combine IV and ecrypted part...");
                finalMsgEnc = new byte[ivSize + msgenc.length];
                System.arraycopy(iv, 0, finalMsgEnc, 1, ivSize);
                System.arraycopy(msgenc, 0, finalMsgEnc, ivSize+1, msgenc.length);
                finalMsgEnc[0] = 0x01;
                //System.out.print("OK\n");
            }
            if(mode.equals("3DES")){
                //System.out.print("Generating IV...");
                ivSize = 8;
                iv = new byte[ivSize];
                random.nextBytes(iv);
                ivParameterSpec = new IvParameterSpec(iv);
                //System.out.print("OK\n");

                //System.out.print("Setting Key...");
                originalKey = new SecretKeySpec(key, 0, key.length, "DESede");
                //System.out.print("OK\n");

                //System.out.print("Ciphering...");
                c  = Cipher.getInstance("DESede/CBC/PKCS5Padding");
                c.init(Cipher.ENCRYPT_MODE,originalKey,ivParameterSpec);
                msgenc = c.doFinal(msg.getBytes());
                //System.out.print("OK\n");

                //System.out.print("Combine IV and ecrypted part...");
                finalMsgEnc = new byte[1+ ivSize + msgenc.length];
                System.arraycopy(iv, 0, finalMsgEnc, 1, ivSize);
                System.arraycopy(msgenc, 0, finalMsgEnc, ivSize+1, msgenc.length);
                finalMsgEnc[0] = 0x02;
                //System.out.print("OK\n");
            }

            return Base64.getEncoder().encodeToString(finalMsgEnc);
        }catch (Exception e){
            System.err.println("Error enc AES" + e);
            return null;
        }
    }

    String
    decrAESKey(String msgEncEnconded,byte[] key){
        try{
            byte[] msgEnc = Base64.getDecoder().decode(msgEncEnconded);
            // Type of encryption
            int ivSize;
            Key originalKey;
            byte[] iv;
            IvParameterSpec ivParameterSpec;
            int encryptedSize;
            byte[] encryptedBytes;
            Cipher cipherDecrypt;
            byte[] decrypted;

            ivSize = 16;
            originalKey = new SecretKeySpec(key, 0, key.length, "AES");

            iv = new byte[ivSize];
            System.arraycopy(msgEnc, 0, iv, 0, iv.length);
            ivParameterSpec = new IvParameterSpec(iv);

            encryptedSize = msgEnc.length - ivSize;
            encryptedBytes = new byte[encryptedSize];
            System.arraycopy(msgEnc, ivSize, encryptedBytes, 0, encryptedSize);

            cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipherDecrypt.init(Cipher.DECRYPT_MODE, originalKey, ivParameterSpec);
            decrypted = cipherDecrypt.doFinal(encryptedBytes);
            return new String(decrypted);
        }catch( Exception e ){
            System.err.println("Error on decr" + e);
            return null;
        }
    }

    String
    decrAES(String msgEncEnconded,byte[] key){
        try{
            byte[] msgEnc = Base64.getDecoder().decode(msgEncEnconded);
            // Type of encryption

            int ivSize;
            Key originalKey;
            byte[] iv;
            IvParameterSpec ivParameterSpec;
            int encryptedSize;
            byte[] encryptedBytes;
            Cipher cipherDecrypt;
            byte[] decrypted;
            if (msgEnc[0] == 0x01){
                ivSize = 16;
                originalKey = new SecretKeySpec(key, 0, key.length, "AES");

                iv = new byte[ivSize];
                System.arraycopy(msgEnc, 1, iv, 0, iv.length);
                ivParameterSpec = new IvParameterSpec(iv);

                encryptedSize = msgEnc.length - ivSize - 1;
                encryptedBytes = new byte[encryptedSize];
                System.arraycopy(msgEnc, ivSize+1, encryptedBytes, 0, encryptedSize);

                cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipherDecrypt.init(Cipher.DECRYPT_MODE, originalKey, ivParameterSpec);
                decrypted = cipherDecrypt.doFinal(encryptedBytes);
            }else{
                ivSize = 8;
                originalKey = new SecretKeySpec(key, 0, key.length, "DESede");

                iv = new byte[ivSize];
                System.arraycopy(msgEnc, 1, iv, 0, iv.length);
                ivParameterSpec = new IvParameterSpec(iv);

                encryptedSize = msgEnc.length - ivSize - 1;
                encryptedBytes = new byte[encryptedSize];
                System.arraycopy(msgEnc, ivSize+1, encryptedBytes, 0, encryptedSize);

                cipherDecrypt = Cipher.getInstance("DESede/CBC/PKCS5Padding");
                cipherDecrypt.init(Cipher.DECRYPT_MODE, originalKey, ivParameterSpec);
                decrypted = cipherDecrypt.doFinal(encryptedBytes);

            }
            return new String(decrypted);
        }catch( Exception e ){
            System.err.println("Error on decr" + e);
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
            //System.out.print("Encrypting Payload...");
            byte[] keyForMac = Arrays.copyOfRange(sessionKey, 0, sessionKey.length/2);
            byte[] keyForEnc = Arrays.copyOfRange(sessionKey, sessionKey.length/2, sessionKey.length);
            byte[] encpayload = null;
            byte[] iv = null;
            SecretKeySpec sks = new SecretKeySpec(keyForEnc, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, sks);
            iv = cipher.getIV();
            encpayload = cipher.doFinal(payload.getBytes());
            //System.out.print("OK\n");

            //System.out.print("Generate Mac...");
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec keyMAC = new SecretKeySpec(keyForMac, "HmacSHA256");
            sha256HMAC.init(keyMAC);
            byte[] mac = sha256HMAC.doFinal(encpayload);
            //System.out.print("OK\n");

            String[] result = new String[4];
            result[0] = Base64.getEncoder().encodeToString(encpayload);
            result[1] = Base64.getEncoder().encodeToString(iv);
            result[2] = Base64.getEncoder().encodeToString(mac);

            // Add nonces
            // System.out.print("Generate Nonce...");
            byte bytes[] = new byte[8];
            random.nextBytes(bytes);
            //System.out.print(Base64.getEncoder().encodeToString(bytes)+ " OK\n");
            result[3] = Base64.getEncoder().encodeToString(bytes);
            return result;
        }catch( Exception e ){
            System.err.println("Error Processing Payload to Send " + e);
            return null;
        }
    }


    JsonObject
    processPayloadRecv( JsonObject payload,  byte[] sessionKey, String expectedNonce ){
        try{
            JsonElement elem = payload.get("type");
            if(elem.getAsString().equals("session")){
                System.out.print("Session Establishment Process");
                return payload;
            }
            elem = payload.get("nonce");
            //System.out.println("Payload Recv" + elem.getAsString());
            if( !elem.getAsString().equals(expectedNonce) ){
                System.err.println("Something went wrong with the nonce expected");
                return null;
            }

            elem = payload.get("payload");
            byte[] keyForMac = Arrays.copyOfRange(sessionKey, 0, sessionKey.length/2);
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec keyMAC = new SecretKeySpec(keyForMac, "HmacSHA256");
            sha256HMAC.init(keyMAC);
            byte[] mac = sha256HMAC.doFinal(Base64.getDecoder().decode(elem.getAsString()));

            elem = payload.get("mac");
            if (!Base64.getEncoder().encodeToString(mac).equals(elem.getAsString())){
                System.out.println("Something Went wrong on the Mac");
                return null;
            }

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
