package org.bf2.cos.fleetshard.api;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.model.annotation.PrinterColumn;
import io.sundr.builder.annotations.Buildable;

@Buildable(
    builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Operator {
    @PrinterColumn
    private String type;
    @PrinterColumn
    private String version;
    @PrinterColumn
    private String namespace;
    @PrinterColumn
    private String metaService;

    public Operator() {
        this(null, null, null);
    }

    public Operator(String type, String version) {
        this(null, type, version);
    }

    public Operator(String namespace, String type, String version) {
        this.namespace = namespace;
        this.type = type;
        this.version = version;
    }

    public Operator(String namespace, String type, String version, String metaService) {
        this.namespace = namespace;
        this.type = type;
        this.version = version;
        this.metaService = metaService;
    }

    @JsonProperty
    public String getType() {
        return type;
    }

    @JsonProperty
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty
    public String getVersion() {
        return version;
    }

    @JsonProperty
    public void setVersion(String version) {
        this.version = version;
    }

    @JsonProperty
    public String getNamespace() {
        return namespace;
    }

    @JsonProperty
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @JsonProperty
    public String getMetaService() {
        return metaService;
    }

    @JsonProperty
    public void setMetaService(String metaService) {
        this.metaService = metaService;
    }

    @JsonIgnore
    public String id() {
        return Objects.requireNonNull(this.type) + "-" + Objects.requireNonNull(this.version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Operator)) {
            return false;
        }
        Operator operator = (Operator) o;
        return Objects.equals(getType(), operator.getType())
            && Objects.equals(getVersion(), operator.getVersion())
            && Objects.equals(getNamespace(), operator.getNamespace())
            && Objects.equals(getMetaService(), operator.getMetaService());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            getType(),
            getVersion(),
            getNamespace(),
            getMetaService());
    }

    @Override
    public String toString() {
        return "Operator{" +
            "type='" + type + '\'' +
            ", version='" + version + '\'' +
            ", namespace='" + namespace + '\'' +
            ", metaService='" + metaService + '\'' +
            '}';
    }
}
