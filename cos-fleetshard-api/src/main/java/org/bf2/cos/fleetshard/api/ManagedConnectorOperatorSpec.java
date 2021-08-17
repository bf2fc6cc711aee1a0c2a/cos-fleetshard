package org.bf2.cos.fleetshard.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.model.annotation.PrinterColumn;
import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManagedConnectorOperatorSpec {
    @PrinterColumn
    private String type;
    @PrinterColumn
    private String version;
    @PrinterColumn
    private String runtime;

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
    public String getRuntime() {
        return runtime;
    }

    @JsonProperty
    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }
}