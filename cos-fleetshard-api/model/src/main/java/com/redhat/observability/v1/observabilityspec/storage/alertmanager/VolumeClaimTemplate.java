package com.redhat.observability.v1.observabilityspec.storage.alertmanager;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({ "apiVersion", "kind", "metadata", "spec", "status" })
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class VolumeClaimTemplate implements io.fabric8.kubernetes.api.model.KubernetesResource {

    /**
     * APIVersion defines the versioned schema of this representation of an object. Servers should convert recognized schemas to
     * the latest internal value, and may reject unrecognized values. More info:
     * https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#resources
     */
    @com.fasterxml.jackson.annotation.JsonProperty("apiVersion")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("APIVersion defines the versioned schema of this representation of an object. Servers should convert recognized schemas to the latest internal value, and may reject unrecognized values. More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#resources")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private String apiVersion;

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    /**
     * Kind is a string value representing the REST resource this object represents. Servers may infer this from the endpoint
     * the client submits requests to. Cannot be updated. In CamelCase. More info:
     * https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#types-kinds
     */
    @com.fasterxml.jackson.annotation.JsonProperty("kind")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Kind is a string value representing the REST resource this object represents. Servers may infer this from the endpoint the client submits requests to. Cannot be updated. In CamelCase. More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#types-kinds")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private String kind;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    /**
     * EmbeddedMetadata contains metadata relevant to an EmbeddedResource.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("metadata")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("EmbeddedMetadata contains metadata relevant to an EmbeddedResource.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.storage.alertmanager.volumeclaimtemplate.Metadata metadata;

    public com.redhat.observability.v1.observabilityspec.storage.alertmanager.volumeclaimtemplate.Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(
        com.redhat.observability.v1.observabilityspec.storage.alertmanager.volumeclaimtemplate.Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Spec defines the desired characteristics of a volume requested by a pod author. More info:
     * https://kubernetes.io/docs/concepts/storage/persistent-volumes#persistentvolumeclaims
     */
    @com.fasterxml.jackson.annotation.JsonProperty("spec")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Spec defines the desired characteristics of a volume requested by a pod author. More info: https://kubernetes.io/docs/concepts/storage/persistent-volumes#persistentvolumeclaims")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.storage.alertmanager.volumeclaimtemplate.Spec spec;

    public com.redhat.observability.v1.observabilityspec.storage.alertmanager.volumeclaimtemplate.Spec getSpec() {
        return spec;
    }

    public void setSpec(com.redhat.observability.v1.observabilityspec.storage.alertmanager.volumeclaimtemplate.Spec spec) {
        this.spec = spec;
    }

    /**
     * Status represents the current information/status of a persistent volume claim. Read-only. More info:
     * https://kubernetes.io/docs/concepts/storage/persistent-volumes#persistentvolumeclaims
     */
    @com.fasterxml.jackson.annotation.JsonProperty("status")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Status represents the current information/status of a persistent volume claim. Read-only. More info: https://kubernetes.io/docs/concepts/storage/persistent-volumes#persistentvolumeclaims")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.storage.alertmanager.volumeclaimtemplate.Status status;

    public com.redhat.observability.v1.observabilityspec.storage.alertmanager.volumeclaimtemplate.Status getStatus() {
        return status;
    }

    public void setStatus(
        com.redhat.observability.v1.observabilityspec.storage.alertmanager.volumeclaimtemplate.Status status) {
        this.status = status;
    }
}
