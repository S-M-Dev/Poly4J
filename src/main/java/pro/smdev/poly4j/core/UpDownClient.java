package pro.smdev.poly4j.core;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.smdev.poly4j.factory.AuthenticatedGuard;
import pro.smdev.poly4j.mapper.ResponseMapper;
import pro.smdev.poly4j.model.Order;
import pro.smdev.poly4j.model.OrderType;
import pro.smdev.poly4j.model.Side;
import pro.smdev.poly4j.model.SignatureType;
import pro.smdev.poly4j.model.dto.OrderResult;
import pro.smdev.poly4j.model.dto.UpDownMarket;
import pro.smdev.poly4j.utils.MarketPriceUtils;
import pro.smdev.poly4j.utils.OrderSigningUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Convenience client for trading Polymarket "Up/Down" markets (e.g. {@code btc-updown-15m-*}) with
 * {@link SignatureType#DEPOSIT_WALLET}-signed FOK market orders.
 *
 * <p>Obtained via {@link PolyClient#getUpDownClientAsync}, which also derives L2 (CLOB API key) credentials
 * on first use.</p>
 *
 * @author ALazyGuy
 * @since 2.1.1
 */
public class UpDownClient extends AuthenticatedGuard {

    private static final Logger log = LoggerFactory.getLogger(UpDownClient.class);

    private final PolyClient client;

    /**
     * Wraps {@code client}, sharing its {@link PolyClient#getAuthenticationAtomic() authentication}.
     * Prefer obtaining an instance via {@link PolyClient#getUpDownClient} rather than calling this directly.
     *
     * @param client the {@link PolyClient} to trade through
     */
    public UpDownClient(PolyClient client) {
        super(client.getAuthenticationAtomic());
        this.client = client;
    }

    /**
     * Derives L2 credentials for {@code client}'s current authentication if not already present.
     *
     * @param nonce nonce to derive/create the API key with, see {@link PolyClient#deriveL2CredentialsAsync}
     * @return a future completing with this {@link UpDownClient} once ready to trade
     */
    public CompletableFuture<UpDownClient> initializeAsync(String nonce) {
        return CompletableFuture.supplyAsync(() -> initialize(nonce));
    }

    /**
     * Derives L2 credentials for {@code client}'s current authentication if not already present.
     *
     * @param nonce nonce to derive/create the API key with, see {@link PolyClient#deriveL2Credentials}
     * @return this {@link UpDownClient} once ready to trade
     */
    public UpDownClient initialize(String nonce) {
        if (!authenticated()) {
            client.deriveL2Credentials(nonce);
        }
        return this;
    }

    /**
     * Resolves an Up/Down market's condition id and outcome token ids by its slug.
     *
     * @param slug market slug, e.g. {@code "btc-updown-15m-<epochSeconds>"}
     * @return a future completing with the resolved {@link UpDownMarket}, or {@code null} if the market
     * data does not have exactly two outcomes/token ids
     */
    public CompletableFuture<UpDownMarket> getMarketAsync(String slug) {
        return CompletableFuture.supplyAsync(() -> getMarket(slug));
    }

    /**
     * Resolves an Up/Down market's condition id and outcome token ids by its slug.
     *
     * @param slug market slug, e.g. {@code "btc-updown-15m-<epochSeconds>"}
     * @return the resolved {@link UpDownMarket}, or {@code null} if the market data does not have
     * exactly two outcomes/token ids
     */
    public UpDownMarket getMarket(String slug) {
        log.debug("Loading market '{}'", slug);
        JsonNode node = client.perform(client.request().markets().bySlug(slug), ResponseMapper.jsonMapper());
        String[] outcomes = splitJsonStringArray(node.at("/outcomes").textValue());
        String[] clobTokens = splitJsonStringArray(node.at("/clobTokenIds").textValue());
        if (outcomes.length == clobTokens.length && outcomes.length == 2) {
            String up = null, down = null;

            for (int d = 0; d < 2; d++) {
                if (outcomes[d].equalsIgnoreCase("up")) {
                    up = clobTokens[d];
                } else if (outcomes[d].equalsIgnoreCase("down")) {
                    down = clobTokens[d];
                }
            }

            UpDownMarket upDownMarket = new UpDownMarket(node.at("/conditionId").textValue(), up, down);
            log.debug("Loaded market by slug '{}' -> {}", slug, upDownMarket);
            return upDownMarket;
        }
        log.debug("No market found by slug '{}'", slug);
        return null;
    }

    /**
     * Max attempts for a FOK market order before giving up and reporting a zero fill.
     */
    private static final int MAX_FOK_ATTEMPTS = 3;

    /**
     * Places a {@link SignatureType#DEPOSIT_WALLET}-signed FOK market order, sized against the current
     * order book depth, retrying up to {@link #MAX_FOK_ATTEMPTS} times if it gets killed.
     *
     * @param side    {@link Side#BUY} or {@link Side#SELL}
     * @param assetId CLOB token id (asset id) being traded
     * @param cash    USD notional to spend for a BUY, or number of shares to sell for a SELL
     * @return a future completing with an {@link OrderResult} describing the shares filled (BUY) or
     * USD received (SELL), or a failure message if every attempt was killed
     */
    public CompletableFuture<OrderResult> placeOrderAsync(Side side, String assetId, double cash) {
        return placeOrderAsync(side, assetId, cash, MAX_FOK_ATTEMPTS);
    }

    /**
     * Attempts a FOK market order against a freshly fetched book, retrying with a new snapshot if the
     * previous attempt was killed for insufficient depth (a stale-book race, not a real failure) &mdash;
     * see {@link pro.smdev.poly4j.utils.MarketPriceUtils#calculateMarketPrice}.
     *
     * @param side         {@link Side#BUY} or {@link Side#SELL}
     * @param assetId      CLOB token id (asset id) being traded
     * @param cash         USD notional to spend for a BUY, or number of shares to sell for a SELL
     * @param attemptsLeft remaining attempts, including this one
     * @return a future completing with an {@link OrderResult} describing the filled amount, or a failure
     * message once {@code attemptsLeft} is exhausted
     */
    public CompletableFuture<OrderResult> placeOrderAsync(Side side, String assetId, double cash, int attemptsLeft) {
        return CompletableFuture.supplyAsync(() -> placerOder(side, assetId, cash, attemptsLeft));
    }

    /**
     * Places a {@link SignatureType#DEPOSIT_WALLET}-signed FOK market order, sized against the current
     * order book depth, retrying up to {@link #MAX_FOK_ATTEMPTS} times if it gets killed.
     *
     * @param side    {@link Side#BUY} or {@link Side#SELL}
     * @param assetId CLOB token id (asset id) being traded
     * @param cash    USD notional to spend for a BUY, or number of shares to sell for a SELL
     * @return an {@link OrderResult} describing the shares filled (BUY) or USD received (SELL), or a
     * failure message if every attempt was killed
     */
    public OrderResult placeOrder(Side side, String assetId, double cash) {
        return placerOder(side, assetId, cash, MAX_FOK_ATTEMPTS);
    }

    /**
     * Attempts a FOK market order against a freshly fetched book, retrying with a new snapshot if the
     * previous attempt was killed for insufficient depth (a stale-book race, not a real failure) &mdash;
     * see {@link pro.smdev.poly4j.utils.MarketPriceUtils#calculateMarketPrice}.
     *
     * @param side         {@link Side#BUY} or {@link Side#SELL}
     * @param assetId      CLOB token id (asset id) being traded
     * @param cash         USD notional to spend for a BUY, or number of shares to sell for a SELL
     * @param attemptsLeft remaining attempts, including this one
     * @return an {@link OrderResult} describing the filled amount, or a failure message once
     * {@code attemptsLeft} is exhausted
     */
    public OrderResult placerOder(Side side, String assetId, double cash, int attemptsLeft) {
        log.debug("Trying to place order {} on {} for {} {}. Attempt: {}", side.name(), assetId, cash, side == Side.BUY ? "$" : " shares", attemptsLeft);
        JsonNode bookNode = client.perform(client.request().orderbook().getOrderBook(assetId), ResponseMapper.jsonMapper());
        double marketPrice = MarketPriceUtils.calculateMarketPrice(bookNode, side, cash);
        int takerDecimals = MarketPriceUtils.takerAmountDecimals(bookNode.get("tick_size").asText());

        double roundedDollarAmount = MarketPriceUtils.roundDown(cash, MarketPriceUtils.MAKER_AMOUNT_DECIMALS);
        double roundedShares = side == Side.BUY ?
                MarketPriceUtils.roundDown(roundedDollarAmount / marketPrice, takerDecimals)
                :
                MarketPriceUtils.roundDown(roundedDollarAmount * marketPrice, takerDecimals);
        String makerAmount = String.valueOf(Math.round(roundedDollarAmount * 1_000_000));
        String takerAmount = String.valueOf(Math.round(roundedShares * 1_000_000));

        Order order = OrderSigningUtils.createAndSignOrder(
                client.getAuthentication(), assetId, makerAmount, takerAmount,
                side, 0, SignatureType.DEPOSIT_WALLET, false);

        log.debug("Order to be placed: {}", order);
        JsonNode postOrderNode = client.perform(client.request().orders().postOrder(order, OrderType.FOK, false, false),
                ResponseMapper.jsonMapper());

        log.trace("Order placement response: {}", postOrderNode.toString());
        if (postOrderNode.has("error")) {
            String error = postOrderNode.at("/error").asText();
            if (!error.isBlank()) {
                return new OrderResult(0.0, error);
            }
        }
        if (postOrderNode.has("takingAmount") && !postOrderNode.at("/takingAmount").isMissingNode()) {
            return new OrderResult(
                    Double.parseDouble(postOrderNode.at("/takingAmount").asText()),
                    "Success"
            );
        }
        if (attemptsLeft > 1) {
            return placerOder(side, assetId, cash, attemptsLeft - 1);
        }
        return new OrderResult(0.0, "Unhandled error");
    }

    /**
     * Polls the positions API until a {@link #placeOrderAsync BUY} order's fill has settled and been indexed,
     * without a delay between poll attempts.
     *
     * @param market         market the position belongs to, see {@link #getMarketAsync}
     * @param outcome        outcome name to match against, e.g. {@code "up"} or {@code "down"}
     * @param sharesAcquired number of shares expected to appear in the position, as returned by
     *                       {@link OrderResult#amount()} from {@link #placeOrderAsync}
     * @param waitFor        milliseconds to keep polling before giving up
     * @return a future completing with the confirmed position size, or {@code 0.0} if it did not settle in time
     */
    public CompletableFuture<Double> awaitOpenPositionAsync(UpDownMarket market, String outcome, double sharesAcquired, long waitFor) {
        return awaitOpenPositionAsync(market, outcome, sharesAcquired, waitFor, 0);
    }

    /**
     * Polls the positions API until a {@link #placeOrderAsync BUY} order's fill has settled and been indexed,
     * since a matched order's on-chain settlement lags behind the "matched" response.
     *
     * @param market          market the position belongs to, see {@link #getMarketAsync}
     * @param outcome         outcome name to match against, e.g. {@code "up"} or {@code "down"}
     * @param sharesAcquired  number of shares expected to appear in the position, as returned by
     *                        {@link OrderResult#amount()} from {@link #placeOrderAsync}
     * @param waitFor         milliseconds to keep polling before giving up
     * @param waitInterval    milliseconds to wait between poll attempts (0 for no delay)
     * @return a future completing with the confirmed position size, or {@code 0.0} if it did not settle in time
     */
    public CompletableFuture<Double> awaitOpenPositionAsync(UpDownMarket market, String outcome, double sharesAcquired, long waitFor, long waitInterval) {
        return CompletableFuture.supplyAsync(() -> awaitOpenPosition(market, outcome, sharesAcquired, waitFor, waitInterval));
    }

    /**
     * Polls the positions API until a {@link #placeOrderAsync BUY} order's fill has settled and been indexed,
     * without a delay between poll attempts.
     *
     * @param market         market the position belongs to, see {@link #getMarketAsync}
     * @param outcome        outcome name to match against, e.g. {@code "up"} or {@code "down"}
     * @param sharesAcquired number of shares expected to appear in the position, as returned by
     *                       {@link OrderResult#amount()} from {@link #placeOrderAsync}
     * @param waitFor        milliseconds to keep polling before giving up
     * @return the confirmed position size, or {@code 0.0} if it did not settle in time
     */
    public double awaitOpenPosition(UpDownMarket market, String outcome, double sharesAcquired, long waitFor) {
        return awaitOpenPosition(market, outcome, sharesAcquired, waitFor, 0);
    }

    /**
     * Polls the positions API until a {@link #placeOrderAsync BUY} order's fill has settled and been indexed,
     * since a matched order's on-chain settlement lags behind the "matched" response.
     *
     * @param market          market the position belongs to, see {@link #getMarketAsync}
     * @param outcome         outcome name to match against, e.g. {@code "up"} or {@code "down"}
     * @param sharesAcquired  number of shares expected to appear in the position, as returned by
     *                        {@link OrderResult#amount()} from {@link #placeOrderAsync}
     * @param waitFor         milliseconds to keep polling before giving up
     * @param waitInterval    milliseconds to wait between poll attempts (0 for no delay)
     * @return the confirmed position size, or {@code 0.0} if it did not settle in time, or if interrupted
     * while waiting between attempts
     */
    public double awaitOpenPosition(UpDownMarket market, String outcome, double sharesAcquired, long waitFor, long waitInterval) {
        double confirmedShares = 0;
        long end = System.currentTimeMillis() + waitFor;
        log.debug("Waiting for {} millis. Recheck interval is {}", waitFor, waitInterval);
        try {
            while (confirmedShares < sharesAcquired - 0.001 && System.currentTimeMillis() < end) {
                if (waitInterval != 0) {
                    TimeUnit.MILLISECONDS.sleep(waitInterval);
                }
                JsonNode positionsNode = client.perform(client.request()
                                .positions()
                                .getPositions(client.getAuthentication().getFundAddress(), market.conditionId()), ResponseMapper.jsonMapper());
                for (JsonNode position : positionsNode) {
                    if (position.at("/outcome").asText().equalsIgnoreCase(outcome)) {
                        confirmedShares = position.at("/size").asDouble();
                        break;
                    }
                }
                log.debug("Waiting for {} millis more", end - System.currentTimeMillis());
            }
        } catch (InterruptedException e) {
            return 0;
        }
        return confirmedShares;
    }

    /**
     * @return {@code true} if L2 (CLOB API key) credentials are already present, i.e. {@link #initialize}
     * does not need to derive them again
     */
    private boolean authenticated() {
        return client.getAuthentication() != null && client.getAuthentication().getClobSecrets() != null;
    }

    /**
     * Parses a Gamma API string-encoded JSON array field (e.g. {@code "[\"Up\", \"Down\"]"}) into a
     * plain string array.
     *
     * @param data raw string value of the field
     * @return the parsed array elements
     */
    private String[] splitJsonStringArray(String data) {
        String[] result = data.substring(2, data.length() - 2).split("\", \"");
        for (int i = 0; i < result.length; i++) {
            result[i] = result[i].replaceAll("\\[\"|\"]", "");
        }
        return result;
    }

}
