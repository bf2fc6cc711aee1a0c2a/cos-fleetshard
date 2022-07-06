package com.redhat.observability.v1.observabilityspec.storage;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({ "disableMountSubPath", "emptyDir", "volumeClaimTemplate" })
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class Prometheus implements io.fabric8.kubernetes.api.model.KubernetesResource {

    /**
     * Deprecated: subPath usage will be disabled by default in a future release, this option will become unnecessary.
     * DisableMountSubPath allows to remove any subPath usage in volume mounts.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("disableMountSubPath")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Deprecated: subPath usage will be disabled by default in a future release, this option will become unnecessary. DisableMountSubPath allows to remove any subPath usage in volume mounts.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private Boolean disableMountSubPath;

    public Boolean getDisableMountSubPath() {
        return disableMountSubPath;
    }

    public void setDisableMountSubPath(Boolean disableMountSubPath) {
        this.disableMountSubPath = disableMountSubPath;
    }

    /**
     * EmptyDirVolumeSource to be used by the Prometheus StatefulSets. If specified, used in place of any volumeClaimTemplate.
     * More info: https://kubernetes.io/docs/concepts/storage/volumes/#emptydir
     */
    @com.fasterxml.jackson.annotation.JsonProperty("emptyDir")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("EmptyDirVolumeSource to be used by the Prometheus StatefulSets. If specified, used in place of any volumeClaimTemplate. More info: https://kubernetes.io/docs/concepts/storage/volumes/#emptydir")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.storage.prometheus.EmptyDir emptyDir;

    public com.redhat.observability.v1.observabilityspec.storage.prometheus.EmptyDir getEmptyDir() {
        return emptyDir;
    }

    public void setEmptyDir(com.redhat.observability.v1.observabilityspec.storage.prometheus.EmptyDir emptyDir) {
        this.emptyDir = emptyDir;
    }

    /**
     * A PVC spec to be used by the Prometheus StatefulSets.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("volumeClaimTemplate")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A PVC spec to be used by the Prometheus StatefulSets.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.storage.prometheus.VolumeClaimTemplate volumeClaimTemplate;

    public com.redhat.observability.v1.observabilityspec.storage.prometheus.VolumeClaimTemplate getVolumeClaimTemplate() {
        return volumeClaimTemplate;
    }

    public void setVolumeClaimTemplate(
        com.redhat.observability.v1.observabilityspec.storage.prometheus.VolumeClaimTemplate volumeClaimTemplate) {
        this.volumeClaimTemplate = volumeClaimTemplate;
    }
}
