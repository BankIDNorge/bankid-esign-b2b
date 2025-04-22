package no.bankid.esign.merchant.b2b.feignclients;

import static no.bankid.esign.merchant.b2b.PrettyPrint.prettyPrintJWT;

import feign.Client;
import feign.Request;
import feign.Response;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * This class is a Feign client that intercepts the request and response and adds the necessary headers for OAuth2 and DPoP.
 * It also, for now,  logs the response headers in case of a non-200 response.
 */
public class InterceptingFeignClient extends Client.Default {
    public static boolean traceTokens = false;
    final String name;
    public static final ThreadLocal<BasicOrAccessAndDPopTokens> currentTokenContainer = new ThreadLocal<BasicOrAccessAndDPopTokens>();

    public InterceptingFeignClient(String name) {
        super(null, null);
        this.name = name;
    }

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        setAccessAndDPoPProof(request);
        Response response = super.execute(request, options);
        // inspect the response here
        if (response.status() != 200) {
            System.out.println("Response "+name+" : " + response.status());
            Map<String, Collection<String>> headers = response.headers();
            headers.forEach((key, value) -> System.out.println(key + ": " + value));
            System.out.println("End Response " + name +" Dump");
        }
        return response;
    }


    public void setAccessAndDPoPProof(Request request) {
        BasicOrAccessAndDPopTokens tokens = currentTokenContainer.get();
        if (tokens != null) {
            currentTokenContainer.remove();
            if (traceTokens) {
                traceAndPrettyPrintTokens(request.url(), tokens);
            }
            if (tokens.basicAuth != null) {
                request.header("Authorization", "Basic " + tokens.basicAuth());
            }
            if (tokens.dPoPProof() != null) {
                if (tokens.accessToken != null) {
                    request.header("Authorization", "DPoP " + tokens.accessToken());
                }
                request.header("DPoP", tokens.dPoPProof());
            } else {
                if (tokens.accessToken != null) {
                    request.header("Authorization", "Bearer " + tokens.accessToken());
                }
            }
        }
    }

    private void traceAndPrettyPrintTokens(String url, BasicOrAccessAndDPopTokens tokens) {
        System.out.println("Request to " + url);
        System.out.println("Basic Auth:" + tokens.basicAuth());
        System.out.println("Access Token:"+tokens.accessToken()+"\n" + prettyPrintJWT(tokens.accessToken()));
        System.out.println("DPoP Proof:"+tokens.dPoPProof()+"\n" + prettyPrintJWT(tokens.dPoPProof()));
    }

    public static void injectAccessOrBasicAndDPoPProof(String basicAuth,
        String accessToken, String dPopProof) {
        currentTokenContainer.set(new BasicOrAccessAndDPopTokens(basicAuth, accessToken, dPopProof));
    }

    public record BasicOrAccessAndDPopTokens(String basicAuth, String accessToken, String dPoPProof) {
        public BasicOrAccessAndDPopTokens {
            if (accessToken != null && basicAuth != null) {
                throw new IllegalArgumentException("Cannot combine access token and basic auth");
            }
        }
    }


}
