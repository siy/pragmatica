/*
 *  Copyright (c) 2022 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.pragmatica.protocol.http.uri.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.pragmatica.lang.Option.option;

public class QueryParameters {
    private final Map<String, List<String>> data = new HashMap<>();

    private QueryParameters(Map<String, List<String>> data) {
        this.data.putAll(data);
    }

    public static QueryParameters parameters() {
        return new QueryParameters(Map.of());
    }

    /**
     * Make a mutable copy.
     */
    public QueryParameters deepCopy() {
        return new QueryParameters(data);
    }

    public QueryParameters add(final String key, final String value) {
        data.compute(key, (aKey, list) -> computeEntry(list, value));
        return this;
    }

    private static List<String> computeEntry(List<String> existingList, String value) {
        var list = existingList == null ? new ArrayList<String>() : existingList;
        list.add(value);

        return list;
    }

    public QueryParameters put(final String key, final List<String> value) {
        data.put(key, value);

        return this;
    }

    public QueryParameters remove(final String key, String value) {
        option(data.get(key)).map(list -> list.remove(value));

        return this;
    }

    public QueryParameters remove(final String key) {
        data.remove(key);

        return this;
    }

    public QueryParameters replace(String key, String newValue) {
        data.compute(key, (aKey, list) -> computeReplace(list, newValue));
        return this;
    }

    private static List<String> computeReplace(List<String> list, String newValue) {
        var newList = list == null ? new ArrayList<String>() : list;

        newList.clear();
        newList.add(newValue);

        return newList;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof final QueryParameters otherParameters && data.equals(otherParameters.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public void encode(StringBuilder out) {
        if (data.isEmpty()) {
            return;
        }

        out.append('?');
        for (var entry : data.entrySet()) {
            var key = entry.getKey();

            for (var value : entry.getValue()) {
                out.append(Encoder.encodeQueryElement(key))
                   .append('=');

                if (value != null) {
                   out.append(Encoder.encodeQueryElement(value));
                }
                out.append('&');
            }
        }
        out.setLength(out.length() - 1);
    }
}
