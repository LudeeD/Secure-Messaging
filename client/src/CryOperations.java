import java.util.*;
import java.security.*;
import java.security.spec.*;
import java.nio.file.*;

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
