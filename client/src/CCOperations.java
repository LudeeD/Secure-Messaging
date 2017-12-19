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

    CCOperations () {
        System.out.println("CC Operations!");
        try{
            p = new sun.security.pkcs11.SunPKCS11(f);
            Security.addProvider( p );
            System.out.println("Addedd provider");
            ks = KeyStore.getInstance( "PKCS11", "SunPKCS11-PTeID");
            ks.load(null, null);
        }catch(Exception e){
            System.err.println("Error in CC Operations "+e);
        }
    }

    // Test Zones
    void
    printAlias() throws Exception {
        Enumeration<String> aliases = ks.aliases();
        while(aliases.hasMoreElements()){
            System.out.println("===================================");
            System.out.println(aliases.nextElement());
        }

        System.out.println("size-> "+ks.size());
        boolean xD = checkCertChain();
    }

    boolean
    checkCertChain() throws Exception {
        X509Certificate cac = (X509Certificate) ks.getCertificate("CITIZEN AUTHENTICATION CERTIFICATE");

        //System.out.println("=== CAC ===\n");
        //System.out.println(cac);

        KeyStore allCerts = KeyStore.getInstance(KeyStore.getDefaultType());
        allCerts.load(new FileInputStream("certs/CC_KS"), "password".toCharArray());
        //System.out.println("size->"+allCerts.size());
        allCerts.setCertificateEntry("CITIZEN AUTHENTICATION CERTIFICATE", cac);
        //System.out.println("size->"+allCerts.size());
        promptEnterKey();

        Enumeration<String> aliases = allCerts.aliases();
        //for (Enumeration<String> aliases = allCerts.aliases(); aliases.hasMoreElements();)
        //    System.out.println(aliases.nextElement());

        X509Certificate cert;
        String alias;
        PublicKey pubK;
        Set<TrustAnchor> anchors = new HashSet<TrustAnchor>();
        Set<X509Certificate> intermediates = new HashSet<X509Certificate>();
        while(aliases.hasMoreElements()){
            alias = aliases.nextElement();
            if(allCerts.isCertificateEntry(alias)){
                //System.out.println("Is a Certificate");
                cert = (X509Certificate) allCerts.getCertificate(alias);
                //System.out.print(cert);
                pubK = cert.getPublicKey();
                try{
                    cert.verify(pubK);
                    //System.out.println("Anchor");
                    anchors.add(new TrustAnchor(cert, null));
                }catch (Exception e){
                    //System.out.println("Intermediate");
                    intermediates.add(cert);
                }
            }else{
                System.out.println("Not a Certificate");
            }
        }

        System.out.println("Anchors Size->"+anchors.size());
        System.out.println("Intermediates Size->"+intermediates.size());


        X509CertSelector selector = new X509CertSelector();
        selector.setCertificate(cac);

        PKIXBuilderParameters pkixParams= new PKIXBuilderParameters(anchors,selector);
        pkixParams.setRevocationEnabled(false);

        CollectionCertStoreParameters interCertsParams = new CollectionCertStoreParameters(intermediates);
        CertStore interCerts = CertStore.getInstance("Collection", interCertsParams);
        pkixParams.addCertStore(interCerts);

        CollectionCertStoreParameters ccsp = new CollectionCertStoreParameters(intermediates);
        CertStore intermediateCertStore = CertStore.getInstance("Collection", ccsp);

        pkixParams.addCertStore(intermediateCertStore);

        CertPathBuilder builder = CertPathBuilder.getInstance("PKIX");
        PKIXCertPathBuilderResult path = (PKIXCertPathBuilderResult)builder.build(pkixParams);

        System.out.println(path);
        promptEnterKey();
        CertPathValidator cpv = CertPathValidator.getInstance("PKIX");
        PKIXParameters validationParams = new PKIXParameters(anchors);
        validationParams.setRevocationEnabled(false);
        validationParams.setDate(new Date());
        System.out.println(cpv.validate(path.getCertPath(),validationParams));
        return true;
    }


    String
    getUUID() throws Exception{
        System.out.println("getUUID!");
        X509Certificate cac = (X509Certificate) ks.getCertificate("CITIZEN AUTHENTICATION CERTIFICATE");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(cac.getEncoded());
        return Base64.getEncoder().encodeToString(hash);
    }

    String
    getCertString() throws Exception{
        String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
        String END_CERT = "-----END CERTIFICATE-----";
        String LINE_SEPARATOR = System.getProperty("line.separator");
        Base64.Encoder encoder = Base64.getEncoder();
        X509Certificate cac = (X509Certificate) ks.getCertificate("CITIZEN AUTHENTICATION CERTIFICATE");
        byte[] buffer = cac.getEncoded();
        return BEGIN_CERT+LINE_SEPARATOR+new String(encoder.encode(buffer))+LINE_SEPARATOR+END_CERT;
    }

    void
    writeCert() throws Exception{
        System.out.println("Write Cert");
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
        System.out.println("Read Cert");
        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        InputStream is = Files.newInputStream(Paths.get("./teste2.pem"));
        X509Certificate cer = (X509Certificate) fact.generateCertificate(is);
        System.out.println(cer.toString());
        return;
    }


    String
    sign(String toSign) throws Exception {
        System.out.println("Signing...");
        Signature sign = Signature.getInstance("SHA256withRSA", "SunPKCS11-PTeID");
        PrivateKey privKey = (PrivateKey) ks.getKey("CITIZEN AUTHENTICATION CERTIFICATE", null);
        sign.initSign(privKey);
        sign.update(toSign.getBytes());
        byte[] signature = sign.sign();
        //System.out.println("Signature: " + signature);
        return Base64.getEncoder().encodeToString(signature);

        // Verify
        //Signature verif = Signature.getInstance("SHA256withRSA"); // If I load the pkcs11 provider this will fail
        //Certificate cert = ks.getCertificate("CITIZEN AUTHENTICATION CERTIFICATE");
        //PublicKey pubK = cert.getPublicKey();
        //System.out.println(pubK);
        //verif.initVerify(pubK);
        //promptEnterKey();
        //verif.update(teste.getBytes());
        //boolean verification = verif.verify(signature);
        //System.out.print("Verification: "+verification);
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
