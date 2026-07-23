package pro.smdev.poly4j.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import pro.smdev.poly4j.factory.MarketsRequestFactory;
import pro.smdev.poly4j.factory.ProfileRequestFactory;
import pro.smdev.poly4j.model.RequestBuilder;

import java.net.http.HttpResponse;

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
 * Declares mapping method to convert HttpResponse&lt;String&gt; to any other type
 *
 * @param <T> type to which response will be converted
 *
 * @author ALazyGuy
 * @since 1.0.0
 * @see StringResponseMapper
 * @see JsonResponseMapper
 */
@FunctionalInterface
public interface ResponseMapper<T> {
    T map(HttpResponse<String> response);

    static ResponseMapper<String> stringMapper() {
        return new StringResponseMapper();
    }

    static ResponseMapper<JsonNode> jsonMapper() {
        return new JsonResponseMapper();
    }

}
