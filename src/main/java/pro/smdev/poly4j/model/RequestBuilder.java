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

import pro.smdev.poly4j.core.PolyClient;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *  This class contains all temporal configuration which will be used to build {@link HttpRequest}.
 *
 *  <p>Configure this object using builder-style syntax. New instance can be created using
 *  {@link pro.smdev.poly4j.factory.RequestFactoryHolder} which is provided by {@link PolyClient#request()}</p>
 *
 * @author ALazyGuy
 * @since 1.0.0
 */
public class RequestBuilder {

    private static final String POLYMARKET_GAMMA_API = "gamma-api.polymarket.com";
    private static final String POLYMARKET_DATA_API = "data-api.polymarket.com";
    private static final String POLYMARKET_CLOB_API = "clob.polymarket.com";

    private final Map<String, String> params = new HashMap<>();
    private final Map<String, String> headers = new HashMap<>();
    private final String domain;
    private RequestMethod requestMethod = RequestMethod.GET;
    private String postData;
    private String url;

    public RequestBuilder(String domain) {
        this.domain = domain;
    }

    public RequestBuilder url(String url) {
        this.url = url;
        return this;
    }

    public RequestBuilder get() {
        requestMethod = RequestMethod.GET;
        return this;
    }

    public RequestBuilder post(String data) {
        requestMethod = RequestMethod.POST;
        postData = data;
        return this;
    }

    public RequestBuilder addParam(String key, String value) {
        params.put(key, value);
        return this;
    }

    public RequestBuilder addHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public String toUrl() {
        String paramsString = "";

        if (requestMethod == RequestMethod.GET && !params.isEmpty()) {
            paramsString = "?" + params.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining("&"));
        }

        return String.format("https://%s%s%s", domain, url, paramsString);
    }

    public HttpRequest toHttpRequest() {
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        builder = builder.uri(URI.create(toUrl()));

        if (requestMethod == RequestMethod.POST) {
            builder = builder.POST(postData == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(postData));
        }

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.setHeader(entry.getKey(), entry.getValue());
        }

        return builder.build();
    }

    public static  RequestBuilder gammaApi() {
        return new RequestBuilder(POLYMARKET_GAMMA_API);
    }

    public static  RequestBuilder dataApi() {
        return new RequestBuilder(POLYMARKET_DATA_API);
    }

    public static  RequestBuilder clobApi() {
        return new RequestBuilder(POLYMARKET_CLOB_API);
    }

}

