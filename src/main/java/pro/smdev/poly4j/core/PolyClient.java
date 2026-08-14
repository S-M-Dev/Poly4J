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
import pro.smdev.poly4j.factory.RequestFactoryHolder;
import pro.smdev.poly4j.mapper.ResponseMapper;
import pro.smdev.poly4j.model.Authentication;
import pro.smdev.poly4j.model.RequestBuilder;
import pro.smdev.poly4j.model.Secrets;
import pro.smdev.poly4j.utils.AuthenticationUtils;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The main client for performing polymarket requests.
 *
 * <p>This class provides methods for requests building and performing operations.
 * Each performed request may be mapped by custom {@link ResponseMapper} or existing ones.</p>
 *
 * @author ALazyGuy
 * @see RequestFactoryHolder
 * @see RequestBuilder
 * @since 1.0.0
 */
public class PolyClient {

    private final AtomicReference<Authentication> authentication = new AtomicReference<>();
    private final RequestFactoryHolder requestFactoryHolder = new RequestFactoryHolder(authentication);
    private final AtomicReference<UpDownClient> upDownClient = new AtomicReference<>();
    private final HttpClient client;

    public PolyClient() {
        this(HttpClient.newHttpClient());
    }

    public PolyClient(HttpClient client) {
        this.client = client;
    }

    /**
     * Returns {@link RequestFactoryHolder} object to use builder-like semantic for requests.
     *
     * @return {@link RequestFactoryHolder}
     */
    public RequestFactoryHolder request() {
        return requestFactoryHolder;
    }

    /**
     * Perform request and map response body to string.
     *
     * @param requestBuilder configured request builder
     * @return {@link CompletableFuture} with response body as string
     */
    public CompletableFuture<String> perform(RequestBuilder requestBuilder) {
        return perform(requestBuilder, ResponseMapper.stringMapper());
    }

    /**
     * Perform request and map response body to type defined by generic {@link ResponseMapper mapper}.
     *
     * @param requestBuilder configured request builder
     * @param <T>            Type to which body will be cast.
     * @return {@link CompletableFuture} with response body.
     */
    public <T> CompletableFuture<T> perform(RequestBuilder requestBuilder, ResponseMapper<T> responseMapper) {
        return client.sendAsync(requestBuilder.toHttpRequest(), HttpResponse.BodyHandlers.ofString())
                .thenApply(responseMapper::map);
    }

    public PolyClient authenticated(Authentication authentication) {
        this.authentication.set(authentication);
        return this;
    }

    public Authentication getAuthentication() {
        return authentication.get();
    }

    public AtomicReference<Authentication> getAuthenticationAtomic() {
        return authentication;
    }

    /**
     * Derives and stores L2 (CLOB API key) credentials for the current {@link #authenticated authentication},
     * creating a new API key if one does not already exist for {@code nonce}.
     *
     * @param nonce nonce to derive/create the API key with
     * @return a future that completes once the {@link Secrets} have been stored
     */
    public CompletableFuture<Void> deriveL2Credentials(String nonce) {
        return perform(request().authentication().deriveApiKey(nonce), ResponseMapper.jsonMapper())
                .thenCompose(node -> {
                    if (node.has("error")) {
                        return perform(request().authentication().createApiKey(nonce), ResponseMapper.jsonMapper())
                                .thenCompose(this::saveClobCredentials);
                    } else {
                        return saveClobCredentials(node);
                    }
                });
    }

    public synchronized CompletableFuture<UpDownClient> getUpDownClient(String nonce) {
        if (upDownClient.get() == null) {
            UpDownClient cl = new UpDownClient(this);
            upDownClient.set(cl);
            return cl.initialize(nonce);
        }

        return CompletableFuture.completedFuture(upDownClient.get());
    }

    private CompletableFuture<Void> saveClobCredentials(JsonNode node) {
        Secrets secrets = AuthenticationUtils.buildClobSecrets(node);
        Authentication authentication = this.authentication.get();
        authentication.setClobSecrets(secrets);
        return CompletableFuture.completedFuture(null);
    }

}
