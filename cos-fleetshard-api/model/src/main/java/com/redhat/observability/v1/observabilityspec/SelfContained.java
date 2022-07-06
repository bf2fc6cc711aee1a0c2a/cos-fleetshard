package com.redhat.observability.v1.observabilityspec;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({ "alertManagerConfigSecret", "alertManagerResourceRequirement",
    "alertManagerVersion", "blackboxBearerTokenSecret", "disableBlackboxExporter", "disableDeadmansSnitch",
    "disableObservatorium", "disablePagerDuty", "disableRepoSync", "disableSmtp", "federatedMetrics",
    "grafanaDashboardLabelSelector", "grafanaOperatorResourceRequirement", "grafanaResourceRequirement", "overrideSelectors",
    "podMonitorLabelSelector", "podMonitorNamespaceSelector", "probeNamespaceSelector", "probeSelector",
    "prometheusOperatorResourceRequirement", "prometheusResourceRequirement", "prometheusVersion", "ruleLabelSelector",
    "ruleNamespaceSelector", "selfSignedCerts", "serviceMonitorLabelSelector", "serviceMonitorNamespaceSelector" })
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class SelfContained implements io.fabric8.kubernetes.api.model.KubernetesResource {

    @com.fasterxml.jackson.annotation.JsonProperty("alertManagerConfigSecret")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private String alertManagerConfigSecret;

    public String getAlertManagerConfigSecret() {
        return alertManagerConfigSecret;
    }

    public void setAlertManagerConfigSecret(String alertManagerConfigSecret) {
        this.alertManagerConfigSecret = alertManagerConfigSecret;
    }

    /**
     * ResourceRequirements describes the compute resource requirements.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("alertManagerResourceRequirement")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("ResourceRequirements describes the compute resource requirements.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.selfcontained.AlertManagerResourceRequirement alertManagerResourceRequirement;

    public com.redhat.observability.v1.observabilityspec.selfcontained.AlertManagerResourceRequirement getAlertManagerResourceRequirement() {
        return alertManagerResourceRequirement;
    }

    public void setAlertManagerResourceRequirement(
        com.redhat.observability.v1.observabilityspec.selfcontained.AlertManagerResourceRequirement alertManagerResourceRequirement) {
        this.alertManagerResourceRequirement = alertManagerResourceRequirement;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("alertManagerVersion")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private String alertManagerVersion;

    public String getAlertManagerVersion() {
        return alertManagerVersion;
    }

    public void setAlertManagerVersion(String alertManagerVersion) {
        this.alertManagerVersion = alertManagerVersion;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("blackboxBearerTokenSecret")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private String blackboxBearerTokenSecret;

    public String getBlackboxBearerTokenSecret() {
        return blackboxBearerTokenSecret;
    }

    public void setBlackboxBearerTokenSecret(String blackboxBearerTokenSecret) {
        this.blackboxBearerTokenSecret = blackboxBearerTokenSecret;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("disableBlackboxExporter")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private Boolean disableBlackboxExporter;

    public Boolean getDisableBlackboxExporter() {
        return disableBlackboxExporter;
    }

    public void setDisableBlackboxExporter(Boolean disableBlackboxExporter) {
        this.disableBlackboxExporter = disableBlackboxExporter;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("disableDeadmansSnitch")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private Boolean disableDeadmansSnitch;

    public Boolean getDisableDeadmansSnitch() {
        return disableDeadmansSnitch;
    }

    public void setDisableDeadmansSnitch(Boolean disableDeadmansSnitch) {
        this.disableDeadmansSnitch = disableDeadmansSnitch;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("disableObservatorium")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private Boolean disableObservatorium;

    public Boolean getDisableObservatorium() {
        return disableObservatorium;
    }

    public void setDisableObservatorium(Boolean disableObservatorium) {
        this.disableObservatorium = disableObservatorium;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("disablePagerDuty")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private Boolean disablePagerDuty;

    public Boolean getDisablePagerDuty() {
        return disablePagerDuty;
    }

    public void setDisablePagerDuty(Boolean disablePagerDuty) {
        this.disablePagerDuty = disablePagerDuty;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("disableRepoSync")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private Boolean disableRepoSync;

    public Boolean getDisableRepoSync() {
        return disableRepoSync;
    }

    public void setDisableRepoSync(Boolean disableRepoSync) {
        this.disableRepoSync = disableRepoSync;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("disableSmtp")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private Boolean disableSmtp;

    public Boolean getDisableSmtp() {
        return disableSmtp;
    }

    public void setDisableSmtp(Boolean disableSmtp) {
        this.disableSmtp = disableSmtp;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("federatedMetrics")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private java.util.List<String> federatedMetrics;

    public java.util.List<String> getFederatedMetrics() {
        return federatedMetrics;
    }

    public void setFederatedMetrics(java.util.List<String> federatedMetrics) {
        this.federatedMetrics = federatedMetrics;
    }

    /**
     * A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An
     * empty label selector matches all objects. A null label selector matches no objects.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("grafanaDashboardLabelSelector")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.selfcontained.GrafanaDashboardLabelSelector grafanaDashboardLabelSelector;

    public com.redhat.observability.v1.observabilityspec.selfcontained.GrafanaDashboardLabelSelector getGrafanaDashboardLabelSelector() {
        return grafanaDashboardLabelSelector;
    }

    public void setGrafanaDashboardLabelSelector(
        com.redhat.observability.v1.observabilityspec.selfcontained.GrafanaDashboardLabelSelector grafanaDashboardLabelSelector) {
        this.grafanaDashboardLabelSelector = grafanaDashboardLabelSelector;
    }

    /**
     * ResourceRequirements describes the compute resource requirements.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("grafanaOperatorResourceRequirement")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("ResourceRequirements describes the compute resource requirements.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.selfcontained.GrafanaOperatorResourceRequirement grafanaOperatorResourceRequirement;

    public com.redhat.observability.v1.observabilityspec.selfcontained.GrafanaOperatorResourceRequirement getGrafanaOperatorResourceRequirement() {
        return grafanaOperatorResourceRequirement;
    }

    public void setGrafanaOperatorResourceRequirement(
        com.redhat.observability.v1.observabilityspec.selfcontained.GrafanaOperatorResourceRequirement grafanaOperatorResourceRequirement) {
        this.grafanaOperatorResourceRequirement = grafanaOperatorResourceRequirement;
    }

    /**
     * ResourceRequirements describes the compute resource requirements.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("grafanaResourceRequirement")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("ResourceRequirements describes the compute resource requirements.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.selfcontained.GrafanaResourceRequirement grafanaResourceRequirement;

    public com.redhat.observability.v1.observabilityspec.selfcontained.GrafanaResourceRequirement getGrafanaResourceRequirement() {
        return grafanaResourceRequirement;
    }

    public void setGrafanaResourceRequirement(
        com.redhat.observability.v1.observabilityspec.selfcontained.GrafanaResourceRequirement grafanaResourceRequirement) {
        this.grafanaResourceRequirement = grafanaResourceRequirement;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("overrideSelectors")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private Boolean overrideSelectors;

    public Boolean getOverrideSelectors() {
        return overrideSelectors;
    }

    public void setOverrideSelectors(Boolean overrideSelectors) {
        this.overrideSelectors = overrideSelectors;
    }

    /**
     * A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An
     * empty label selector matches all objects. A null label selector matches no objects.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("podMonitorLabelSelector")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.selfcontained.PodMonitorLabelSelector podMonitorLabelSelector;

    public com.redhat.observability.v1.observabilityspec.selfcontained.PodMonitorLabelSelector getPodMonitorLabelSelector() {
        return podMonitorLabelSelector;
    }

    public void setPodMonitorLabelSelector(
        com.redhat.observability.v1.observabilityspec.selfcontained.PodMonitorLabelSelector podMonitorLabelSelector) {
        this.podMonitorLabelSelector = podMonitorLabelSelector;
    }

    /**
     * A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An
     * empty label selector matches all objects. A null label selector matches no objects.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("podMonitorNamespaceSelector")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.selfcontained.PodMonitorNamespaceSelector podMonitorNamespaceSelector;

    public com.redhat.observability.v1.observabilityspec.selfcontained.PodMonitorNamespaceSelector getPodMonitorNamespaceSelector() {
        return podMonitorNamespaceSelector;
    }

    public void setPodMonitorNamespaceSelector(
        com.redhat.observability.v1.observabilityspec.selfcontained.PodMonitorNamespaceSelector podMonitorNamespaceSelector) {
        this.podMonitorNamespaceSelector = podMonitorNamespaceSelector;
    }

    /**
     * A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An
     * empty label selector matches all objects. A null label selector matches no objects.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("probeNamespaceSelector")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.selfcontained.ProbeNamespaceSelector probeNamespaceSelector;

    public com.redhat.observability.v1.observabilityspec.selfcontained.ProbeNamespaceSelector getProbeNamespaceSelector() {
        return probeNamespaceSelector;
    }

    public void setProbeNamespaceSelector(
        com.redhat.observability.v1.observabilityspec.selfcontained.ProbeNamespaceSelector probeNamespaceSelector) {
        this.probeNamespaceSelector = probeNamespaceSelector;
    }

    /**
     * A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An
     * empty label selector matches all objects. A null label selector matches no objects.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("probeSelector")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.selfcontained.ProbeSelector probeSelector;

    public com.redhat.observability.v1.observabilityspec.selfcontained.ProbeSelector getProbeSelector() {
        return probeSelector;
    }

    public void setProbeSelector(com.redhat.observability.v1.observabilityspec.selfcontained.ProbeSelector probeSelector) {
        this.probeSelector = probeSelector;
    }

    /**
     * ResourceRequirements describes the compute resource requirements.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("prometheusOperatorResourceRequirement")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("ResourceRequirements describes the compute resource requirements.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.selfcontained.PrometheusOperatorResourceRequirement prometheusOperatorResourceRequirement;

    public com.redhat.observability.v1.observabilityspec.selfcontained.PrometheusOperatorResourceRequirement getPrometheusOperatorResourceRequirement() {
        return prometheusOperatorResourceRequirement;
    }

    public void setPrometheusOperatorResourceRequirement(
        com.redhat.observability.v1.observabilityspec.selfcontained.PrometheusOperatorResourceRequirement prometheusOperatorResourceRequirement) {
        this.prometheusOperatorResourceRequirement = prometheusOperatorResourceRequirement;
    }

    /**
     * ResourceRequirements describes the compute resource requirements.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("prometheusResourceRequirement")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("ResourceRequirements describes the compute resource requirements.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.selfcontained.PrometheusResourceRequirement prometheusResourceRequirement;

    public com.redhat.observability.v1.observabilityspec.selfcontained.PrometheusResourceRequirement getPrometheusResourceRequirement() {
        return prometheusResourceRequirement;
    }

    public void setPrometheusResourceRequirement(
        com.redhat.observability.v1.observabilityspec.selfcontained.PrometheusResourceRequirement prometheusResourceRequirement) {
        this.prometheusResourceRequirement = prometheusResourceRequirement;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("prometheusVersion")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private String prometheusVersion;

    public String getPrometheusVersion() {
        return prometheusVersion;
    }

    public void setPrometheusVersion(String prometheusVersion) {
        this.prometheusVersion = prometheusVersion;
    }

    /**
     * A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An
     * empty label selector matches all objects. A null label selector matches no objects.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("ruleLabelSelector")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.selfcontained.RuleLabelSelector ruleLabelSelector;

    public com.redhat.observability.v1.observabilityspec.selfcontained.RuleLabelSelector getRuleLabelSelector() {
        return ruleLabelSelector;
    }

    public void setRuleLabelSelector(
        com.redhat.observability.v1.observabilityspec.selfcontained.RuleLabelSelector ruleLabelSelector) {
        this.ruleLabelSelector = ruleLabelSelector;
    }

    /**
     * A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An
     * empty label selector matches all objects. A null label selector matches no objects.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("ruleNamespaceSelector")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.selfcontained.RuleNamespaceSelector ruleNamespaceSelector;

    public com.redhat.observability.v1.observabilityspec.selfcontained.RuleNamespaceSelector getRuleNamespaceSelector() {
        return ruleNamespaceSelector;
    }

    public void setRuleNamespaceSelector(
        com.redhat.observability.v1.observabilityspec.selfcontained.RuleNamespaceSelector ruleNamespaceSelector) {
        this.ruleNamespaceSelector = ruleNamespaceSelector;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("selfSignedCerts")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private Boolean selfSignedCerts;

    public Boolean getSelfSignedCerts() {
        return selfSignedCerts;
    }

    public void setSelfSignedCerts(Boolean selfSignedCerts) {
        this.selfSignedCerts = selfSignedCerts;
    }

    /**
     * A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An
     * empty label selector matches all objects. A null label selector matches no objects.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("serviceMonitorLabelSelector")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.selfcontained.ServiceMonitorLabelSelector serviceMonitorLabelSelector;

    public com.redhat.observability.v1.observabilityspec.selfcontained.ServiceMonitorLabelSelector getServiceMonitorLabelSelector() {
        return serviceMonitorLabelSelector;
    }

    public void setServiceMonitorLabelSelector(
        com.redhat.observability.v1.observabilityspec.selfcontained.ServiceMonitorLabelSelector serviceMonitorLabelSelector) {
        this.serviceMonitorLabelSelector = serviceMonitorLabelSelector;
    }

    /**
     * A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An
     * empty label selector matches all objects. A null label selector matches no objects.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("serviceMonitorNamespaceSelector")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.selfcontained.ServiceMonitorNamespaceSelector serviceMonitorNamespaceSelector;

    public com.redhat.observability.v1.observabilityspec.selfcontained.ServiceMonitorNamespaceSelector getServiceMonitorNamespaceSelector() {
        return serviceMonitorNamespaceSelector;
    }

    public void setServiceMonitorNamespaceSelector(
        com.redhat.observability.v1.observabilityspec.selfcontained.ServiceMonitorNamespaceSelector serviceMonitorNamespaceSelector) {
        this.serviceMonitorNamespaceSelector = serviceMonitorNamespaceSelector;
    }
}
