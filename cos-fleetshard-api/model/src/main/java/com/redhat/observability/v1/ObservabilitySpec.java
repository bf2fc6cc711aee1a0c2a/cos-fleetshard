package com.redhat.observability.v1;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({ "affinity", "alertManagerDefaultName", "clusterId",
    "configurationSelector", "descopedMode", "grafanaDefaultName", "prometheusDefaultName", "resyncPeriod", "retention",
    "selfContained", "storage", "tolerations" })
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class ObservabilitySpec implements io.fabric8.kubernetes.api.model.KubernetesResource {

    /**
     * Affinity is a group of affinity scheduling rules.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("affinity")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Affinity is a group of affinity scheduling rules.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.Affinity affinity;

    public com.redhat.observability.v1.observabilityspec.Affinity getAffinity() {
        return affinity;
    }

    public void setAffinity(com.redhat.observability.v1.observabilityspec.Affinity affinity) {
        this.affinity = affinity;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("alertManagerDefaultName")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private String alertManagerDefaultName;

    public String getAlertManagerDefaultName() {
        return alertManagerDefaultName;
    }

    public void setAlertManagerDefaultName(String alertManagerDefaultName) {
        this.alertManagerDefaultName = alertManagerDefaultName;
    }

    /**
     * Cluster ID. If not provided, the operator tries to obtain it.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("clusterId")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Cluster ID. If not provided, the operator tries to obtain it.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private String clusterId;

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    /**
     * A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An
     * empty label selector matches all objects. A null label selector matches no objects.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("configurationSelector")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.ConfigurationSelector configurationSelector;

    public com.redhat.observability.v1.observabilityspec.ConfigurationSelector getConfigurationSelector() {
        return configurationSelector;
    }

    public void setConfigurationSelector(
        com.redhat.observability.v1.observabilityspec.ConfigurationSelector configurationSelector) {
        this.configurationSelector = configurationSelector;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("descopedMode")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.DescopedMode descopedMode;

    public com.redhat.observability.v1.observabilityspec.DescopedMode getDescopedMode() {
        return descopedMode;
    }

    public void setDescopedMode(com.redhat.observability.v1.observabilityspec.DescopedMode descopedMode) {
        this.descopedMode = descopedMode;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("grafanaDefaultName")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private String grafanaDefaultName;

    public String getGrafanaDefaultName() {
        return grafanaDefaultName;
    }

    public void setGrafanaDefaultName(String grafanaDefaultName) {
        this.grafanaDefaultName = grafanaDefaultName;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("prometheusDefaultName")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private String prometheusDefaultName;

    public String getPrometheusDefaultName() {
        return prometheusDefaultName;
    }

    public void setPrometheusDefaultName(String prometheusDefaultName) {
        this.prometheusDefaultName = prometheusDefaultName;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("resyncPeriod")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private String resyncPeriod;

    public String getResyncPeriod() {
        return resyncPeriod;
    }

    public void setResyncPeriod(String resyncPeriod) {
        this.resyncPeriod = resyncPeriod;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("retention")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private String retention;

    public String getRetention() {
        return retention;
    }

    public void setRetention(String retention) {
        this.retention = retention;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("selfContained")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.SelfContained selfContained;

    public com.redhat.observability.v1.observabilityspec.SelfContained getSelfContained() {
        return selfContained;
    }

    public void setSelfContained(com.redhat.observability.v1.observabilityspec.SelfContained selfContained) {
        this.selfContained = selfContained;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("storage")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.Storage storage;

    public com.redhat.observability.v1.observabilityspec.Storage getStorage() {
        return storage;
    }

    public void setStorage(com.redhat.observability.v1.observabilityspec.Storage storage) {
        this.storage = storage;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("tolerations")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private java.util.List<com.redhat.observability.v1.observabilityspec.Tolerations> tolerations;

    public java.util.List<com.redhat.observability.v1.observabilityspec.Tolerations> getTolerations() {
        return tolerations;
    }

    public void setTolerations(java.util.List<com.redhat.observability.v1.observabilityspec.Tolerations> tolerations) {
        this.tolerations = tolerations;
    }
}
