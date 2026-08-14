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

import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.crypto.StructuredDataEncoder;
import org.web3j.utils.Numeric;
import pro.smdev.poly4j.model.Authentication;
import pro.smdev.poly4j.model.Order;
import pro.smdev.poly4j.model.Side;
import pro.smdev.poly4j.model.SignatureType;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Builds and EIP-712 signs {@link Order orders} for the CLOB Exchange contract.
 * <br>
 * API:
 * <a href="https://docs.polymarket.com/trading/place-orders">https://docs.polymarket.com/trading/place-orders</a>
 *
 * <p>{@link SignatureType#EOA}, {@link SignatureType#POLY_PROXY} and {@link SignatureType#GNOSIS_SAFE} orders
 * are signed directly against the CLOB Exchange {@code Order} struct.</p>
 *
 * <p>{@link SignatureType#DEPOSIT_WALLET} orders are signed by the owning EOA but validated on-chain via the
 * deposit wallet's ERC-1271 {@code isValidSignature}, which requires wrapping the signature in an ERC-7739
 * nested {@code TypedDataSign} structure. Both {@code maker} and {@code signer} are the deposit wallet's own
 * address in this case &mdash; the EOA only produces the underlying signature bytes.</p>
 *
 * @author ALazyGuy
 * @since 2.0.0
 */
public class OrderSigningUtils {

    private static final long CHAIN_ID = 137;
    private static final String CTF_EXCHANGE = "0xE111180000d2663C0091e4f400237545B87B996B";
    private static final String NEG_RISK_CTF_EXCHANGE = "0xe2222d279d744050d28e00520010520000310F59";
    private static final String CTF_EXCHANGE_DOMAIN_NAME = "Polymarket CTF Exchange";
    private static final String CTF_EXCHANGE_DOMAIN_VERSION = "2";
    private static final String ZERO_BYTES32 = "0x" + "0".repeat(64);

    private static final long MAX_SAFE_INTEGER = 9_007_199_254_740_991L;

    private static final String ORDER_TYPE_STRING = "Order(uint256 salt,address maker,address signer,"
            + "uint256 tokenId,uint256 makerAmount,uint256 takerAmount,uint8 side,uint8 signatureType,"
            + "uint256 timestamp,bytes32 metadata,bytes32 builder)";

    private static final byte[] DOMAIN_TYPE_HASH = Hash.sha3(
            "EIP712Domain(string name,string version,uint256 chainId,address verifyingContract)"
                    .getBytes(StandardCharsets.UTF_8));
    private static final byte[] ORDER_TYPE_HASH = Hash.sha3(ORDER_TYPE_STRING.getBytes(StandardCharsets.UTF_8));

    private static final String ORDER_EIP712_JSON_PATTERN = "{"
            + "\"types\": {"
            + "  \"EIP712Domain\": ["
            + "    {\"name\": \"name\", \"type\": \"string\"},"
            + "    {\"name\": \"version\", \"type\": \"string\"},"
            + "    {\"name\": \"chainId\", \"type\": \"uint256\"},"
            + "    {\"name\": \"verifyingContract\", \"type\": \"address\"}"
            + "  ],"
            + "  \"Order\": ["
            + "    {\"name\": \"salt\", \"type\": \"uint256\"},"
            + "    {\"name\": \"maker\", \"type\": \"address\"},"
            + "    {\"name\": \"signer\", \"type\": \"address\"},"
            + "    {\"name\": \"tokenId\", \"type\": \"uint256\"},"
            + "    {\"name\": \"makerAmount\", \"type\": \"uint256\"},"
            + "    {\"name\": \"takerAmount\", \"type\": \"uint256\"},"
            + "    {\"name\": \"side\", \"type\": \"uint8\"},"
            + "    {\"name\": \"signatureType\", \"type\": \"uint8\"},"
            + "    {\"name\": \"timestamp\", \"type\": \"uint256\"},"
            + "    {\"name\": \"metadata\", \"type\": \"bytes32\"},"
            + "    {\"name\": \"builder\", \"type\": \"bytes32\"}"
            + "  ]"
            + "},"
            + "\"primaryType\": \"Order\","
            + "\"domain\": {"
            + "  \"name\": \"" + CTF_EXCHANGE_DOMAIN_NAME + "\","
            + "  \"version\": \"" + CTF_EXCHANGE_DOMAIN_VERSION + "\","
            + "  \"chainId\": " + CHAIN_ID + ","
            + "  \"verifyingContract\": \"%s\""
            + "},"
            + "\"message\": {"
            + "  \"salt\": %d,"
            + "  \"maker\": \"%s\","
            + "  \"signer\": \"%s\","
            + "  \"tokenId\": \"%s\","
            + "  \"makerAmount\": \"%s\","
            + "  \"takerAmount\": \"%s\","
            + "  \"side\": %d,"
            + "  \"signatureType\": %d,"
            + "  \"timestamp\": %d,"
            + "  \"metadata\": \"%s\","
            + "  \"builder\": \"%s\""
            + "}"
            + "}";

    /**
     * The ERC-7739 nested {@code TypedDataSign} wrapper signed by the owning EOA for
     * {@link SignatureType#DEPOSIT_WALLET} orders. The outer domain/digest uses the CLOB Exchange's own domain
     * (matching what the Exchange contract will recompute when calling the deposit wallet's
     * {@code isValidSignature}), while {@code message.contents} carries the actual {@code Order} struct and
     * {@code message.verifyingContract} identifies the deposit wallet being signed for.
     */
    private static final String TYPED_DATA_SIGN_JSON_PATTERN = "{"
            + "\"types\": {"
            + "  \"EIP712Domain\": ["
            + "    {\"name\": \"name\", \"type\": \"string\"},"
            + "    {\"name\": \"version\", \"type\": \"string\"},"
            + "    {\"name\": \"chainId\", \"type\": \"uint256\"},"
            + "    {\"name\": \"verifyingContract\", \"type\": \"address\"}"
            + "  ],"
            + "  \"TypedDataSign\": ["
            + "    {\"name\": \"contents\", \"type\": \"Order\"},"
            + "    {\"name\": \"name\", \"type\": \"string\"},"
            + "    {\"name\": \"version\", \"type\": \"string\"},"
            + "    {\"name\": \"chainId\", \"type\": \"uint256\"},"
            + "    {\"name\": \"verifyingContract\", \"type\": \"address\"},"
            + "    {\"name\": \"salt\", \"type\": \"bytes32\"}"
            + "  ],"
            + "  \"Order\": ["
            + "    {\"name\": \"salt\", \"type\": \"uint256\"},"
            + "    {\"name\": \"maker\", \"type\": \"address\"},"
            + "    {\"name\": \"signer\", \"type\": \"address\"},"
            + "    {\"name\": \"tokenId\", \"type\": \"uint256\"},"
            + "    {\"name\": \"makerAmount\", \"type\": \"uint256\"},"
            + "    {\"name\": \"takerAmount\", \"type\": \"uint256\"},"
            + "    {\"name\": \"side\", \"type\": \"uint8\"},"
            + "    {\"name\": \"signatureType\", \"type\": \"uint8\"},"
            + "    {\"name\": \"timestamp\", \"type\": \"uint256\"},"
            + "    {\"name\": \"metadata\", \"type\": \"bytes32\"},"
            + "    {\"name\": \"builder\", \"type\": \"bytes32\"}"
            + "  ]"
            + "},"
            + "\"primaryType\": \"TypedDataSign\","
            + "\"domain\": {"
            + "  \"name\": \"" + CTF_EXCHANGE_DOMAIN_NAME + "\","
            + "  \"version\": \"" + CTF_EXCHANGE_DOMAIN_VERSION + "\","
            + "  \"chainId\": " + CHAIN_ID + ","
            + "  \"verifyingContract\": \"%s\""
            + "},"
            + "\"message\": {"
            + "  \"contents\": {"
            + "    \"salt\": %d,"
            + "    \"maker\": \"%s\","
            + "    \"signer\": \"%s\","
            + "    \"tokenId\": \"%s\","
            + "    \"makerAmount\": \"%s\","
            + "    \"takerAmount\": \"%s\","
            + "    \"side\": %d,"
            + "    \"signatureType\": %d,"
            + "    \"timestamp\": %d,"
            + "    \"metadata\": \"%s\","
            + "    \"builder\": \"%s\""
            + "  },"
            + "  \"name\": \"DepositWallet\","
            + "  \"version\": \"1\","
            + "  \"chainId\": " + CHAIN_ID + ","
            + "  \"verifyingContract\": \"%s\","
            + "  \"salt\": \"" + ZERO_BYTES32 + "\""
            + "}"
            + "}";

    /**
     * Build and sign a new {@link Order}. Fills in a fresh random {@code salt} and the current
     * {@code timestamp}, then EIP-712 signs the order struct with {@code authentication}'s signer key.
     *
     * @param authentication {@link Authentication} object holding the signer's private key
     * @param tokenId CLOB token id (asset id) being traded
     * @param makerAmount amount the maker provides, fixed-point with 6 decimals
     * @param takerAmount amount the taker provides, fixed-point with 6 decimals
     * @param side {@link Side#BUY} or {@link Side#SELL}
     * @param expiration Unix timestamp (seconds) after which the order expires, or {@code 0} for no expiration
     * @param signatureType {@link SignatureType} identifying the signing wallet
     * @param negRisk whether the market is a negative-risk market, selecting the verifying exchange contract
     * @return the fully signed {@link Order}
     */
    public static Order createAndSignOrder(Authentication authentication, String tokenId,
            String makerAmount, String takerAmount, Side side, long expiration, SignatureType signatureType,
            boolean negRisk) {
        String maker = authentication.getFundAddress();
        long salt = ThreadLocalRandom.current().nextLong(1, MAX_SAFE_INTEGER);
        long timestamp = Instant.now().toEpochMilli();
        String verifyingContract = negRisk ? NEG_RISK_CTF_EXCHANGE : CTF_EXCHANGE;

        String signer;
        String signature;
        if (signatureType == SignatureType.DEPOSIT_WALLET) {
            signer = maker;
            signature = signDepositWalletOrder(authentication, verifyingContract, salt, maker, tokenId, makerAmount,
                    takerAmount, side, timestamp);
        } else {
            signer = authentication.getSignerAddress();
            signature = signDirectOrder(authentication, verifyingContract, salt, maker, signer, tokenId, makerAmount,
                    takerAmount, side, timestamp, signatureType);
        }

        return new Order(maker, signer, tokenId, makerAmount, takerAmount, side, expiration, timestamp,
                ZERO_BYTES32, ZERO_BYTES32, salt, signatureType, signature);
    }

    private static String signDirectOrder(Authentication authentication, String verifyingContract, long salt,
            String maker, String signer, String tokenId, String makerAmount, String takerAmount, Side side,
            long timestamp, SignatureType signatureType) {
        try {
            ECKeyPair keyPair = signerKeyPair(authentication);

            String message = String.format(ORDER_EIP712_JSON_PATTERN, verifyingContract, salt, maker, signer, tokenId,
                    makerAmount, takerAmount, side.getValue(), signatureType.getValue(), timestamp, ZERO_BYTES32,
                    ZERO_BYTES32);
            StructuredDataEncoder encoder = new StructuredDataEncoder(message);
            byte[] messageHash = encoder.hashStructuredData();
            return Numeric.toHexString(ecdsaSign(keyPair, messageHash));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to generate order signature: " + e.getMessage(), e);
        }
    }

    /**
     * Signs an order for a {@link SignatureType#DEPOSIT_WALLET} maker/signer, wrapping the signature in the
     * ERC-7739 nested {@code TypedDataSign} structure expected by the deposit wallet's ERC-1271 handler.
     * <br>
     * Mirrors {@code ExchangeOrderBuilderV2.buildOrderSignature} in Polymarket's official
     * {@code clob-client-v2} TypeScript SDK
     * (<a href="https://github.com/Polymarket/clob-client-v2/blob/main/src/order-utils/exchangeOrderBuilderV2.ts">source</a>).
     */
    private static String signDepositWalletOrder(Authentication authentication, String verifyingContract, long salt,
            String wallet, String tokenId, String makerAmount, String takerAmount, Side side, long timestamp) {
        try {
            byte[] appDomainSep = domainSeparator(CTF_EXCHANGE_DOMAIN_NAME, CTF_EXCHANGE_DOMAIN_VERSION, verifyingContract);
            byte[] contentsHash = orderStructHash(salt, wallet, wallet, tokenId, makerAmount, takerAmount, side,
                    SignatureType.DEPOSIT_WALLET, timestamp);

            ECKeyPair keyPair = signerKeyPair(authentication);

            String message = String.format(TYPED_DATA_SIGN_JSON_PATTERN, verifyingContract, salt, wallet, wallet,
                    tokenId, makerAmount, takerAmount, side.getValue(), SignatureType.DEPOSIT_WALLET.getValue(),
                    timestamp, ZERO_BYTES32, ZERO_BYTES32, wallet);
            StructuredDataEncoder encoder = new StructuredDataEncoder(message);
            byte[] innerDigest = encoder.hashStructuredData();
            byte[] innerSignature = ecdsaSign(keyPair, innerDigest);

            byte[] contentsType = ORDER_TYPE_STRING.getBytes(StandardCharsets.US_ASCII);
            byte[] contentsTypeLength = { (byte) (contentsType.length >> 8), (byte) contentsType.length };

            return Numeric.toHexString(DepositWalletUtils.concat(innerSignature, appDomainSep, contentsHash,
                    contentsType, contentsTypeLength));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to generate deposit wallet order signature: " + e.getMessage(), e);
        }
    }

    private static ECKeyPair signerKeyPair(Authentication authentication) {
        String cleanedPrivateKey = authentication.getSignerPrivateKey().replace("0x", "").trim();
        return ECKeyPair.create(Numeric.hexStringToByteArray(cleanedPrivateKey));
    }

    private static byte[] ecdsaSign(ECKeyPair keyPair, byte[] digest) {
        Sign.SignatureData signatureData = Sign.signMessage(digest, keyPair, false);
        byte[] signatureBytes = new byte[65];
        System.arraycopy(signatureData.getR(), 0, signatureBytes, 0, 32);
        System.arraycopy(signatureData.getS(), 0, signatureBytes, 32, 32);
        System.arraycopy(signatureData.getV(), 0, signatureBytes, 64, 1);
        return signatureBytes;
    }

    /** EIP-712 {@code domainSeparator} for an {@code EIP712Domain(string,string,uint256,address)} domain. */
    private static byte[] domainSeparator(String name, String version, String verifyingContract) {
        byte[] nameHash = Hash.sha3(name.getBytes(StandardCharsets.UTF_8));
        byte[] versionHash = Hash.sha3(version.getBytes(StandardCharsets.UTF_8));
        byte[] chainIdWord = Numeric.toBytesPadded(BigInteger.valueOf(CHAIN_ID), 32);
        byte[] contractWord = DepositWalletUtils.addressWord(verifyingContract);
        return Hash.sha3(DepositWalletUtils.concat(DOMAIN_TYPE_HASH, nameHash, versionHash, chainIdWord, contractWord));
    }

    /** EIP-712 {@code hashStruct(Order, ...)}, i.e. the CLOB Exchange order hash. */
    private static byte[] orderStructHash(long salt, String maker, String signer, String tokenId,
            String makerAmount, String takerAmount, Side side, SignatureType signatureType, long timestamp) {
        return Hash.sha3(DepositWalletUtils.concat(
                ORDER_TYPE_HASH,
                Numeric.toBytesPadded(BigInteger.valueOf(salt), 32),
                DepositWalletUtils.addressWord(maker),
                DepositWalletUtils.addressWord(signer),
                Numeric.toBytesPadded(new BigInteger(tokenId), 32),
                Numeric.toBytesPadded(new BigInteger(makerAmount), 32),
                Numeric.toBytesPadded(new BigInteger(takerAmount), 32),
                Numeric.toBytesPadded(BigInteger.valueOf(side.getValue()), 32),
                Numeric.toBytesPadded(BigInteger.valueOf(signatureType.getValue()), 32),
                Numeric.toBytesPadded(BigInteger.valueOf(timestamp), 32),
                Numeric.hexStringToByteArray(ZERO_BYTES32),
                Numeric.hexStringToByteArray(ZERO_BYTES32)
        ));
    }

}
