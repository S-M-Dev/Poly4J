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

import org.web3j.crypto.Keys;
import pro.smdev.poly4j.model.Authentication;
import pro.smdev.poly4j.model.RequestBuilder;
import pro.smdev.poly4j.model.Wallet;
import pro.smdev.poly4j.utils.AuthenticationUtils;

import java.io.IOException;
import java.time.Instant;

/**
 * Factory for all supported requests to authentication block on polymarket
 *
 * @author ALazyGuy
 * @since 1.0.0
 * @see RequestBuilder
 */
public class AuthenticationRequestFactory {

    /**
     * Create request to create API key for profile
     * <br>
     * API:
     * <a href="https://docs.polymarket.com/getting-started/api#authentication">https://clob.polymarket.com/auth/api-key</a>
     * @param authentication {@link Authentication} object
     * @param nonce Nonce for newly created api key
     * @return Configured {@link RequestBuilder}
     *
     */
    public RequestBuilder createApiKey(Authentication authentication, String nonce) throws IOException {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        return RequestBuilder.clobApi()
                .post(null)
                .url("/auth/api-key")
                .addHeader("POLY_ADDRESS", Keys.toChecksumAddress(authentication.getSignerAddress()))
                .addHeader("POLY_SIGNATURE", AuthenticationUtils.encodeL1Signature(authentication, timestamp, nonce))
                .addHeader("POLY_TIMESTAMP", timestamp)
                .addHeader("POLY_NONCE", nonce);
    }

    /**
     * Create request to derive existing api key
     * <br>
     * API:
     * <a href="https://docs.polymarket.com/getting-started/api#authentication">https://clob.polymarket.com/auth/derive-api-key</a>
     * @param wallet {@link Wallet} object
     * @param nonce Nonce for existing api key
     * @return Configured {@link RequestBuilder}
     *
     */
    public RequestBuilder deriveApiKey(Authentication authentication, String nonce) throws IOException {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        return RequestBuilder.clobApi()
                .get()
                .url("/auth/derive-api-key")
                .addHeader("POLY_ADDRESS", Keys.toChecksumAddress(authentication.getSignerAddress()))
                .addHeader("POLY_SIGNATURE", AuthenticationUtils.encodeL1Signature(authentication, timestamp, nonce))
                .addHeader("POLY_TIMESTAMP", timestamp)
                .addHeader("POLY_NONCE", nonce);
    }

}

