package no.bankid.esign.merchant.b2b.dpop;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.oauth2.sdk.dpop.DPoPProofFactory;
import com.nimbusds.oauth2.sdk.dpop.DefaultDPoPProofFactory;
import com.nimbusds.oauth2.sdk.id.JWTID;
import com.nimbusds.oauth2.sdk.token.AccessToken;

import java.net.URI;
import java.util.Date;

public class Es256DPopGenerator implements DPoPGenerator {

    ECKey ecKey;
    DPoPProofFactory dPoPProofFactory;

    {
        try {
            ecKey = new ECKeyGenerator(Curve.P_256)
                .keyIDFromThumbprint(true)
                .generate();
            dPoPProofFactory = new DefaultDPoPProofFactory(ecKey, JWSAlgorithm.ES256);
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String generate(String method, URI uri, AccessToken accessToken) {
        try {

            return dPoPProofFactory.createDPoPJWT(
                new JWTID(12),
                method,
                uri,
                new Date(System.currentTimeMillis()), // TODO: add some extra time here if debugging needed, keycloak has lifetime
                // {"error":"invalid_dpop_proof","error_description":"DPoP proof is not active"},
                // see: https://github.com/keycloak/keycloak/blob/35a4a17aa5b942e69f8c23d47f6d61bfca816915/services/src/main/java/org/keycloak/services/util/DPoPUtil.java#L273
                accessToken,
                null

            ).serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }
}
