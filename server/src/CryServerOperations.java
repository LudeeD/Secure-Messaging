import java.util.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.file.*;
import java.io.*;
import java.security.cert.CertificateFactory;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import com.google.gson.*;

class CryServerOperations{

    CryServerOperations () {
        System.out.println("Cryptography Operations!");
    }

    Certificate
    readCert(){
        try{
            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            InputStream is = Files.newInputStream(Paths.get("./certs/example.com.crt"));
            X509Certificate cer = (X509Certificate) fact.generateCertificate(is);
            return cer;
        }catch(Exception e){
            System.err.println("Error Reading Cert : "+e);
            return null;
        }
    }

    PrivateKey
    readKey(){
        try{
            System.out.println("Reading Key of Server from File");
            Path p = Paths.get("./certs/example.com.key.der");
            byte[] bytes = Files.readAllBytes(p);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(bytes);
            return kf.generatePrivate(ks);
        }catch ( Exception e ){
            System.err.println("Error Reading Key : "+e);
            return null;
        }
   }

    String
    getCertString() throws Exception{
        try{
            String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
            String END_CERT = "-----END CERTIFICATE-----";
            String LINE_SEPARATOR = System.getProperty("line.separator");
            Base64.Encoder encoder = Base64.getEncoder();
            X509Certificate cac = (X509Certificate) readCert();
            byte[] buffer = cac.getEncoded();
            return BEGIN_CERT+LINE_SEPARATOR+new String(encoder.encode(buffer))+LINE_SEPARATOR+END_CERT;
        }catch( Exception e ){
            System.err.println("Error Getting Cert String: "+e);
            return null;
        }
    }

    String
    sign(String toSign){
        try{
            Signature sign = Signature.getInstance("SHA256withRSA");
            PrivateKey privKey = readKey();
            sign.initSign(privKey);
            sign.update(toSign.getBytes());
            byte[] signature = sign.sign();
            return Base64.getEncoder().encodeToString(signature);
        }catch(Exception e){
            System.err.println("Error Signing in the Server : "+e);
            return null;
        }
    }

    String
    generateNonce() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] nonce = new byte[16];
        secureRandom.nextBytes(nonce);
        return Base64.getEncoder().encodeToString(nonce);
    }

    String[]
    processPayloadSend(String payload, byte[] sessionKey){
        try{
            // TODO 256 bit its too long for java, additional files are required
            byte[] keyForMac = Arrays.copyOfRange(sessionKey, 0, sessionKey.length/2);
            byte[] keyForEnc = Arrays.copyOfRange(sessionKey, sessionKey.length/2, sessionKey.length);
            System.out.print("Encrypting Payload..");
            byte[] encpayload = null;
            byte[] iv = null;
            SecretKeySpec sks = new SecretKeySpec(keyForEnc, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, sks);
            iv = cipher.getIV();
            encpayload = cipher.doFinal(payload.getBytes());
            System.out.print("OK\n");
            System.out.print("Generate Mac...");
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec keyMAC = new SecretKeySpec(keyForMac, "HmacSHA256");
            sha256HMAC.init(keyMAC);
            byte[] mac = sha256HMAC.doFinal(encpayload);
            System.out.print("OK\n");
            String[] result = new String[3];
            result[0] = Base64.getEncoder().encodeToString(encpayload);
            result[1] = Base64.getEncoder().encodeToString(iv);
            result[2] = Base64.getEncoder().encodeToString(mac);
            return result;
        }catch (Exception e){
            System.err.println("Error Processing Payload in: "+e);
            return null;
        }
    }

    JsonObject
    processPayloadRecv( JsonObject payload,  byte[] sessionKey ) throws Exception{
        try{
            JsonElement elem = payload.get("type");
            if(elem.getAsString().equals("session")){
                System.out.print("Session Establishment Process");
                return payload;
            }
            elem = payload.get("payload");
            byte[] keyForMac = Arrays.copyOfRange(sessionKey, 0, sessionKey.length/2);
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec keyMAC = new SecretKeySpec(keyForMac, "HmacSHA256");
            System.out.println("Mac Key:"+Base64.getEncoder().encodeToString(keyMAC.getEncoded()));
            sha256HMAC.init(keyMAC);
            byte[] mac = sha256HMAC.doFinal(Base64.getDecoder().decode(elem.getAsString()));

            //System.out.println("Calculated " +Base64.getEncoder().encodeToString(mac) );
            //System.out.println("Received " + payload.get("mac").getAsString());

            System.out.print("MAC...");
            elem = payload.get("mac");
            if (!Base64.getEncoder().encodeToString(mac).equals(elem.getAsString())){
                System.out.println("Something Went wrong on the Mac");
                return null;
            }
            System.out.print("OK\n");

            byte[] keyForEnc = Arrays.copyOfRange(sessionKey, sessionKey.length/2, sessionKey.length);
            elem = payload.get("iv");
            Cipher decryCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(Base64.getDecoder().decode(elem.getAsString()));
            SecretKeySpec sks = new SecretKeySpec(keyForEnc, "AES");
            decryCipher.init(Cipher.DECRYPT_MODE, sks, ivParameterSpec);

            elem = payload.get("payload");
            byte[] fin = decryCipher.doFinal(Base64.getDecoder().decode(elem.getAsString()));
            JsonElement data = new JsonParser().parse(new InputStreamReader(new ByteArrayInputStream(fin)));
            if (data.isJsonObject()) {
                System.out.println("Sucess!");
                return data.getAsJsonObject();
            }
            return null;
        }catch (Exception e){
            System.err.println("Error Processing Receved Payload: "+e);
            return null;
        }
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
