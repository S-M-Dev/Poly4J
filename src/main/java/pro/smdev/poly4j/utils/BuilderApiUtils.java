package pro.smdev.poly4j.utils;

import pro.smdev.poly4j.model.Wallet;

public class BuilderApiUtils {
    private static final String WALLET_CREATE = "{" +
            "\"type\": \"WALLET-CREATE\"," +
            "\"from\": \"%s\"," +
            "\"to\": \"0x00000000000Fb5C9ADea0298D729A0CB3823Cc07\"," +
            "\"metadata\": \"Deploy Deposit Wallet\"" +
            "}";

    public static String walletCreate(String signer) {
        return WALLET_CREATE.formatted(signer);
    }
}
