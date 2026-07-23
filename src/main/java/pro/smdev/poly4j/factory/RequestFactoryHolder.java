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

import pro.smdev.poly4j.model.RequestBuilder;

/**
 * @author ALazyGuy
 * @since 1.0.0
 * @see RequestBuilder
 * @see ProfileRequestFactory
 * @see MarketsRequestFactory
 */
public class RequestFactoryHolder {

    private final ProfileRequestFactory profileRequestFactory = new ProfileRequestFactory();
    private final MarketsRequestFactory marketsRequestFactory = new MarketsRequestFactory();

    public ProfileRequestFactory profile() {
        return profileRequestFactory;
    }

    public MarketsRequestFactory markets() {
        return marketsRequestFactory;
    }

}
