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
 * Factory for all supported requests to the positions block on polymarket
 *
 * @author ALazyGuy
 * @since 2.0.0
 * @see RequestBuilder
 */
public class PositionsRequestFactory {

    /**
     * Create request for a user's current positions in a market.
     * <br>
     * API:
     * <a href="https://docs.polymarket.com/api-reference/core/get-current-positions-for-a-user">https://data-api.polymarket.com/positions</a>
     * @param user wallet address holding the positions (e.g. {@link pro.smdev.poly4j.model.Authentication#getFundAddress()})
     * @param conditionId market condition id to filter by
     * @return Configured {@link RequestBuilder}
     */
    public RequestBuilder getPositions(String user, String conditionId) {
        return RequestBuilder.dataApi()
                .get()
                .addParam("user", user)
                .addParam("market", conditionId)
                .addParam("sizeThreshold", "0")
                .url("/positions");
    }

}
