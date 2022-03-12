package org.bf2.cos.fleetshard.operator.camel.model;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.bf2.cos.fleetshard.api.ResourceRef;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "ref", "properties" })
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
public class KameletEndpoint {
    @JsonProperty("ref")
    private ResourceRef ref;
    @JsonProperty("properties")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> properties = new TreeMap<>();

    public KameletEndpoint() {
    }

    public KameletEndpoint(ResourceRef ref) {
        this.ref = ref;
    }

    public KameletEndpoint(ResourceRef ref, Map<String, Object> properties) {
        this.ref = ref;
        this.properties.putAll(properties);
    }

    public KameletEndpoint(String apiVersion, String kind, String name) {
        this.ref = new ResourceRef(apiVersion, kind, name);
    }

    public KameletEndpoint(String apiVersion, String kind, String name, Map<String, Object> properties) {
        this.ref = new ResourceRef(apiVersion, kind, name);
        this.properties.putAll(properties);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public ResourceRef getRef() {
        return ref;
    }

    public void setRef(ResourceRef ref) {
        this.ref = ref;
    }

    public void setRef(String apiVersion, String kind, String name) {
        setRef(new ResourceRef(apiVersion, kind, name));
    }

    public static KameletEndpoint kamelet(String name, Map<String, Object> properties) {
        return new KameletEndpoint(
            Kamelet.RESOURCE_API_VERSION,
            Kamelet.RESOURCE_KIND,
            name,
            properties);
    }

    public static KameletEndpoint kamelet(String name) {
        return new KameletEndpoint(
            Kamelet.RESOURCE_API_VERSION,
            Kamelet.RESOURCE_KIND,
            name);
    }

    public static KameletEndpoint kamelet(String name, Consumer<Map<String, Object>> consumer) {
        KameletEndpoint answer = kamelet(name);
        consumer.accept(answer.getProperties());
        return answer;
    }
}
