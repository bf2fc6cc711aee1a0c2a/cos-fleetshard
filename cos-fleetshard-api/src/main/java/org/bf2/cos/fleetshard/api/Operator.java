package org.bf2.cos.fleetshard.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Operator {
    private String id;
    private String type;
    private String version;
    private String metaService;

    public Operator() {
        this(null, null, null, null);
    }

    public Operator(String id, String type, String version) {
        this(id, type, version, null);
    }

    public Operator(String id, String type, String version, String metaService) {
        this.id = id;
        this.type = type;
        this.version = version;
        this.metaService = metaService;
    }

    @JsonProperty
    public String getId() {
        return id;
    }

    @JsonProperty
    public void setId(String id) {
        this.id = id;
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
    public String getMetaService() {
        return metaService;
    }

    @JsonProperty
    public void setMetaService(String metaService) {
        this.metaService = metaService;
    }
}
