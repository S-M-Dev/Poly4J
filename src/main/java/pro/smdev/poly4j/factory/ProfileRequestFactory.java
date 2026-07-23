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

import pro.smdev.poly4j.model.RequestBuilder;

/**
 * Factory for all supported requests to profile block on polymarket
 *
 * @author ALazyGuy
 * @since 1.0.0
 * @see RequestBuilder
 */
public class ProfileRequestFactory {

    /**
     * Create request for profile data loading by walletId
     * <br>
     * API:
     * <a href="https://docs.polymarket.com/api-reference/profiles/get-public-profile-by-wallet-address">https://gamma-api.polymarket.com/public-profile</a>}
     * @param walletId profile walletId
     * @return Configured {@link RequestBuilder}
     *
     */
    public RequestBuilder getPublicProfile(String walletId) {
        return RequestBuilder.gammaApi()
                .get()
                .url("/public-profile")
                .addParam("address", walletId);
    }

}
