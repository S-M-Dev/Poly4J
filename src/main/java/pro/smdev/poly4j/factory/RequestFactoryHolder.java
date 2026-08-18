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
import pro.smdev.poly4j.model.RequestBuilder;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Aggregates every request factory, exposed as a single entry point via {@link pro.smdev.poly4j.core.PolyClient#request()}.
 *
 * @author ALazyGuy
 * @since 1.0.0
 * @see RequestBuilder
 * @see ProfileRequestFactory
 * @see MarketsRequestFactory
 * @see AuthenticationRequestFactory
 * @see OrdersRequestFactory
 */
public class RequestFactoryHolder {

    private final ProfileRequestFactory profileRequestFactory = new ProfileRequestFactory();
    private final MarketsRequestFactory marketsRequestFactory = new MarketsRequestFactory();
    private final OrderbookRequestFactory orderbookRequestFactory = new OrderbookRequestFactory();
    private final PositionsRequestFactory positionsRequestFactory = new PositionsRequestFactory();
    private final AuthenticationRequestFactory authenticationRequestFactory;
    private final BuilderRequestFactory builderRequestFactory;
    private final OrdersRequestFactory ordersRequestFactory;

    public RequestFactoryHolder(AtomicReference<Authentication> authentication) {
        authenticationRequestFactory = new AuthenticationRequestFactory(authentication);
        builderRequestFactory = new BuilderRequestFactory(authentication);
        ordersRequestFactory = new OrdersRequestFactory(authentication);
    }

    /**
     * @return factory for requests to the profile block
     */
    public ProfileRequestFactory profile() {
        return profileRequestFactory;
    }

    /**
     * @return factory for requests to the markets block
     */
    public MarketsRequestFactory markets() {
        return marketsRequestFactory;
    }

    /**
     * @return factory for requests to the authentication block
     */
    public AuthenticationRequestFactory authentication() {
        return authenticationRequestFactory;
    }

    /**
     * @return factory for requests to the orders block
     */
    public OrdersRequestFactory orders() {
        return ordersRequestFactory;
    }

    /**
     * @return factory for requests to the relayer/builder block
     */
    public BuilderRequestFactory builder() {
        return builderRequestFactory;
    }

    /**
     * @return factory for requests to the orderbook block
     */
    public OrderbookRequestFactory orderbook() {
        return orderbookRequestFactory;
    }

    /**
     * @return factory for requests to the positions block
     */
    public PositionsRequestFactory positions() {
        return positionsRequestFactory;
    }

}
