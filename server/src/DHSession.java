import java.util.*;
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.interfaces.*;

class DHSession{

    KeyAgreement ka;
    KeyPair kp;
    byte[] sharedSecret = null;
    PublicKey friendPubK;

    DHSession( String pubK , String type) throws Exception{
        System.out.println("Key Exchange Server");
        if(type.equals("DH")){
            System.out.println("Diffie Helman");
            byte[] pubKenc = Base64.getDecoder().decode(pubK);
            KeyFactory keyFac = KeyFactory.getInstance("DH");
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(pubKenc);
            friendPubK = keyFac.generatePublic(x509KeySpec);
            DHParameterSpec dhParam = ((DHPublicKey)friendPubK).getParams();

            KeyPairGenerator KG = KeyPairGenerator.getInstance("DH");
            KG.initialize(dhParam);
            kp = KG.generateKeyPair();

            ka = KeyAgreement.getInstance("DH");
            ka.init(kp.getPrivate());
            return;
        }
        if(type.equals("EC")){
            System.out.println("Elliptic Curves");
            byte[] pubKenc = Base64.getDecoder().decode(pubK);
            KeyFactory keyFac = KeyFactory.getInstance("EC");
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(pubKenc);
            friendPubK = keyFac.generatePublic(x509KeySpec);

            ECParameterSpec dhParam = ((ECPublicKey)friendPubK).getParams();
            KeyPairGenerator KG = KeyPairGenerator.getInstance("EC");
            KG.initialize(dhParam);
            kp = KG.generateKeyPair();

            ka = KeyAgreement.getInstance("ECDH");
            ka.init(kp.getPrivate());
        }
    }

    String
    getStringPubKey() throws Exception{
        //System.out.println("Sent Pubk " +kp.getPublic());
        return Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
    }

    void
    generateSecret() throws Exception{
        //System.out.println(friendPubK);
        ka.doPhase(friendPubK, true);
        sharedSecret = ka.generateSecret();
        //System.out.println("secret: " + Base64.getEncoder().encodeToString(sharedSecret));
    }

    byte[]
    getSharedSecret(){
        return sharedSecret;
    }
}

