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
import pro.smdev.poly4j.model.Side;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Computes a depth-aware "market order" price from a {@code GET /book} response, mirroring
 * {@code calculateBuyMarketPrice}/{@code calculateSellMarketPrice} in Polymarket's official
 * {@code clob-client-v2} SDK.
 * <br>
 * API:
 * <a href="https://docs.polymarket.com/api-reference/market-data/get-order-book">https://docs.polymarket.com/api-reference/market-data/get-order-book</a>
 *
 * <p>Polymarket's own docs and third-party reports disagree on whether {@code bids}/{@code asks} are returned
 * best-price-first or worst-price-first, so this class sorts each side itself before walking it &mdash;
 * correct regardless of the array's original order.</p>
 *
 * @author ALazyGuy
 * @since 2.0.0
 */
public class MarketPriceUtils {

    /**
     * Walks the book from the best price outward, accumulating depth until {@code amountToMatch} is covered,
     * and returns the worst price needed to fill it &mdash; the price a marketable order must be placed at to
     * guarantee a full fill against current liquidity.
     *
     * @param book {@code GET /book} response, must contain a {@code bids} or {@code asks} array (matching {@code side})
     * @param side {@link Side#BUY} walks {@code asks} accumulating notional (price &times; size, i.e. USD to spend);
     *             {@link Side#SELL} walks {@code bids} accumulating size (shares to sell)
     * @param amountToMatch USD notional to spend (BUY) or number of shares to sell (SELL)
     * @return the worst execution price required to fill {@code amountToMatch}
     * @throws IllegalStateException if the book does not have enough depth to fill {@code amountToMatch}
     */
    public static double calculateMarketPrice(JsonNode book, Side side, double amountToMatch) {
        JsonNode levelsNode = book.get(side == Side.BUY ? "asks" : "bids");

        List<double[]> levels = new ArrayList<>();
        for (JsonNode level : levelsNode) {
            levels.add(new double[]{Double.parseDouble(level.get("price").asText()),
                    Double.parseDouble(level.get("size").asText())});
        }

        Comparator<double[]> bestPriceFirst = side == Side.BUY
                ? Comparator.comparingDouble(level -> level[0])
                : Comparator.comparingDouble((double[] level) -> level[0]).reversed();
        levels.sort(bestPriceFirst);

        double cumulative = 0;
        for (double[] level : levels) {
            double price = level[0];
            double size = level[1];
            cumulative += side == Side.BUY ? price * size : size;
            if (cumulative >= amountToMatch) {
                return price;
            }
        }

        throw new IllegalStateException("Order book does not have enough depth to fill a market order of size "
                + amountToMatch);
    }

    /**
     * Rounds {@code value} down to at most {@code decimals} decimal places. The CLOB API rejects market orders
     * whose implied maker/taker amounts carry more precision than it allows, so amounts must be rounded to the
     * correct granularity (see {@link #MAKER_AMOUNT_DECIMALS} / {@link #takerAmountDecimals}) <em>before</em>
     * being scaled into the 6-decimal fixed-point integers the API expects.
     */
    public static double roundDown(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.floor(value * factor) / factor;
    }

    /**
     * For a market order, the maker amount is always rounded to this many decimals, on both sides: the USD
     * amount for a BUY, or the share amount for a SELL. Mirrors {@code roundConfig.size} in Polymarket's
     * official {@code clob-client-v2} SDK ({@code roundingConfig.ts}), which is 2 for every tick size.
     */
    public static final int MAKER_AMOUNT_DECIMALS = 2;

    /**
     * For a market order, the taker amount (shares for a BUY, USD for a SELL) is rounded to this many decimals,
     * depending on the market's tick size. Mirrors {@code roundConfig.amount} in Polymarket's official
     * {@code clob-client-v2} SDK ({@code roundingConfig.ts}).
     *
     * @param tickSize the market's {@code tick_size}/{@code minimum_tick_size}, e.g. from {@code GET /book}
     */
    public static int takerAmountDecimals(String tickSize) {
        return switch (tickSize) {
            case "0.1" -> 3;
            case "0.01" -> 4;
            case "0.005" -> 5;
            case "0.0025" -> 6;
            case "0.001" -> 5;
            case "0.0001" -> 6;
            default -> throw new IllegalArgumentException("Unknown tick size: " + tickSize);
        };
    }

}
