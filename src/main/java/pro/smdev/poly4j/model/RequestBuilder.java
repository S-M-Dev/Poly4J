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

import org.web3j.crypto.Keys;
import pro.smdev.poly4j.core.PolyClient;
import pro.smdev.poly4j.utils.AuthenticationUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class contains all temporal configuration which will be used to build {@link HttpRequest}.
 *
 * <p>Configure this object using builder-style syntax. New instance can be created using
 * {@link pro.smdev.poly4j.factory.RequestFactoryHolder} which is provided by {@link PolyClient#request()}</p>
 *
 * @author ALazyGuy
 * @since 1.0.0
 */
public class RequestBuilder {

    private static final String POLYMARKET_GAMMA_API = "gamma-api.polymarket.com";
    private static final String POLYMARKET_DATA_API = "data-api.polymarket.com";
    private static final String POLYMARKET_CLOB_API = "clob.polymarket.com";
    private static final String POLYMARKET_REPLAYER_API = "relayer-v2.polymarket.com";

    private final Map<String, String> params = new HashMap<>();
    private final Map<String, String> headers = new HashMap<>();
    private final String domain;
    private RequestMethod requestMethod = RequestMethod.GET;
    private String postData;
    private String url;

    /**
     * Creates a builder targeting {@code domain}. Prefer the {@link #gammaApi()}/{@link #dataApi()}/
     * {@link #clobApi()}/{@link #relayerApi()} factory methods over calling this directly.
     *
     * @param domain host to send the request to, e.g. {@code clob.polymarket.com}
     */
    public RequestBuilder(String domain) {
        this.domain = domain;
    }

    /**
     * Sets the request path.
     *
     * @param url path, e.g. {@code "/book"}
     * @return this builder
     */
    public RequestBuilder url(String url) {
        this.url = url;
        return this;
    }

    /**
     * Configures this request as a {@code GET}.
     *
     * @return this builder
     */
    public RequestBuilder get() {
        requestMethod = RequestMethod.GET;
        return this;
    }

    /**
     * Configures this request as a {@code POST}.
     *
     * @param data request body, or {@code null} for no body
     * @return this builder
     */
    public RequestBuilder post(String data) {
        requestMethod = RequestMethod.POST;
        postData = data;
        return this;
    }

    /**
     * Configures this request as a {@code DELETE}.
     *
     * @param data request body, or {@code null} for no body
     * @return this builder
     */
    public RequestBuilder delete(String data) {
        requestMethod = RequestMethod.DELETE;
        postData = data;
        return this;
    }

    /**
     * Adds a query parameter, applied to the request URL only when the method is {@code GET}.
     *
     * @param key   parameter name
     * @param value parameter value
     * @return this builder
     */
    public RequestBuilder addParam(String key, String value) {
        params.put(key, value);
        return this;
    }

    /**
     * Adds an HTTP header to the request.
     *
     * @param key   header name
     * @param value header value
     * @return this builder
     */
    public RequestBuilder addHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    /**
     * Adds the {@code POLY_ADDRESS}, {@code POLY_SIGNATURE}, {@code POLY_TIMESTAMP} and {@code POLY_NONCE}
     * headers required for L1 (wallet-signature) authenticated endpoints.
     *
     * @param authentication {@link Authentication} holding the signer's address/private key
     * @param nonce request nonce
     * @return this builder
     */
    public RequestBuilder authenticatedL1(Authentication authentication, String nonce) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        return addHeader("POLY_ADDRESS", Keys.toChecksumAddress(authentication.getSignerAddress()))
                .addHeader("POLY_SIGNATURE", AuthenticationUtils.encodeL1Signature(authentication, timestamp, nonce))
                .addHeader("POLY_TIMESTAMP", timestamp)
                .addHeader("POLY_NONCE", nonce);
    }

    /**
     * Adds the {@code POLY_API_KEY}, {@code POLY_ADDRESS}, {@code POLY_SIGNATURE}, {@code POLY_PASSPHRASE}
     * and {@code POLY_TIMESTAMP} headers required for L2 (CLOB API key) authenticated endpoints.
     *
     * @param authentication {@link Authentication} holding the CLOB {@link pro.smdev.poly4j.model.Secrets}
     * @return this builder
     */
    public RequestBuilder authenticatedL2(Authentication authentication) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        return addHeader("POLY_API_KEY", authentication.getClobSecrets().key())
                .addHeader("POLY_ADDRESS", Keys.toChecksumAddress(authentication.getSignerAddress()))
                .addHeader("POLY_SIGNATURE", AuthenticationUtils.encodeL2Signature(authentication.getClobSecrets().secret(),
                        timestamp, requestMethod.name().toUpperCase(Locale.ROOT), url, requestMethod == RequestMethod.POST ? Objects.requireNonNullElse(postData, "") : ""))
                .addHeader("POLY_PASSPHRASE", authentication.getClobSecrets().passphrase())
                .addHeader("POLY_TIMESTAMP", timestamp);
    }

    /**
     * Builds the full request URL, including query parameters when the method is {@code GET}.
     *
     * @return the absolute {@code https://} URL for this request
     */
    public String toUrl() {
        String paramsString = "";

        if (requestMethod == RequestMethod.GET && !params.isEmpty()) {
            paramsString = "?" + params.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining("&"));
        }

        return String.format("https://%s%s%s", domain, url, paramsString);
    }

    /**
     * Builds the {@link HttpRequest} to send, from this builder's configured method, URL, params,
     * headers and body.
     *
     * @return the built {@link HttpRequest}
     */
    public HttpRequest toHttpRequest() {
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        builder = builder.uri(URI.create(toUrl()));

        if (requestMethod == RequestMethod.POST) {
            builder = builder.POST(postData == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(postData));
        } else if (requestMethod == RequestMethod.DELETE) {
            builder = builder.method("DELETE", postData == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(postData));
        }

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.setHeader(entry.getKey(), entry.getValue());
        }

        return builder.build();
    }

    /**
     * @return a builder targeting the Gamma API ({@code gamma-api.polymarket.com})
     */
    public static RequestBuilder gammaApi() {
        return new RequestBuilder(POLYMARKET_GAMMA_API);
    }

    /**
     * @return a builder targeting the Data API ({@code data-api.polymarket.com})
     */
    public static RequestBuilder dataApi() {
        return new RequestBuilder(POLYMARKET_DATA_API);
    }

    /**
     * @return a builder targeting the CLOB API ({@code clob.polymarket.com})
     */
    public static RequestBuilder clobApi() {
        return new RequestBuilder(POLYMARKET_CLOB_API);
    }

    /**
     * @return a builder targeting the relayer API ({@code relayer-v2.polymarket.com})
     */
    public static RequestBuilder relayerApi() {
        return new RequestBuilder(POLYMARKET_REPLAYER_API);
    }

}

