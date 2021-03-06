import java.util.Enumeration;
import java.security.*;
import java.security.cert.Certificate;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.security.cert.*;

class CCOperations{
    String f = "./src/CitizenCard.cfg";
    Provider p;
    KeyStore ks;
    boolean provider = false;

    CCOperations () {
        provider = addProvider();
    }

    boolean
    addProvider(){
        try{
            p = new sun.security.pkcs11.SunPKCS11(f);
            Security.addProvider( p );
            //System.out.println("Addedd provider");
            ks = KeyStore.getInstance( "PKCS11", "SunPKCS11-PTeID");
            ks.load(null, null);
            provider = true;
            return provider;
        }catch(Exception e){
            System.err.println("Error in CC Add Provider"+e);
            provider = false;
            return provider;
        }
    }

    boolean
    checkCertChain(X509Certificate cac, boolean initSession){
        try{
            // X509Certificate cac = (X509Certificate) ks.getCertificate("CITIZEN AUTHENTICATION CERTIFICATE");
            //
            // System.out.println("=== CAC ===\n");
            // System.out.println(cac);

            KeyStore allCerts = KeyStore.getInstance(KeyStore.getDefaultType());
            allCerts.load(new FileInputStream("certs/CC_KS"), "password".toCharArray());
            // System.out.println("size->"+allCerts.size());
            allCerts.setCertificateEntry("CITIZEN AUTHENTICATION CERTIFICATE", cac);
            // System.out.println("size->"+allCerts.size());
            X509Certificate ca = readCertCA();
            System.out.print(ca);
            allCerts.setCertificateEntry("SERVER CA", ca);

            Enumeration<String> aliases = allCerts.aliases();
            //for (Enumeration<String> aliases = allCerts.aliases(); aliases.hasMoreElements();)
            //    System.out.println(aliases.nextElement());

            X509Certificate cert;
            String alias;
            PublicKey pubK;
            Set<TrustAnchor> anchors = new HashSet<TrustAnchor>();
            Set<X509Certificate> intermediates = new HashSet<X509Certificate>();
            while(aliases.hasMoreElements()){
                // promptEnterKey();
                alias = aliases.nextElement();
                if(allCerts.isCertificateEntry(alias)){
                    // System.out.println("Is a Certificate");
                    cert = (X509Certificate) allCerts.getCertificate(alias);
                    // System.out.print(cert);
                    pubK = cert.getPublicKey();
                    try{
                        cert.verify(pubK);
                        // System.out.println("Anchor");
                        anchors.add(new TrustAnchor(cert, null));
                    }catch (Exception e){
                        // System.out.println("Intermediate");
                        intermediates.add(cert);
                    }
                }else{
                    System.out.println("Not a Certificate");
                }
            }

            //System.out.println("Anchors Size->"+anchors.size());
            //System.out.println("Intermediates Size->"+intermediates.size());
            if(!initSession){
                Security.setProperty("ocsp.enable", "true");
                System.setProperty("com.sun.security.enableCRLDP", "true");
            }
            X509CertSelector selector = new X509CertSelector();
            selector.setCertificate(cac);

            PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(anchors,selector);
            if(initSession) pkixParams.setRevocationEnabled(false);

            CollectionCertStoreParameters interCertsParams = new CollectionCertStoreParameters(intermediates);
            CertStore interCerts = CertStore.getInstance("Collection", interCertsParams);
            pkixParams.addCertStore(interCerts);

            CollectionCertStoreParameters ccsp = new CollectionCertStoreParameters(intermediates);
            CertStore intermediateCertStore = CertStore.getInstance("Collection", ccsp);

            pkixParams.addCertStore(intermediateCertStore);

            CertPathBuilder builder = CertPathBuilder.getInstance("PKIX");
            PKIXCertPathBuilderResult path = (PKIXCertPathBuilderResult)builder.build(pkixParams);

            CertPathValidator cpv = CertPathValidator.getInstance("PKIX");
            PKIXParameters validationParams = new PKIXParameters(anchors);
            if(initSession) validationParams.setRevocationEnabled(false);
            validationParams.setDate(new Date());

            try{
                cpv.validate(path.getCertPath(),validationParams);
                return true;
            }catch (Exception e){
                System.err.println("Could not validate Certificate Chain "+e);
                return false;
            }

        }catch (Exception e){
            System.err.println("Error Certifying chain " +e);
            return false;
        }
   }


    String
    getUUID(){
        try{
            X509Certificate cac = (X509Certificate) ks.getCertificate("CITIZEN AUTHENTICATION CERTIFICATE");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(cac.getEncoded());
            return Base64.getEncoder().encodeToString(hash);
        }catch (Exception e){
            System.err.println("Error Getting Client UUID : " + e);
            return null;
        }
    }

    String
    getCertString(){
        try{
            String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
            String END_CERT = "-----END CERTIFICATE-----";
            String LINE_SEPARATOR = System.getProperty("line.separator");
            Base64.Encoder encoder = Base64.getEncoder();
            X509Certificate cac = (X509Certificate) ks.getCertificate("CITIZEN AUTHENTICATION CERTIFICATE");
            byte[] buffer = cac.getEncoded();
            return BEGIN_CERT+LINE_SEPARATOR+new String(encoder.encode(buffer))+LINE_SEPARATOR+END_CERT;
        }catch (Exception e){
            System.err.println("Error Getting Client Cert String : " + e);
            return null;
        }
    }

    void
    writeCert() throws Exception{
        //System.out.println("Write Cert");
        String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
        String END_CERT = "-----END CERTIFICATE-----";
        String LINE_SEPARATOR = System.getProperty("line.separator");
        Base64.Encoder encoder = Base64.getMimeEncoder(64, LINE_SEPARATOR.getBytes());
        X509Certificate cac = (X509Certificate) ks.getCertificate("CITIZEN AUTHENTICATION CERTIFICATE");
        byte[] buffer = cac.getEncoded();
        String cert = BEGIN_CERT+LINE_SEPARATOR+new String(encoder.encode(buffer))+LINE_SEPARATOR+END_CERT;
        Files.write(Paths.get("./teste2.pem"), cert.getBytes());
        return;
    }

    void
    readCert() throws Exception{
        //System.out.println("Read Cert");
        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        InputStream is = Files.newInputStream(Paths.get("./teste2.pem"));
        X509Certificate cer = (X509Certificate) fact.generateCertificate(is);
        //System.out.println(cer.toString());
        return;
    }

    X509Certificate
    readCertCA() throws Exception{
        //System.out.println("Read Cert");
        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        InputStream is = Files.newInputStream(Paths.get("./certs/myCA.crt"));
        X509Certificate cer = (X509Certificate) fact.generateCertificate(is);
        //System.out.println(cer.toString());
        return cer;
    }

    String
    sign(String toSign){
        try{
            Signature sign = Signature.getInstance("SHA256withRSA", "SunPKCS11-PTeID");
            PrivateKey privKey = (PrivateKey) ks.getKey("CITIZEN AUTHENTICATION CERTIFICATE", null);
            sign.initSign(privKey);
            sign.update(toSign.getBytes());
            byte[] signature = sign.sign();
            return Base64.getEncoder().encodeToString(signature);
        }catch (Exception e){
            System.err.println("Error Signing with CC : " + e);
            return null;
        }
    }

    boolean
    verifySign(String toVerify, String signature, PublicKey pubk){
        try{
            Signature verif = Signature.getInstance("SHA256withRSA"); // If I load the pkcs11 provider this will fail
            verif.initVerify(pubk);
            verif.update(toVerify.getBytes());
            boolean verification = verif.verify(Base64.getDecoder().decode(signature));
            return verification;
        }catch (Exception e){
            System.err.println("Error Verifying Signature with CC : " + e);
            return false;
        }
   }

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
