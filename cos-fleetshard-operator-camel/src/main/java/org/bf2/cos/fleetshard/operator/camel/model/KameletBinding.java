package org.bf2.cos.fleetshard.operator.camel.model;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.fabric8.kubernetes.api.Pluralize;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
public class KameletBinding implements HasMetadata, Namespaced {
    public static final String RESOURCE_GROUP = "camel.apache.org";
    public static final String RESOURCE_VERSION = "v1alpha1";
    public static final String RESOURCE_API_VERSION = RESOURCE_GROUP + "/" + RESOURCE_VERSION;
    public static final String RESOURCE_KIND = "KameletBinding";

    public static final ResourceDefinitionContext RESOURCE_DEFINITION = new ResourceDefinitionContext.Builder()
        .withNamespaced(true)
        .withGroup(RESOURCE_GROUP)
        .withVersion(RESOURCE_VERSION)
        .withKind(RESOURCE_KIND)
        .withPlural(Pluralize.toPlural(RESOURCE_KIND.toLowerCase(Locale.US)))
        .build();

    @JsonProperty("apiVersion")
    private String apiVersion = RESOURCE_API_VERSION;
    @JsonProperty("kind")
    private String kind = RESOURCE_KIND;

    @JsonProperty("metadata")
    private io.fabric8.kubernetes.api.model.ObjectMeta metadata;
    @JsonProperty("spec")
    private KameletBindingSpec spec;
    @JsonProperty("status")
    private KameletBindingStatus status;

    @Override
    public String getApiVersion() {
        return apiVersion;
    }

    @Override
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    @Override
    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    @Override
    public ObjectMeta getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(ObjectMeta metadata) {
        this.metadata = metadata;
    }

    public KameletBindingSpec getSpec() {
        return spec;
    }

    public void setSpec(KameletBindingSpec spec) {
        this.spec = spec;
    }

    public KameletBindingStatus getStatus() {
        return status;
    }

    public void setStatus(KameletBindingStatus status) {
        this.status = status;
    }
}
