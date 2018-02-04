import java.util.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.KeyAgreement;

class DHSession{

    String type;
    KeyAgreement ka;
    KeyPair kp;
    byte[] sharedSecret = null;

    DHSession(String type) throws Exception{
        //System.out.println("Diffie Helman Session Key Exchange");
        this.type = type;
        if(type.equals("DH")){
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
            kpg.initialize(512);
            this.kp = kpg.generateKeyPair();
            this.ka = KeyAgreement.getInstance("DH");
            ka.init(kp.getPrivate());
        }else if(type.equals("EC")){
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(256);
            this.kp = kpg.generateKeyPair();
            this.ka = KeyAgreement.getInstance("ECDH");
            ka.init(kp.getPrivate());
        }
    }

    String
    getStringPubKey() throws Exception{
        //System.out.println("Sent Pubk " +kp.getPublic());
        return Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
    }

    void
    generateSecret( String pubK ) throws Exception{
        if(this.type.equals("DH")){
            byte[] pubKenc = Base64.getDecoder().decode(pubK);
            KeyFactory keyFac = KeyFactory.getInstance("DH");
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(pubKenc);
            PublicKey friendPubK = keyFac.generatePublic(x509KeySpec);
            //System.out.println(friendPubK);
            ka.doPhase(friendPubK, true);
            sharedSecret = ka.generateSecret();
            //System.out.println("secret: " + Base64.getEncoder().encodeToString(sharedSecret));
        }else if(this.type.equals("EC")){
            byte[] pubKenc = Base64.getDecoder().decode(pubK);
            KeyFactory keyFac = KeyFactory.getInstance("EC");
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(pubKenc);
            PublicKey friendPubK = keyFac.generatePublic(x509KeySpec);
            ka.doPhase(friendPubK, true);
            sharedSecret = ka.generateSecret();
        }
    }

    byte[]
    getSharedSecret(){
        return sharedSecret;
    }
}

