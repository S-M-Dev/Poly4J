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
import pro.smdev.poly4j.factory.AuthenticatedGuard;
import pro.smdev.poly4j.mapper.ResponseMapper;
import pro.smdev.poly4j.model.Order;
import pro.smdev.poly4j.model.OrderType;
import pro.smdev.poly4j.model.Side;
import pro.smdev.poly4j.model.SignatureType;
import pro.smdev.poly4j.model.dto.UpDownMarket;
import pro.smdev.poly4j.utils.MarketPriceUtils;
import pro.smdev.poly4j.utils.OrderSigningUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Convenience client for trading Polymarket "Up/Down" markets (e.g. {@code btc-updown-15m-*}) with
 * {@link SignatureType#DEPOSIT_WALLET}-signed FOK market orders.
 *
 * <p>Obtained via {@link PolyClient#getUpDownClient}, which also derives L2 (CLOB API key) credentials
 * on first use.</p>
 *
 * @author ALazyGuy
 * @since 2.1.1
 */
public class UpDownClient extends AuthenticatedGuard {

    private final PolyClient client;

    public UpDownClient(PolyClient client) {
        super(client.getAuthenticationAtomic());
        this.client = client;
    }

    /**
     * Derives L2 credentials for {@code client}'s current authentication if not already present.
     *
     * @param nonce nonce to derive/create the API key with, see {@link PolyClient#deriveL2Credentials}
     * @return a future completing with this {@link UpDownClient} once ready to trade
     */
    public CompletableFuture<UpDownClient> initialize(String nonce) {
        if (!authenticated()) {
            return client.deriveL2Credentials(nonce).thenApply(ignored -> this);
        }

        return CompletableFuture.completedFuture(this);
    }

    /**
     * Resolves an Up/Down market's condition id and outcome token ids by its slug.
     *
     * @param slug market slug, e.g. {@code "btc-updown-15m-<epochSeconds>"}
     * @return a future completing with the resolved {@link UpDownMarket}, or {@code null} if the market
     *         data does not have exactly two outcomes/token ids
     */
    public CompletableFuture<UpDownMarket> getMarket(String slug) {
        return client.perform(client.request().markets().bySlug(slug), ResponseMapper.jsonMapper())
                .thenApply(node -> {
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

                        return new UpDownMarket(node.at("/conditionId").textValue(), up, down);
                    }
                    return null;
                });
    }

    /** Max attempts for a FOK market order before giving up and reporting a zero fill. */
    private static final int MAX_FOK_ATTEMPTS = 3;

    /**
     * Places a {@link SignatureType#DEPOSIT_WALLET}-signed FOK market order, sized against the current
     * order book depth, retrying up to {@link #MAX_FOK_ATTEMPTS} times if it gets killed.
     *
     * @param side {@link Side#BUY} or {@link Side#SELL}
     * @param assetId CLOB token id (asset id) being traded
     * @param cash USD notional to spend for a BUY, or number of shares to sell for a SELL
     * @return a future completing with the number of shares filled (BUY) or USD received (SELL),
     *         or {@code 0.0} if every attempt was killed
     */
    public CompletableFuture<Double> placeOrder(Side side, String assetId, double cash) {
        return placeOrder(side, assetId, cash, MAX_FOK_ATTEMPTS);
    }

    /**
     * Attempts a FOK market order against a freshly fetched book, retrying with a new snapshot if the
     * previous attempt was killed for insufficient depth (a stale-book race, not a real failure) &mdash;
     * see {@link pro.smdev.poly4j.utils.MarketPriceUtils#calculateMarketPrice}.
     *
     * @param side {@link Side#BUY} or {@link Side#SELL}
     * @param assetId CLOB token id (asset id) being traded
     * @param cash USD notional to spend for a BUY, or number of shares to sell for a SELL
     * @param attemptsLeft remaining attempts, including this one
     * @return a future completing with the filled amount, or {@code 0.0} once {@code attemptsLeft} is exhausted
     */
    private CompletableFuture<Double> placeOrder(Side side, String assetId, double cash, int attemptsLeft) {
        return client.perform(client.request().orderbook().getOrderBook(assetId), ResponseMapper.jsonMapper())
                .thenCompose(bookNode -> {
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

                    return client.perform(client.request().orders().postOrder(order, OrderType.FOK, false, false),
                                    ResponseMapper.jsonMapper())
                            .thenCompose(postOrderNode -> {
                                System.out.print(side + " -> ");
                                System.out.println(postOrderNode);
                                if (postOrderNode.has("takingAmount") && !postOrderNode.at("/takingAmount").isMissingNode()) {
                                    return CompletableFuture.completedFuture(
                                            Double.parseDouble(postOrderNode.at("/takingAmount").asText()));
                                }
                                if (attemptsLeft > 1) {
                                    return placeOrder(side, assetId, cash, attemptsLeft - 1);
                                }
                                return CompletableFuture.completedFuture(0.0);
                            });
                });
    }

    /**
     * Polls the positions API until a {@link #placeOrder BUY} order's fill has settled and been indexed,
     * since a matched order's on-chain settlement lags behind the "matched" response.
     *
     * @param market market the position belongs to, see {@link #getMarket}
     * @param outcome outcome name to match against, e.g. {@code "up"} or {@code "down"}
     * @param sharesAcquired number of shares expected to appear in the position, as returned by {@link #placeOrder}
     * @param recheckInterval milliseconds to wait between poll attempts (up to 15 attempts)
     * @return a future completing with the confirmed position size, or {@code 0.0} if it did not settle in time
     */
    public CompletableFuture<Double> awaitOpenPosition(UpDownMarket market, String outcome, double sharesAcquired, long recheckInterval) {
        return CompletableFuture.supplyAsync(() -> {
            double confirmedShares = 0;
            try {
                for (int attempt = 0; attempt < 15 && confirmedShares < sharesAcquired - 0.001; attempt++) {
                    TimeUnit.MILLISECONDS.sleep(recheckInterval);
                    var positionsNode = client.perform(client.request()
                                    .positions()
                                    .getPositions(client.getAuthentication().getFundAddress(), market.conditionId()), ResponseMapper.jsonMapper())
                            .get();
                    for (JsonNode position : positionsNode) {
                        if (position.at("/outcome").asText().equalsIgnoreCase(outcome)) {
                            confirmedShares = position.at("/size").asDouble();
                            break;
                        }
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                return 0.0;
            }
            return confirmedShares;
        });
    }

    private boolean authenticated() {
        return client.getAuthentication() != null && client.getAuthentication().getClobSecrets() != null;
    }

    private String[] splitJsonStringArray(String data) {
        String[] result = data.substring(2, data.length() - 2).split("\", \"");
        for (int i = 0; i < result.length; i++) {
            result[i] = result[i].replaceAll("\\[\"|\"]", "");
        }
        return result;
    }

}
