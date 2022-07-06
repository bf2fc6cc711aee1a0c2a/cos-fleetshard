package com.redhat.observability.v1.observabilityspec;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({ "alertmanager", "prometheus" })
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class Storage implements io.fabric8.kubernetes.api.model.KubernetesResource {

    /**
     * StorageSpec defines the configured storage for a group Prometheus servers. If neither `emptyDir` nor
     * `volumeClaimTemplate` is specified, then by default an
     * [EmptyDir](https://kubernetes.io/docs/concepts/storage/volumes/#emptydir) will be used.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("alertmanager")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("StorageSpec defines the configured storage for a group Prometheus servers. If neither `emptyDir` nor `volumeClaimTemplate` is specified, then by default an [EmptyDir](https://kubernetes.io/docs/concepts/storage/volumes/#emptydir) will be used.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.storage.Alertmanager alertmanager;

    public com.redhat.observability.v1.observabilityspec.storage.Alertmanager getAlertmanager() {
        return alertmanager;
    }

    public void setAlertmanager(com.redhat.observability.v1.observabilityspec.storage.Alertmanager alertmanager) {
        this.alertmanager = alertmanager;
    }

    /**
     * StorageSpec defines the configured storage for a group Prometheus servers. If neither `emptyDir` nor
     * `volumeClaimTemplate` is specified, then by default an
     * [EmptyDir](https://kubernetes.io/docs/concepts/storage/volumes/#emptydir) will be used.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("prometheus")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("StorageSpec defines the configured storage for a group Prometheus servers. If neither `emptyDir` nor `volumeClaimTemplate` is specified, then by default an [EmptyDir](https://kubernetes.io/docs/concepts/storage/volumes/#emptydir) will be used.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.storage.Prometheus prometheus;

    public com.redhat.observability.v1.observabilityspec.storage.Prometheus getPrometheus() {
        return prometheus;
    }

    public void setPrometheus(com.redhat.observability.v1.observabilityspec.storage.Prometheus prometheus) {
        this.prometheus = prometheus;
    }
}
