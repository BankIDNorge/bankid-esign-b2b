package no.bankid.esign.merchant.b2b.environment;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class PrivateKeyAssertionBuilder {
    private final SignerAndPublicJwk signerAndPublicJwk;

    public PrivateKeyAssertionBuilder(String keystorePath, String keystorePassword, String keystoreAlias) {
        this.signerAndPublicJwk = createPrivateKeyJwtSigner(keystorePath, keystorePassword, keystoreAlias);
    }

    /**
     * Returns the public key in JWK format as a Map<String, Object>. This can be used to register the
     * public key with the authorization server.
     */
    public Map<String, Object> getPublicKeyAsJsonMap() {
        return signerAndPublicJwk.publicKeyJwk().toJSONObject();
    }

    /**
     * Builds a private_key_jwt assertion for the given client id and endpoint.
     */
    public String buildAssertion(String cid, URI endPoint) {
        JWTClaimsSet claimsSet = buildPKJClaims(cid, endPoint.toString());
        try {
            SignedJWT signedJWT = new SignedJWT(signerAndPublicJwk.jwsHeader(), claimsSet);
            signedJWT.sign(signerAndPublicJwk.signer());
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Error signing private_key_jwt", e);
        }
    }

    record SignerAndPublicJwk(JWSSigner signer, JWSHeader jwsHeader, JWK publicKeyJwk) {
    }

    /**
     * Loads a private key from a keystore and creates a signer and public JWK.
     */
    private static SignerAndPublicJwk createPrivateKeyJwtSigner(String keystorePath, String keystorePassword, String keystoreAlias) {
        Path filePath = Paths.get(keystorePath);
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream is = Files.newInputStream(filePath)) {
                keyStore.load(is, keystorePassword.toCharArray());
            }
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(keystoreAlias);
            if (cert == null) {
                throw new IllegalArgumentException("Certificate not found for key alias=" + keystoreAlias + " in keystore " + keystorePath);
            }
            PublicKey certPublicKey = cert.getPublicKey();

            Key privateKey = keyStore.getKey(keystoreAlias, keystorePassword.toCharArray());

            final SignerAndPublicJwk retval;
            switch (privateKey) {
                case ECPrivateKey ecPrivateKey -> {
                    ECPublicKey publicKey = (ECPublicKey) certPublicKey;
                    JWK publicKeyJwk = new ECKey.Builder(Curve.forECParameterSpec(publicKey.getParams()), publicKey)
                            .keyUse(KeyUse.SIGNATURE)
                            .algorithm(JWSAlgorithm.parse("ES256"))
                            .keyIDFromThumbprint()
                            .build();
                    JWSHeader jwsHeader = new JWSHeader
                            .Builder(JWSAlgorithm.parse(publicKeyJwk.getAlgorithm().getName()))
                            .keyID(publicKeyJwk.getKeyID()).build();
                    retval = new SignerAndPublicJwk(new ECDSASigner(ecPrivateKey), jwsHeader, publicKeyJwk);
                }
                case RSAPrivateKey rsaPrivateKey -> {
                    RSAPublicKey publicKey = (RSAPublicKey) certPublicKey;
                    JWK publicKeyJwk = new RSAKey.Builder(publicKey)
                            .keyUse(KeyUse.SIGNATURE)
                            .algorithm(JWSAlgorithm.parse("RS256"))
                            .keyIDFromThumbprint()
                            .build();
                    JWSHeader jwsHeader = new JWSHeader
                            .Builder(JWSAlgorithm.parse(publicKeyJwk.getAlgorithm().getName()))
                            .keyID(publicKeyJwk.getKeyID()).build();
                    retval = new SignerAndPublicJwk(new RSASSASigner(rsaPrivateKey), jwsHeader, publicKeyJwk);
                }
                default ->
                        throw new IllegalArgumentException("Private key is not an EC or RSA private key in keystore " + keystorePath);
            }
            System.out.println(
                    "\nKey is read for alias : " + keystoreAlias +
                            "\nKey is for certificate: " + cert.getSubjectX500Principal() +
                            "\nKey will use this  kid: " + retval.publicKeyJwk().getKeyID());
            return retval;
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException |
                 UnrecoverableKeyException | JOSEException e) {
            throw new RuntimeException("Error getting key from keyStore" + keystorePath, e);
        }
    }

    /**
     * Builds the claims for authentication to the keycloak server. They should be signed before sent.
     */
    private static JWTClaimsSet buildPKJClaims(String cid, String targetUrl) {
        long epochMilli = Instant.now().toEpochMilli();
        return new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .issuer(cid)
                .subject(cid)
                .audience(targetUrl)
                .expirationTime(new Date(epochMilli + 30L * 1000L))
                .issueTime(new Date(epochMilli))
                .build();
    }

}
