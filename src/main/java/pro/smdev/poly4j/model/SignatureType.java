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
 * Identifies which kind of wallet signed an {@link Order}, matching the {@code signatureType}
 * field of the CLOB Exchange {@code Order} struct.
 *
 * <p>{@link #DEPOSIT_WALLET} orders are signed by the owning EOA but validated on-chain by the deposit
 * wallet's ERC-1271 {@code isValidSignature}, so {@link pro.smdev.poly4j.utils.OrderSigningUtils} wraps the
 * signature in an ERC-7739 nested {@code TypedDataSign} structure. For this type, both {@code maker} and
 * {@code signer} on the {@link Order} must be the deposit wallet's own address
 * ({@link Authentication#getFundAddress()}).</p>
 *
 * @author ALazyGuy
 * @since 2.0.0
 */
public enum SignatureType {
    EOA(0),
    POLY_PROXY(1),
    GNOSIS_SAFE(2),
    DEPOSIT_WALLET(3);

    private final int value;

    SignatureType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
