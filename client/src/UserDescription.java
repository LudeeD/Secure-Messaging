import com.google.gson.*;
import java.util.*;
import java.security.*;
import java.security.spec.*;
import java.security.cert.*;
import java.io.*;

class UserDescription{

    private JsonObject description;        // JSON user's description
    private PrivateKey pr;

    UserDescription ( JsonObject description, PrivateKey pr ) {
        // Constructor
        // System.out.println("User Description Constructor");
        this.description = description;
        this.pr = pr;
    }

    int
    getId(){
        // Return User ID from description
        return this.description.get("id").getAsInt();
    }

    String
    getUUID(){
        // Return User ID from description
        return this.description.get("uuid").getAsString();
    }

    boolean
    isValid( CCOperations cc ){
        // Check if Signature and PublicKey

        String ln = System.getProperty("line.separator");
        String signature =  this.description.get("signature").getAsString();
        String toVerify  =  this.description.get("uuid").getAsString()+ln+
                            getPublicKeyString()+ln+
                            getCertString();
        PublicKey pubK = getCertKey();
        System.out.println("Signature Valid: "+cc.verifySign( toVerify, signature, pubK ));
        System.out.println("Cert Chain Valid: " + cc.checkCertChain(getCert()));
        return cc.verifySign( toVerify, signature, pubK ) && cc.checkCertChain(getCert());
    }

    PublicKey
    getPublicKey(){
        // Returns Public Key
        try{
            byte[] pubk = Base64.getDecoder().decode(getPublicKeyString());
            KeyFactory kf = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec ks = new X509EncodedKeySpec(pubk);
            return kf.generatePublic(ks);
        }catch(Exception e){
            System.err.print("Error geting User pubk");
            System.exit(1);
        }
        return null;
    }

    PublicKey
    getCertKey(){
        try{
            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            X509Certificate cer = (X509Certificate) fact.generateCertificate(
                    new ByteArrayInputStream(getCertString().getBytes()));
            return cer.getPublicKey();
        }catch(Exception e){
            System.err.print("Error geting User pubk");
            System.exit(1);
        }
        return null;
    }

    X509Certificate
    getCert(){
        try{
            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            X509Certificate cer = (X509Certificate) fact.generateCertificate(
                    new ByteArrayInputStream(getCertString().getBytes()));
            return cer;
        }catch(Exception e){
            System.err.print("Error geting User Certificate");
            System.exit(1);
        }
        return null;
    }

    PrivateKey
    getPrivateKey(){
        return this.pr;
    }

    String
    getPublicKeyString(){
        // Returns Public Key
        return this.description.get("pubk").getAsString();
    }

    String
    getCertString(){
        return this.description.get("cert").getAsString();
    }
}
