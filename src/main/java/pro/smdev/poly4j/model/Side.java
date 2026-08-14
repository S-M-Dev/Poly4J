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
 * Side of an {@link Order}. The name of each constant is also the JSON wire value
 * expected by the CLOB API, while {@link #getValue()} is the {@code uint8} encoding
 * signed as part of the EIP-712 order struct (0 for BUY, 1 for SELL).
 *
 * @author ALazyGuy
 * @since 2.0.0
 */
public enum Side {
    BUY(0),
    SELL(1);

    private final int value;

    Side(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
