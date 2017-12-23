import java.util.*;
import java.security.*;
import java.security.spec.*;
import java.nio.file.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.Base64;
import javax.xml.bind.DatatypeConverter;

class CryOperations{

    CryOperations () {
        System.out.println("Cryptography Operations!");
    }

    void
    generateKey() throws Exception{
        System.out.println("Generating Key");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();

        PublicKey pk = keyPair.getPublic();
        PrivateKey pr = keyPair.getPrivate();

        writeKey(pk, "./luis.pub");
        writeKey(pr, "./luis.key");

        PublicKey pk1 = (PublicKey) readKey(true, "./luis.pub");
        PrivateKey pr1 = (PrivateKey)readKey(false, "./luis.key");

        System.out.println(pk.equals(pk1));
        System.out.println(pr.equals(pr1));
    }

    void
    writeKey(Key k, String path) throws Exception{
        Files.write(Paths.get(path), k.getEncoded());
    }

    Key
    readKey(boolean pub, String path) throws Exception{
        System.out.println("Reading key from file");

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
