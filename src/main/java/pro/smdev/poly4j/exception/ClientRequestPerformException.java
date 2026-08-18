package pro.smdev.poly4j.exception;

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
 * Wraps an {@link java.io.IOException} or {@link InterruptedException} raised while sending a request via
 * {@link pro.smdev.poly4j.core.PolyClient#perform(pro.smdev.poly4j.model.RequestBuilder)}.
 *
 * @author ALazyGuy
 * @since 2.2.0
 */
public class ClientRequestPerformException extends RuntimeException {

    /**
     * Wraps the given cause, which was raised while sending the underlying HTTP request.
     *
     * @param cause the {@link java.io.IOException} or {@link InterruptedException} raised while sending the request
     */
    public ClientRequestPerformException(Throwable cause) {
        super(cause);
    }
}
