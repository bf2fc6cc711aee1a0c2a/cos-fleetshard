package org.bf2.cos.fleetshard.support.json;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.fabric8.kubernetes.client.utils.Serialization;

public final class JacksonUtil {
    private JacksonUtil() {
    }

    @SuppressWarnings("unchecked")
    public static <T> Iterator<T> iterator(JsonNode node) {
        return node != null ? (Iterator<T>) node.fields() : Collections.emptyIterator();
    }

    public static <T> List<T> covertToListOf(Object fromValue, Class<T> type) {
        return Serialization.jsonMapper().convertValue(
            fromValue,
            TypeFactory.defaultInstance().constructCollectionType(List.class, type));
    }
}
