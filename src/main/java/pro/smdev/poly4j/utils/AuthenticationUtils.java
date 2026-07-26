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

import com.fasterxml.jackson.databind.JsonNode;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.crypto.StructuredDataEncoder;
import org.web3j.utils.Numeric;
import pro.smdev.poly4j.model.PolymarketAuthentication;
import pro.smdev.poly4j.model.Wallet;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility methods for authentication
 */
public class AuthenticationUtils {

    private static final String EIP_712_JSON_MESSAGE_PATTERN = "{"
            + "\"types\": {"
            + "  \"EIP712Domain\": ["
            + "    {\"name\": \"name\", \"type\": \"string\"},"
            + "    {\"name\": \"version\", \"type\": \"string\"},"
            + "    {\"name\": \"chainId\", \"type\": \"uint256\"}"
            + "  ],"
            + "  \"ClobAuth\": ["
            + "    {\"name\": \"address\", \"type\": \"address\"},"
            + "    {\"name\": \"timestamp\", \"type\": \"string\"},"
            + "    {\"name\": \"nonce\", \"type\": \"uint256\"},"
            + "    {\"name\": \"message\", \"type\": \"string\""
            + "  ]"
            + "},"
            + "\"primaryType\": \"ClobAuth\","
            + "\"domain\": {"
            + "  \"name\": \"ClobAuthDomain\","
            + "  \"version\": \"1\","
            + "  \"chainId\": 137"
            + "},"
            + "\"message\": {"
            + "  \"address\": \"%s\","
            + "  \"timestamp\": \"%s\","
            + "  \"nonce\": %s,"
            + "  \"message\": \"%s\""
            + "}"
            + "}";

    /**
     * Create signature for L1 authentication header
     * @param wallet {@link Wallet} object associated with account
     * @param timestamp Current UNIX timestamp in seconds
     * @param nonce Request nonce
     * @return Encoded signature for L1 authentication
     */
    public static String encodeL1Signature(Wallet wallet, String timestamp, String nonce) throws IOException {
        try {
            String cleanedPrivateKey = wallet.privateKeyHex().replace("0x", "").trim();
            String checksummedWallet = Keys.toChecksumAddress(wallet.address().trim());
            ECKeyPair keyPair = ECKeyPair.create(Numeric.hexStringToByteArray(cleanedPrivateKey));
            String derivedAddress = Keys.getAddress(keyPair);
            String checksummedDerived = Keys.toChecksumAddress("0x" + derivedAddress);

            if (!checksummedWallet.equals(checksummedDerived)) {
                throw new IllegalArgumentException("Private key does not match address!");
            }

            String message = String.format(
                    EIP_712_JSON_MESSAGE_PATTERN,
                    checksummedWallet,
                    timestamp,
                    nonce,
                    "This message attests that I control the given wallet"
            );
            StructuredDataEncoder encoder = new StructuredDataEncoder(message);
            byte[] messageHash = encoder.hashStructuredData();
            Sign.SignatureData signatureData = Sign.signMessage(messageHash, keyPair, false);
            byte[] signatureBytes = new byte[65];
            System.arraycopy(signatureData.getR(), 0, signatureBytes, 0, 32);
            System.arraycopy(signatureData.getS(), 0, signatureBytes, 32, 32);
            System.arraycopy(signatureData.getV(), 0, signatureBytes, 64, 1);
            return Numeric.toHexString(signatureBytes);
        } catch (Exception e) {
            throw new IOException("Failed to generate L1 signature: " + e.getMessage(), e);
        }
    }


    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Create signature for L2 authentication header
     * @param secret {@link PolymarketAuthentication#secret()} associated with account api key
     * @param timestamp Current UNIX timestamp in seconds
     * @param method {@link pro.smdev.poly4j.model.RequestMethod}
     * @param path Request path
     * @param body Post body of exists
     * @return Encoded signature for L2 authentication
     */
    public static String encodeL2Signature(String secret, String timestamp, String method, String path, String body) {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalArgumentException("buildPolyHmacSignature: secret is empty");
        }

        try {
            String message = timestamp + method + path;
            if (body != null) {
                message += body;
            }
            byte[] keyBytes = Base64.getUrlDecoder().decode(secret);
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, HMAC_SHA256);
            mac.init(secretKey);
            byte[] sigBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sigBytes)
                    .replace('+', '-')
                    .replace('/', '_');

        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate Polymarket HMAC signature", e);
        }
    }

    /**
     * Create authentication object after L1 authentication
     * @param wallet Wallet associated with account
     * @param authNode Response node from L1 authentication
     * @return Authentication object which contains all information for L2 requests
     */
    public static PolymarketAuthentication buildAuthentication(Wallet wallet, JsonNode authNode) {
        return new PolymarketAuthentication(
                wallet,
                authNode.at("/apiKey").asText(),
                authNode.at("/secret").asText(),
                authNode.at("/passphrase").asText()
        );
    }

}
