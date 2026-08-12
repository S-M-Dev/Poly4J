package pro.smdev.poly4j.factory;

import pro.smdev.poly4j.model.RequestBuilder;

public class OrderbookRequestFactory {

    public RequestBuilder getPrice(String tokenId, String side) {
        return RequestBuilder.clobApi()
                .get()
                .addParam("token_id", tokenId)
                .addParam("side", side)
                .url("/price");
    }

}
