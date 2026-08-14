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
import pro.smdev.poly4j.model.Order;
import pro.smdev.poly4j.model.OrderType;
import pro.smdev.poly4j.model.RequestBuilder;
import pro.smdev.poly4j.utils.OrderJsonUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Factory for all supported requests to orders block on polymarket
 *
 * @author ALazyGuy
 * @since 1.0.0
 * @see RequestBuilder
 */
public class OrdersRequestFactory extends AuthenticatedGuard {

    public OrdersRequestFactory(AtomicReference<Authentication> authentication) {
        super(authentication);
    }

    /**
     * Create request for open orders by specific account using Authentication
     * <br>
     * API:
     * <a href="https://docs.polymarket.com/api-reference/trade/get-user-orders">https://clob.polymarket.com/data/orders</a>
     * @return Configured {@link RequestBuilder}
     *
     */
    public RequestBuilder getOpenOrders() {
        return RequestBuilder.clobApi()
                .get()
                .url("/data/orders")
                .authenticatedL2(validateL2());
    }

    /**
     * Create request to submit a signed order.
     * <br>
     * API:
     * <a href="https://docs.polymarket.com/api-reference/trade/post-a-new-order">https://clob.polymarket.com/order</a>
     * @param order signed {@link Order}, see {@link pro.smdev.poly4j.utils.OrderSigningUtils#createAndSignOrder}
     * @param orderType time in force for the order
     * @param deferExec whether to defer execution
     * @param postOnly whether the order must rest on the book and not match immediately (GTC/GTD only)
     * @return Configured {@link RequestBuilder}
     *
     */
    public RequestBuilder postOrder(Order order, OrderType orderType,
            boolean deferExec, boolean postOnly) {
        Authentication authentication = validateL2();
        String body = OrderJsonUtils.toSendOrderBody(order, authentication.getClobSecrets().key(), orderType,
                deferExec, postOnly);
        return RequestBuilder.clobApi()
                .post(body)
                .url("/order")
                .addHeader("Content-Type", "application/json")
                .authenticatedL2(authentication);
    }

    /**
     * Create request to cancel a single order.
     * <br>
     * API:
     * <a href="https://docs.polymarket.com/api-reference/trade/cancel-single-order">https://clob.polymarket.com/order</a>
     * @param orderId the order hash to cancel, as returned in {@code orderID} by {@link #postOrder}
     * @return Configured {@link RequestBuilder}
     *
     */
    public RequestBuilder cancelOrder(String orderId) throws IOException {
        String body = OrderJsonUtils.toCancelOrderBody(orderId);
        return RequestBuilder.clobApi()
                .delete(body)
                .url("/order")
                .addHeader("Content-Type", "application/json")
                .authenticatedL2(validateL2());
    }

}
