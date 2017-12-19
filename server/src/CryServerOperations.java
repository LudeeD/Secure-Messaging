import java.util.*;
import java.security.*;
import java.security.spec.*;
import java.nio.file.*;
import java.io.*;
import java.security.cert.CertificateFactory;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

class CryServerOperations{

    CryServerOperations () {
        System.out.println("Cryptography Operations!");
    }

    Certificate
    readCert() throws Exception{
        System.out.println("Reading Cert to Server KeyStore");
        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        InputStream is = Files.newInputStream(Paths.get("./certs/example.com.crt"));
        X509Certificate cer = (X509Certificate) fact.generateCertificate(is);
        System.out.println(cer.toString());
        return cer;
    }

    PrivateKey
    readKey() throws Exception{
        System.out.println("Reading Key to Server KeyStore");
        Path p = Paths.get("./certs/example.com.key.der");
        byte[] bytes = Files.readAllBytes(p);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(bytes);
        return kf.generatePrivate(ks);
    }

    String
    getCertString() throws Exception{
        String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
        String END_CERT = "-----END CERTIFICATE-----";
        String LINE_SEPARATOR = System.getProperty("line.separator");
        Base64.Encoder encoder = Base64.getEncoder();
        X509Certificate cac = (X509Certificate) readCert();
        byte[] buffer = cac.getEncoded();
        return BEGIN_CERT+LINE_SEPARATOR+new String(encoder.encode(buffer))+LINE_SEPARATOR+END_CERT;
    }

    String
    sign(String toSign) throws Exception {
        System.out.println("Signing...");
        Signature sign = Signature.getInstance("SHA256withRSA");
        PrivateKey privKey = readKey();
        sign.initSign(privKey);
        sign.update(toSign.getBytes());
        byte[] signature = sign.sign();
        return Base64.getEncoder().encodeToString(signature);
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
