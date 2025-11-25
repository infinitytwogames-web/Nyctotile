package org.infinitytwo.nyctotile.core.security;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.json.*;

public class Authentication {
    private static final String GOOGLE_CERTS_URL = "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";
    private static final String PROJECT_ID = "umbralore-mysical-engine";
    private static final int AES_KEY_SIZE = 256;

    private static final ObjectMapper mapper = new ObjectMapper();
    
    public static FirebaseTokenPayload verify(String idToken) throws Exception {
        SignedJWT jwt = SignedJWT.parse(idToken);
        String kid = jwt.getHeader().getKeyID();
        
        // Load Google's public certs
        Map<String, String> certs = mapper.readValue(new URL(GOOGLE_CERTS_URL), Map.class);
        
        if (!certs.containsKey(kid)) {
            throw new SecurityException("Unknown key ID: " + kid);
        }
        
        // Parse X.509 certificate and extract the public key
        String certPEM = certs.get(kid);
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new java.io.ByteArrayInputStream(certPEM.getBytes()));
        RSAPublicKey publicKey = (RSAPublicKey) cert.getPublicKey();
        
        // Verify signature
        JWSVerifier verifier = new RSASSAVerifier(publicKey);
        if (!jwt.verify(verifier)) {
            throw new SecurityException("Invalid signature.");
        }
        
        // --- Extract Claims ---
        String aud = jwt.getJWTClaimsSet().getAudience().get(0);
        String iss = jwt.getJWTClaimsSet().getIssuer();
        
        // These are the claims we want:
        String uid = (String) jwt.getJWTClaimsSet().getClaim("user_id");
        String name = (String) jwt.getJWTClaimsSet().getClaim("name"); // Display Name
        String email = (String) jwt.getJWTClaimsSet().getClaim("email"); // Email is also useful
        
        // Validate Firebase-specific claims
        if (!aud.equals(PROJECT_ID)) {
            throw new SecurityException("Invalid audience: " + aud);
        }
        if (!iss.equals("https://securetoken.google.com/" + PROJECT_ID)) {
            throw new SecurityException("Invalid issuer: " + iss);
        }
        
        // Return the extended payload
        return new FirebaseTokenPayload(uid, name, email);
    }

    public record FirebaseTokenPayload(String uid, String name, String email) { }

    public static JSONObject loginWithEmail(String email, String password, String apiKey) throws IOException {
        URL url = new URL("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + apiKey);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");

        JSONObject request = new JSONObject();
        request.put("email", email);
        request.put("password", password);
        request.put("returnSecureToken", true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(request.toString().getBytes());
        }

        try (InputStream is = conn.getInputStream()) {
            String result = new String(is.readAllBytes());
            return new JSONObject(result);
        } catch (IOException e) {
            InputStream err = conn.getErrorStream();
            String error = new String(err.readAllBytes());
            throw new IOException("Firebase error: " + error);
        }
    }

    // ----------------------------------------------------------------------
    // --- NETWORK SECURITY HELPERS ---
    // ----------------------------------------------------------------------

    /**
     * Generates a random 256-bit AES secret key.
     * @return The generated AES SecretKey.
     * @throws GeneralSecurityException if key generation fails.
     */
    public static SecretKey generateAESKey() throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_SIZE);
        return keyGen.generateKey();
    }

    /**
     * Decodes a raw byte array into an RSA Public Key object.
     * This is used by the client to receive the server's public key.
     * @param encodedKey The raw byte array of the public key (X509 format).
     * @return The decoded PublicKey object.
     * @throws GeneralSecurityException if key decoding fails.
     */
    public static PublicKey decodePublicKey(byte[] encodedKey) throws GeneralSecurityException {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * Encodes a PublicKey object into a raw byte array.
     * This is used by the server to prepare its public key for transmission.
     * @param publicKey The PublicKey object to encode.
     * @return The raw byte array of the encoded public key.
     */
    public static byte[] encodePublicKey(PublicKey publicKey) {
        return publicKey.getEncoded();
    }
}