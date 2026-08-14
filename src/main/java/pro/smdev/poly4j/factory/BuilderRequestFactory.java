package pro.smdev.poly4j.factory;

/*
 * Copyright 2026 S-M-Dev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import pro.smdev.poly4j.model.Authentication;
import pro.smdev.poly4j.model.RequestBuilder;
import pro.smdev.poly4j.utils.AuthenticationUtils;
import pro.smdev.poly4j.utils.BuilderApiUtils;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class BuilderRequestFactory extends AuthenticatedGuard {

    public BuilderRequestFactory(AtomicReference<Authentication> authentication) {
        super(authentication);
    }

    /**
     * Create new deposit wallet for signer
     * @return configured request
     */
    public RequestBuilder createWallet() {
        Authentication authentication = validateBuilder();
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
