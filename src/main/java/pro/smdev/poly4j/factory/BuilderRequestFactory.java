package pro.smdev.poly4j.factory;

import pro.smdev.poly4j.model.Authentication;
import pro.smdev.poly4j.model.RequestBuilder;
import pro.smdev.poly4j.utils.AuthenticationUtils;
import pro.smdev.poly4j.utils.BuilderApiUtils;

import java.time.Instant;

public class BuilderRequestFactory {

    /**
     * Create new deposit wallet for signer
     * @param authentication authentication object with configured {@link pro.smdev.poly4j.model.Secrets}
     * @return configured request
     */
    public RequestBuilder createWallet(Authentication authentication) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String body = BuilderApiUtils.walletCreate(authentication.getSignerAddress());
        return RequestBuilder.relayerApi()
                .post(body)
                .url("/submit")
                .addHeader("Content-Type", "application/json")
                .addHeader("POLY_BUILDER_API_KEY", authentication.getBuilderSecrets().key())
                .addHeader("POLY_BUILDER_TIMESTAMP", timestamp)
                .addHeader("POLY_BUILDER_PASSPHRASE", authentication.getBuilderSecrets().passphrase())
                .addHeader("POLY_BUILDER_SIGNATURE", AuthenticationUtils.encodeL2Signature(authentication.getBuilderSecrets().secret(), timestamp,
                        "POST", "/submit", body));
    }

}
