package org.bf2.cos.fleetshard.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectorDeployment {
    private String id;
    private Spec spec;
    private Status status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Spec getSpec() {
        return spec;
    }

    public void setSpec(Spec spec) {
        this.spec = spec;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @ToString
    @EqualsAndHashCode
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Spec {
        private long resourceVersion;
        private String operatorId;
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<JsonNode> resources = new ArrayList<>();
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<StatusExtractor> statusExtractors = new ArrayList<>();

        public long getResourceVersion() {
            return resourceVersion;
        }

        public void setResourceVersion(long resourceVersion) {
            this.resourceVersion = resourceVersion;
        }

        public String getOperatorId() {
            return operatorId;
        }

        public void setOperatorId(String operatorId) {
            this.operatorId = operatorId;
        }

        public List<JsonNode> getResources() {
            return resources;
        }

        public void setResources(List<JsonNode> resources) {
            this.resources = resources;
        }

        public List<StatusExtractor> getStatusExtractors() {
            return statusExtractors;
        }

        public void setStatusExtractors(List<StatusExtractor> statusExtractors) {
            this.statusExtractors = statusExtractors;
        }
    }

    @ToString
    @EqualsAndHashCode
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Status extends org.bf2.cos.fleetshard.api.Status {
        private String operatorId;

        public String getOperatorId() {
            return operatorId;
        }

        public void setOperatorId(String operatorId) {
            this.operatorId = operatorId;
        }
    }

}
