package pro.smdev.poly4j.utils;

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

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import pro.smdev.poly4j.model.Order;
import pro.smdev.poly4j.model.OrderType;

import java.io.IOException;

/**
 * Serializes a signed {@link Order} into the {@code SendOrder} JSON body expected by {@code POST /order}.
 *
 * @author ALazyGuy
 * @since 2.0.0
 */
public class OrderJsonUtils {

    private static final JsonMapper MAPPER = new JsonMapper();

    /**
     * Serializes a signed {@link Order} into the {@code SendOrder} JSON body expected by {@code POST /order}.
     *
     * @param order signed {@link Order} to submit
     * @param owner CLOB API key of the account submitting the order
     * @param orderType time in force for the order
     * @param deferExec whether to defer execution
     * @param postOnly whether the order must rest on the book and not match immediately (GTC/GTD only)
     * @return the {@code SendOrder} request body
     * @throws IOException if serialization fails
     */
    public static String toSendOrderBody(Order order, String owner, OrderType orderType, boolean deferExec,
            boolean postOnly) throws IOException {
        ObjectNode orderNode = MAPPER.createObjectNode();
        orderNode.put("maker", order.maker());
        orderNode.put("signer", order.signer());
        orderNode.put("tokenId", order.tokenId());
        orderNode.put("makerAmount", order.makerAmount());
        orderNode.put("takerAmount", order.takerAmount());
        orderNode.put("side", order.side().name());
        orderNode.put("expiration", String.valueOf(order.expiration()));
        orderNode.put("timestamp", String.valueOf(order.timestamp()));
        orderNode.put("metadata", order.metadata());
        orderNode.put("builder", order.builder());
        orderNode.put("signature", order.signature());
        orderNode.put("salt", order.salt());
        orderNode.put("signatureType", order.signatureType().getValue());

        ObjectNode root = MAPPER.createObjectNode();
        root.set("order", orderNode);
        root.put("owner", owner);
        root.put("orderType", orderType.name());
        root.put("deferExec", deferExec);
        root.put("postOnly", postOnly);

        return MAPPER.writeValueAsString(root);
    }

    /** Serializes the {@code CancelOrder} JSON body expected by {@code DELETE /order}. */
    public static String toCancelOrderBody(String orderId) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("orderID", orderId);
        return MAPPER.writeValueAsString(root);
    }

}
