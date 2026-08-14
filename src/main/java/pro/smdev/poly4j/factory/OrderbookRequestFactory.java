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
 * Factory for all supported requests to the orderbook block on polymarket
 *
 * @author ALazyGuy
 * @since 2.0.0
 * @see RequestBuilder
 */
public class OrderbookRequestFactory {

    /**
     * Create request for the best price to fully fill a market order on one side of a token.
     * <br>
     * API:
     * <a href="https://docs.polymarket.com/api-reference/market-data/get-market-price">https://clob.polymarket.com/price</a>
     * @param tokenId CLOB token id (asset id)
     * @param side {@code BUY} or {@code SELL}
     * @return Configured {@link RequestBuilder}
     */
    public RequestBuilder getPrice(String tokenId, String side) {
        return RequestBuilder.clobApi()
                .get()
                .addParam("token_id", tokenId)
                .addParam("side", side)
                .url("/price");
    }

    /**
     * Create request for the full order book (bids and asks) of a token.
     * <br>
     * API:
     * <a href="https://docs.polymarket.com/api-reference/market-data/get-order-book">https://clob.polymarket.com/book</a>
     * @param tokenId CLOB token id (asset id)
     * @return Configured {@link RequestBuilder}
     */
    public RequestBuilder getOrderBook(String tokenId) {
        return RequestBuilder.clobApi()
                .get()
                .addParam("token_id", tokenId)
                .url("/book");
    }

}
