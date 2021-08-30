package org.bf2.cos.fleetshard.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.fabric8.kubernetes.model.annotation.PrinterColumn;
import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManagedConnectorClusterSpec {
    @PrinterColumn
    private String clusterId;

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }
}
