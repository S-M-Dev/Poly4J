package pro.smdev.poly4j.utils;

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
