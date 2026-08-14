package pro.smdev.poly4j.model.dto;

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
 * The two outcome tokens of a Polymarket "Up/Down" market (e.g. {@code btc-updown-15m-*}), as resolved by
 * {@link pro.smdev.poly4j.core.UpDownClient#getMarket}.
 *
 * @param conditionId the market's condition id
 * @param upId CLOB token id (asset id) of the "Up" outcome
 * @param downId CLOB token id (asset id) of the "Down" outcome
 *
 * @author ALazyGuy
 * @since 2.1.1
 */
public record UpDownMarket(String conditionId, String upId, String downId) { }
