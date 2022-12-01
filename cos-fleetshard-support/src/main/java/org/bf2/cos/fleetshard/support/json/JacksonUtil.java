package org.bf2.cos.fleetshard.support.json;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.fabric8.kubernetes.client.KubernetesClientException;
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

    public static <T> String asPrettyPrintedJson(T object) {
        try {
            return Serialization.jsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw KubernetesClientException.launderThrowable(e);
        }
    }

    public static <T> String asPrettyPrintedYaml(T object) {
        try {
            return Serialization.yamlMapper().writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw KubernetesClientException.launderThrowable(e);
        }
    }

    public static ArrayNode asArrayNode(String... elements) {
        ArrayNode node = Serialization.jsonMapper().createArrayNode();
        for (String element : elements) {
            node.add(element);
        }

        return node;
    }

    public static ArrayNode asArrayNode(Collection<String> elements) {
        ArrayNode node = Serialization.jsonMapper().createArrayNode();
        for (String element : elements) {
            node.add(element);
        }

        return node;
    }

    public static String asArrayString(String... elements) {
        try {
            return Serialization.jsonMapper().writeValueAsString(asArrayNode(elements));
        } catch (JsonProcessingException e) {
            throw KubernetesClientException.launderThrowable(e);
        }
    }

    public static String asArrayString(Collection<String> elements) {
        try {
            return Serialization.jsonMapper().writeValueAsString(asArrayNode(elements));
        } catch (JsonProcessingException e) {
            throw KubernetesClientException.launderThrowable(e);
        }
    }

    public static JsonNode asJsonNode(Object object) {
        return Serialization.jsonMapper().valueToTree(object);
    }

    public static <T> T treeToValue(TreeNode node, Class<T> valueType) {
        try {
            return Serialization.jsonMapper().treeToValue(node, valueType);
        } catch (JsonProcessingException e) {
            throw KubernetesClientException.launderThrowable(e);
        }
    }
}
