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
import pro.smdev.poly4j.exception.ClientRequestPerformException;
import pro.smdev.poly4j.factory.RequestFactoryHolder;
import pro.smdev.poly4j.mapper.ResponseMapper;
import pro.smdev.poly4j.model.Authentication;
import pro.smdev.poly4j.model.RequestBuilder;
import pro.smdev.poly4j.model.Secrets;
import pro.smdev.poly4j.utils.AuthenticationUtils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
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

    private static final Logger log = LoggerFactory.getLogger(PolyClient.class);

    private final AtomicReference<Authentication> authentication = new AtomicReference<>();
    private final RequestFactoryHolder requestFactoryHolder = new RequestFactoryHolder(authentication);
    private final AtomicReference<UpDownClient> upDownClient = new AtomicReference<>();
    private final HttpClient client;

    /**
     * Creates a client backed by a default {@link HttpClient#newHttpClient()}.
     */
    public PolyClient() {
        this(HttpClient.newHttpClient());
    }

    /**
     * Creates a client backed by the given {@link HttpClient}.
     *
     * @param client HTTP client used to send every request built by this instance
     */
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
     * Synchronously performs the request and maps the response body to a {@link String}.
     *
     * @param requestBuilder configured request builder
     * @return the response body as a string
     */
    public String perform(RequestBuilder requestBuilder) {
        return perform(requestBuilder, ResponseMapper.stringMapper());
    }

    /**
     * Synchronously performs the request and maps the response body using {@code responseMapper}.
     *
     * @param requestBuilder configured request builder
     * @param responseMapper mapper applied to the raw {@link HttpResponse}
     * @param <T>            type to which the response body will be mapped
     * @return the mapped response body
     * @throws ClientRequestPerformException if the request fails or is interrupted
     */
    public <T> T perform(RequestBuilder requestBuilder, ResponseMapper<T> responseMapper) {
        try {
            HttpRequest httpRequest = requestBuilder.toHttpRequest();
            log.info("[{}] {}", httpRequest.method(), requestBuilder.toUrl());
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (body.length() > 100) {
                body = body.substring(0, 100) + "...";
            }
            log.trace("Response: Code[{}]\nBody:\n{}", response.statusCode(), body);
            return responseMapper.map(response);
        } catch (IOException | InterruptedException e) {
            throw new ClientRequestPerformException(e);
        }

    }

    /**
     * Perform request and map response body to string.
     *
     * @param requestBuilder configured request builder
     * @return {@link CompletableFuture} with response body as string
     */
    public CompletableFuture<String> performAsync(RequestBuilder requestBuilder) {
        return CompletableFuture.supplyAsync(() -> perform(requestBuilder));
    }

    /**
     * Perform request and map response body to type defined by generic {@link ResponseMapper mapper}.
     *
     * @param requestBuilder configured request builder
     * @param <T>            Type to which body will be cast.
     * @return {@link CompletableFuture} with response body.
     */
    public <T> CompletableFuture<T> performAsync(RequestBuilder requestBuilder, ResponseMapper<T> responseMapper) {
        return CompletableFuture.supplyAsync(() -> perform(requestBuilder, responseMapper));
    }

    /**
     * Sets the {@link Authentication} used to sign L1/L2/builder authenticated requests.
     *
     * @param authentication authentication holding the signer's wallet and derived credentials
     * @return this client
     */
    public PolyClient authenticated(Authentication authentication) {
        this.authentication.set(authentication);
        return this;
    }

    /**
     * Returns the currently configured {@link Authentication}, or {@code null} if none was set.
     *
     * @return the current {@link Authentication}
     */
    public Authentication getAuthentication() {
        return authentication.get();
    }

    /**
     * Returns the shared {@link AtomicReference} backing {@link #authenticated} and {@link #getAuthentication()},
     * used by request factories and {@link UpDownClient} to read authentication state.
     *
     * @return the shared {@link Authentication} reference
     */
    public AtomicReference<Authentication> getAuthenticationAtomic() {
        return authentication;
    }

    /**
     * Derives and stores L2 (CLOB API key) credentials for the current {@link #authenticated authentication},
     * creating a new API key if one does not already exist for {@code nonce}.
     *
     * @param nonce nonce to derive/create the API key with
     */
    public void deriveL2Credentials(String nonce) {
        JsonNode node = perform(request().authentication().deriveApiKey(nonce), ResponseMapper.jsonMapper());
        if (node.has("error")) {
            saveClobCredentials(perform(request().authentication().createApiKey(nonce), ResponseMapper.jsonMapper()));
        } else {
            saveClobCredentials(node);
        }
    }

    /**
     * Derives and stores L2 (CLOB API key) credentials for the current {@link #authenticated authentication},
     * creating a new API key if one does not already exist for {@code nonce}.
     *
     * @param nonce nonce to derive/create the API key with
     * @return a future that completes once the {@link Secrets} have been stored
     */
    public CompletableFuture<Void> deriveL2CredentialsAsync(String nonce) {
        return CompletableFuture.runAsync(() -> deriveL2Credentials(nonce));
    }

    /**
     * Returns (creating and initializing on first call) the {@link UpDownClient} for this client, deriving
     * L2 credentials with {@code nonce} if not already present.
     *
     * @param nonce nonce to derive/create the API key with, see {@link #deriveL2CredentialsAsync}
     * @return a future completing with the ready-to-trade {@link UpDownClient}
     */
    public CompletableFuture<UpDownClient> getUpDownClientAsync(String nonce) {
        return CompletableFuture.supplyAsync(() -> getUpDownClient(nonce));
    }

    /**
     * Returns (creating and initializing on first call) the {@link UpDownClient} for this client, deriving
     * L2 credentials with {@code nonce} if not already present.
     *
     * @param nonce nonce to derive/create the API key with, see {@link #deriveL2Credentials}
     * @return the ready-to-trade {@link UpDownClient}
     */
    public UpDownClient getUpDownClient(String nonce) {
        if (upDownClient.compareAndSet(null, new UpDownClient(this))) {
            return upDownClient.get().initialize(nonce);
        }
        return upDownClient.get();
    }

    /**
     * Parses a {@code create-api-key}/{@code derive-api-key} response and stores the resulting
     * CLOB {@link Secrets} on the current {@link #authenticated authentication}.
     *
     * @param node JSON response node containing {@code apiKey}, {@code secret} and {@code passphrase}
     */
    private void saveClobCredentials(JsonNode node) {
        Secrets secrets = AuthenticationUtils.buildClobSecrets(node);
        Authentication authentication = this.authentication.get();
        authentication.setClobSecrets(secrets);
    }

}
