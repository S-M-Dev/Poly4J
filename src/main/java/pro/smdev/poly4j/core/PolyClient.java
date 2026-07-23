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

import pro.smdev.poly4j.factory.RequestFactoryHolder;
import pro.smdev.poly4j.mapper.ResponseMapper;
import pro.smdev.poly4j.model.RequestBuilder;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * The main client for performing polymarket requests.
 *
 * <p>This class provides methods for requests building and performing operations.
 * Each performed request may be mapped by custom {@link ResponseMapper} or existing ones.</p>
 *
 * @author ALazyGuy
 * @since 1.0.0
 * @see RequestFactoryHolder
 * @see RequestBuilder
 */
public class PolyClient {

    private final RequestFactoryHolder requestFactoryHolder = new RequestFactoryHolder();
    private final HttpClient client;

    public PolyClient() {
        client = HttpClient.newHttpClient();
    }

    /**
     * Returns {@link RequestFactoryHolder} object to use builder-like semantic for requests.
     * @return {@link RequestFactoryHolder}
     */
    public RequestFactoryHolder request() {
        return requestFactoryHolder;
    }

    /**
     * Perform request and map response body to string.
     * @param requestBuilder configured request builder
     * @return {@link CompletableFuture} with response body as string
     */
    public CompletableFuture<String> perform(RequestBuilder requestBuilder) {
        return perform(requestBuilder, ResponseMapper.stringMapper());
    }

    /**
     * Perform request and map response body to type defined by generic {@link ResponseMapper mapper}.
     * @param requestBuilder configured request builder
     * @param <T> Type to which body will be cast.
     * @return {@link CompletableFuture} with response body.
     */
    public <T> CompletableFuture<T> perform(RequestBuilder requestBuilder, ResponseMapper<T> responseMapper) {
        return client.sendAsync(requestBuilder.toHttpRequest(), HttpResponse.BodyHandlers.ofString())
                .thenApply(responseMapper::map);
    }

}
