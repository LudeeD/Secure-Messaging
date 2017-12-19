import java.util.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.KeyAgreement;

class DHSession{

    KeyAgreement ka;
    KeyPair kp;
    byte[] sharedSecret = null;

    DHSession() throws Exception{
        System.out.println("Diffie Helman Session Key Exchange");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
        kpg.initialize(2048);
        kp = kpg.generateKeyPair();


        ka = KeyAgreement.getInstance("DH");
        ka.init(kp.getPrivate());
    }

    String
    getStringPubKey() throws Exception{
        return Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
    }

    void
    generateSecret( String pubK ) throws Exception{
        byte[] pubKenc = Base64.getDecoder().decode(pubK);
        KeyFactory keyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(pubKenc);
        PublicKey friendPubK = keyFac.generatePublic(x509KeySpec);
        ka.doPhase(friendPubK, true);
        sharedSecret = ka.generateSecret();
        //System.out.println("secret: " + Base64.getEncoder().encodeToString(sharedSecret));
    }
}
