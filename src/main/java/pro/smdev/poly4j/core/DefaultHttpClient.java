package pro.smdev.poly4j.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.smdev.poly4j.model.RequestBuilder;

import java.net.http.HttpRequest;

public class DefaultHttpClient implements HttpClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultHttpClient.class);
    private final java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

    @Override
    public HttpResponse<String> send(RequestBuilder builder) throws Exception {
        HttpRequest httpRequest = builder.toHttpRequest();
        log.info("[{}] {}", httpRequest.method(), builder.toUrl());
        java.net.http.HttpResponse<String> response = client.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
        return new HttpResponse<>(response.body(), response.statusCode());
    }
}
