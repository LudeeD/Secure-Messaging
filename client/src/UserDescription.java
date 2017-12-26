import com.google.gson.*;
import java.util.*;
import java.security.*;
import java.security.spec.*;
import java.security.cert.*;
import java.io.*;

class UserDescription{

    JsonObject description;        // JSON user's description
    private PrivateKey pr;

    UserDescription ( JsonObject description, PrivateKey pr ) {
        // Constructor
        // System.out.println("User Description Constructor");
        this.description = description;
        this.pr = pr;
    }

    UserDescription
    getInstance(){
        // With a uid and the result of a list commant returns a User description
        return null;
    }

    int
    getId(){
        // Return User ID from description
        return this.description.get("id").getAsInt();
    }

    boolean
    isValid(){
        // Check if Signature and PublicKey Match
        return false;
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
