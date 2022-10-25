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
@JsonPropertyOrder({ "ref", "properties", "uri" })
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
public class StepEndpoint {
    @JsonProperty("ref")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private ResourceRef ref;
    @JsonProperty("properties")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> properties = new TreeMap<>();

    @JsonProperty("uri")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String uri;

    public StepEndpoint() {
    }

    public StepEndpoint(ResourceRef ref) {
        this.ref = ref;
    }

    public StepEndpoint(ResourceRef ref, Map<String, Object> properties) {
        this.ref = ref;
        this.properties.putAll(properties);
    }

    public StepEndpoint(String apiVersion, String kind, String name) {
        this.ref = new ResourceRef(apiVersion, kind, name);
    }

    public StepEndpoint(String apiVersion, String kind, String name, Map<String, Object> properties) {
        this.ref = new ResourceRef(apiVersion, kind, name);
        this.properties.putAll(properties);
    }

    /**
     * Create a URI step endpoint. Note that this class used to be just a KameletEndpoint,
     * and other constructors are meant to be for creating kamelet step endpoint. When it's
     * used for a URI endpoint, use only this constructor and/or {@link #getUri()},
     * {@link #setUri(String)} and {@link #uri(String)} and never set other properties.
     * The other way is also true, do not set URI for a kamelet endpoint.
     *
     * @param uri The target URI to route
     */
    public StepEndpoint(String uri) {
        this.uri = uri;
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

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public static StepEndpoint kamelet(String name, Map<String, Object> properties) {
        return new StepEndpoint(
            Kamelet.RESOURCE_API_VERSION,
            Kamelet.RESOURCE_KIND,
            name,
            properties);
    }

    public static StepEndpoint kamelet(String name) {
        return new StepEndpoint(
            Kamelet.RESOURCE_API_VERSION,
            Kamelet.RESOURCE_KIND,
            name);
    }

    public static StepEndpoint kamelet(String name, Consumer<Map<String, Object>> consumer) {
        StepEndpoint answer = kamelet(name);
        consumer.accept(answer.getProperties());
        return answer;
    }

    public static StepEndpoint uri(String uri) {
        return new StepEndpoint(uri);
    }
}
