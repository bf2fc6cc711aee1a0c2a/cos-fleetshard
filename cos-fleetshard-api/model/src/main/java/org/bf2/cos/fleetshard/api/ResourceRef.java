package org.bf2.cos.fleetshard.api;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.sundr.builder.annotations.Buildable;
import lombok.ToString;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonPropertyOrder({
    "apiVersion",
    "kind",
    "name",
    "namespace"
})
public class ResourceRef {
    @JsonProperty
    private String apiVersion;
    @JsonProperty
    private String kind;
    @JsonProperty
    private String name;
    @JsonProperty
    private String namespace;

    public ResourceRef() {
    }

    public ResourceRef(String apiVersion, String kind, String name) {
        this(apiVersion, kind, name, null);
    }

    public ResourceRef(String apiVersion, String kind, String name, String namespace) {
        this.apiVersion = apiVersion;
        this.kind = kind;
        this.name = name;
        this.namespace = namespace;
    }

    @JsonProperty
    public String getApiVersion() {
        return apiVersion;
    }

    @JsonProperty
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    @JsonProperty
    public String getKind() {
        return kind;
    }

    @JsonProperty
    public void setKind(String kind) {
        this.kind = kind;
    }

    @JsonProperty
    public String getName() {
        return name;
    }

    @JsonProperty
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty
    public String getNamespace() {
        return namespace;
    }

    @JsonProperty
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @JsonIgnore
    public boolean is(String apiVersion, String kind, String name) {
        Objects.requireNonNull(apiVersion, "apiVersion");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(name, "name");

        return Objects.equals(this.apiVersion, apiVersion)
            && Objects.equals(this.kind, kind)
            && Objects.equals(this.name, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResourceRef)) {
            return false;
        }
        ResourceRef ref = (ResourceRef) o;
        return Objects.equals(getApiVersion(), ref.getApiVersion())
            && Objects.equals(getKind(), ref.getKind())
            && Objects.equals(getName(), ref.getName())
            && Objects.equals(getNamespace(), ref.getNamespace());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            getApiVersion(),
            getKind(),
            getName(),
            getNamespace());
    }
}
