package org.bf2.cos.fleetshard.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectorSpec {
    private long connectorResourceVersion;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<StatusExtractor> statusExtractors = new ArrayList<>();
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ResourceRef> resources = new ArrayList<>();

    public List<ResourceRef> getResources() {
        return resources;
    }

    public void setResources(List<ResourceRef> resources) {
        this.resources = resources;
    }

    public long getConnectorResourceVersion() {
        return connectorResourceVersion;
    }

    public void setConnectorResourceVersion(long connectorResourceVersion) {
        this.connectorResourceVersion = connectorResourceVersion;
    }

    public List<StatusExtractor> getStatusExtractors() {
        return statusExtractors;
    }

    public void setStatusExtractors(List<StatusExtractor> statusExtractors) {
        this.statusExtractors = statusExtractors;
    }

    @JsonIgnore
    public boolean shouldStatusBeExtracted(ObjectReference reference) {
        for (StatusExtractor extractor : statusExtractors) {
            if (Objects.equals(reference.getApiVersion(), extractor.getApiVersion()) &&
                    Objects.equals(reference.getKind(), extractor.getKind()) &&
                    Objects.equals(reference.getName(), extractor.getName())) {
                return true;
            }
        }

        return false;
    }
}