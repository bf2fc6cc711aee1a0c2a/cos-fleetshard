package org.bf2.cos.fleetshard.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.model.annotation.PrinterColumn;
import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Buildable(
    builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManagedConnectorClusterSpec {
    @PrinterColumn
    private String id;
    @PrinterColumn
    private String connectorsNamespace;

    @JsonProperty
    public String getId() {
        return id;
    }

    @JsonProperty
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty
    public String getConnectorsNamespace() {
        return connectorsNamespace;
    }

    @JsonProperty
    public void setConnectorsNamespace(String connectorsNamespace) {
        this.connectorsNamespace = connectorsNamespace;
    }
}