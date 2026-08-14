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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Factory for all supported requests to authentication block on polymarket
 *
 * @author ALazyGuy
 * @since 1.0.0
 * @see RequestBuilder
 */
public class AuthenticationRequestFactory extends AuthenticatedGuard {

    public AuthenticationRequestFactory(AtomicReference<Authentication> authentication) {
        super(authentication);
    }

    /**
     * Create request to create API key for profile
     * <br>
     * API:
     * <a href="https://docs.polymarket.com/getting-started/api#authentication">https://clob.polymarket.com/auth/api-key</a>
     * @param nonce Nonce for newly created api key
     * @return Configured {@link RequestBuilder}
     *
     */
    public RequestBuilder createApiKey(String nonce) {
        return RequestBuilder.clobApi()
                .post(null)
                .url("/auth/api-key")
                .authenticatedL1(validateL1(), nonce);
    }

    /**
     * Create request to derive existing api key
     * <br>
     * API:
     * <a href="https://docs.polymarket.com/getting-started/api#authentication">https://clob.polymarket.com/auth/derive-api-key</a>
     * @param nonce Nonce for existing api key
     * @return Configured {@link RequestBuilder}
     *
     */
    public RequestBuilder deriveApiKey(String nonce) {
        return RequestBuilder.clobApi()
                .get()
                .url("/auth/derive-api-key")
                .authenticatedL1(validateL1(), nonce);
    }

}

