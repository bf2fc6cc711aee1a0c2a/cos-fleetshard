package org.bf2.cos.fleetshard.api;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.sundr.builder.annotations.Buildable;
import lombok.ToString;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
public class ResourceRef {
    private String apiVersion;
    private String kind;
    private String name;

    public ResourceRef() {
    }

    public ResourceRef(String apiVersion, String kind, String name) {
        this.apiVersion = apiVersion;
        this.kind = kind;
        this.name = name;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        return Objects.equals(getKind(), ref.getKind())
            && Objects.equals(getName(), ref.getName())
            && Objects.equals(getApiVersion(), ref.getApiVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKind(), getName(), getApiVersion());
    }
}
