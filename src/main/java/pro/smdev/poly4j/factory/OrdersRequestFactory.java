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
import pro.smdev.poly4j.model.PolymarketAuthentication;
import pro.smdev.poly4j.model.RequestBuilder;
import pro.smdev.poly4j.utils.AuthenticationUtils;

import java.io.IOException;
import java.time.Instant;

/**
 * Factory for all supported requests to orders block on polymarket
 *
 * @author ALazyGuy
 * @since 1.0.0
 * @see RequestBuilder
 */
public class OrdersRequestFactory {

    /**
     * Create request for open orders by specific account using Authentication
     * <br>
     * API:
     * <a href="https://docs.polymarket.com/api-reference/trade/get-user-orders">https://clob.polymarket.com/data/orders</a>
     * @param authentication {@link PolymarketAuthentication} object
     * @return Configured {@link RequestBuilder}
     *
     */
    public RequestBuilder getOpenOrders(PolymarketAuthentication authentication) throws IOException {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        return RequestBuilder.clobApi()
                .get()
                .url("/data/orders")
                .addHeader("POLY_API_KEY", authentication.apiKey())
                .addHeader("POLY_ADDRESS", Keys.toChecksumAddress(authentication.wallet().address()))
                .addHeader("POLY_SIGNATURE", AuthenticationUtils.encodeL2Signature(authentication.secret(), timestamp, "GET", "/data/orders", ""))
                .addHeader("POLY_PASSPHRASE", authentication.passphrase())
                .addHeader("POLY_TIMESTAMP", timestamp);
    }

}
