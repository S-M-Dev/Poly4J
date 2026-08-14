package pro.smdev.poly4j.factory;

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

import pro.smdev.poly4j.exception.NotAuthenticatedException;
import pro.smdev.poly4j.model.Authentication;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for request factories that require authentication, validating that the necessary
 * credentials are present on the shared {@link Authentication} reference before a request is built.
 *
 * @author ALazyGuy
 * @since 2.0.0
 */
public abstract class AuthenticatedGuard {

    private final AtomicReference<Authentication> authentication;

    protected AuthenticatedGuard(AtomicReference<Authentication> authentication) {
        this.authentication = authentication;
    }

    /**
     * Validates that L1 (wallet-signature) credentials are available.
     *
     * @return the current {@link Authentication}
     * @throws NotAuthenticatedException if no {@link Authentication} or signer address has been set
     */
    protected Authentication validateL1() {
        Authentication result = authentication.get();
        if (result == null || result.getSignerAddress() == null) {
            throw new NotAuthenticatedException("No data for L1 authentication");
        }

        return result;
    }

    /**
     * Validates that L2 (CLOB API key) credentials are available, in addition to L1.
     *
     * @return the current {@link Authentication}
     * @throws NotAuthenticatedException if L1 is invalid or CLOB secrets have not been set
     */
    protected Authentication validateL2() {
        Authentication result = authentication.get();
        validateL1();
        if (result.getClobSecrets() == null) {
            throw new NotAuthenticatedException("No data for L2 authentication");
        }
        return result;
    }

    /**
     * Validates that builder API credentials are available, in addition to L1.
     *
     * @return the current {@link Authentication}
     * @throws NotAuthenticatedException if L1 is invalid or builder secrets have not been set
     */
    protected Authentication validateBuilder() {
        Authentication result = authentication.get();
        validateL1();
        if (result.getBuilderSecrets() == null) {
            throw new NotAuthenticatedException("No data for L2 authentication");
        }
        return result;
    }

}
