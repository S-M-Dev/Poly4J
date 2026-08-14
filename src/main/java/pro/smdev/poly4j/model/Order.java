package pro.smdev.poly4j.model;

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

/**
 * A signed CLOB Exchange order, ready to be submitted via {@code POST /order}.
 *
 * <p>Instances are created by {@link pro.smdev.poly4j.utils.OrderSigningUtils#createAndSignOrder}, which
 * fills in {@code salt}, {@code timestamp} and {@code signature} for you.</p>
 *
 * @param maker Ethereum address providing the funds for the order
 * @param signer Ethereum address that produced {@code signature}
 * @param tokenId CLOB token id (asset id) being traded
 * @param makerAmount Amount the maker provides, fixed-point with 6 decimals
 * @param takerAmount Amount the taker provides, fixed-point with 6 decimals
 * @param side {@link Side#BUY} or {@link Side#SELL}
 * @param expiration Unix timestamp (seconds) after which the order expires, or {@code 0} for no expiration
 * @param timestamp Unix timestamp (milliseconds) the order was created
 * @param metadata Reserved bytes32 field, zero-filled unless otherwise specified
 * @param builder Builder code bytes32 field, zero-filled when no builder is attributing the order
 * @param salt Random value used to make the order hash unique
 * @param signatureType {@link SignatureType} identifying the signing wallet
 * @param signature EIP-712 signature of the order struct
 *
 * @author ALazyGuy
 * @since 2.0.0
 */
public record Order(
        String maker,
        String signer,
        String tokenId,
        String makerAmount,
        String takerAmount,
        Side side,
        long expiration,
        long timestamp,
        String metadata,
        String builder,
        long salt,
        SignatureType signatureType,
        String signature
) {
}
