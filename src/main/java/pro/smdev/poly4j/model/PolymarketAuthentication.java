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

/**
 * Contains information about authenticated user. This object is needed for all requests which
 * require L2 authentication.
 *
 * <p>Can be created from {@link pro.smdev.poly4j.utils.AuthenticationUtils#buildAuthentication(Wallet, com.fasterxml.jackson.databind.JsonNode)}
 * using the response of {@link pro.smdev.poly4j.factory.AuthenticationRequestFactory}</p>
 *
 * @param wallet User wallet
 * @param apiKey existing api key for wallet
 * @param secret api secret for wallet
 * @param passphrase api passphrase
 *
 * @author ALazyGuy
 * @since 1.0.0
 */
public record PolymarketAuthentication(Wallet wallet, String apiKey, String secret, String passphrase) {
}
