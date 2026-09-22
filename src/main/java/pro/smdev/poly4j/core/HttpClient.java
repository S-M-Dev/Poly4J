package pro.smdev.poly4j.core;

import pro.smdev.poly4j.model.RequestBuilder;

public interface HttpClient {
    HttpResponse<String> send(RequestBuilder builder) throws Exception;
}
