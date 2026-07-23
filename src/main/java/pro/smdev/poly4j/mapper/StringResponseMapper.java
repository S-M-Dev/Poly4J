package pro.smdev.poly4j.mapper;

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

import java.net.http.HttpResponse;

/**
 * Mapper from HttpResponse&lt;String&gt; to String
 *
 * @author ALazyGuy
 * @since 1.0.0
 * @see ResponseMapper
 */
public class StringResponseMapper implements ResponseMapper<String> {

    @Override
    public String map(HttpResponse<String> response) {
        return response.body();
    }
}
