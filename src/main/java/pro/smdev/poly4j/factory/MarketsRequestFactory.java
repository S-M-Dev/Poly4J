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
 * Factory for all supported requests to markets block on polymarket
 *
 * @author ALazyGuy
 * @since 1.0.0
 * @see RequestBuilder
 */
public class MarketsRequestFactory {

    /**
     * Create request for market data loading by market slug
     * <br>
     * API:
     * <a href="https://docs.polymarket.com/api-reference/markets/get-market-by-slug">https://gamma-api.polymarket.com/markets/slug/{slug}</a>}
     * @param slug market slug
     * @return Configured {@link RequestBuilder}
     *
     */
    public RequestBuilder bySlug(String slug) {
        return RequestBuilder.gammaApi()
                .get()
                .url("/markets/slug/" + slug);
    }

}
