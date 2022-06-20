package org.pragmatica.protocol.http.parser.uri;

import org.pragmatica.lang.Option;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class Parameters {
    private final Map<String, List<String>> parameters = new HashMap<>();

    public Option<List<String>> get(String name) {
        return Option.option(parameters.get(name));
    }

    public Parameters set(String name) {
        parameters.put(name, new ArrayList<>());
        return this;
    }

    public Parameters add(String name, String value) {
        parameters.compute(name, (key, list) -> computeList(list, value));
        return this;
    }

    public Parameters remove(String name) {
        parameters.remove(name);
        return this;
    }

    public Parameters remove(String name, String value) {
        parameters.computeIfPresent(name, (key, list) -> {
            list.remove(value);
            return list;
        });
        return this;
    }

    /**
     * Applies provided consumers to parameters. In case, when key corresponds to empty list, {@code emptyKeyConsumer} is invoked. Otherwise
     * {@code keyValueConsumer} is invoked to each key/value pair.
     *
     * @param keyValueConsumer regular key/value consumer
     * @param emptyKeyConsumer consumer for empty value list case
     */
    public Parameters forEach(BiConsumer<String, String> keyValueConsumer, Consumer<String> emptyKeyConsumer) {
        parameters.forEach((key, list) -> applyConsumers(keyValueConsumer, emptyKeyConsumer, key, list));
        return this;
    }

    private void applyConsumers(BiConsumer<String, String> keyValueConsumer, Consumer<String> emptyKeyConsumer, String key, List<String> list) {
        if (list.isEmpty()) {
            emptyKeyConsumer.accept(key);
        } else {
            list.forEach(value -> keyValueConsumer.accept(key, value));
        }
    }

    private static List<String> computeList(List<String> list, String value) {
        var listValue = list == null ? new ArrayList<String>() : list;

        listValue.add(value);

        return listValue;
    }
}
