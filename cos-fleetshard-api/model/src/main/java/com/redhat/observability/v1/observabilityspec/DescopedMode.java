package com.redhat.observability.v1.observabilityspec;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({ "enabled", "prometheusOperatorNamespace" })
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class DescopedMode implements io.fabric8.kubernetes.api.model.KubernetesResource {

    @com.fasterxml.jackson.annotation.JsonProperty("enabled")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private Boolean enabled;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("prometheusOperatorNamespace")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private String prometheusOperatorNamespace;

    public String getPrometheusOperatorNamespace() {
        return prometheusOperatorNamespace;
    }

    public void setPrometheusOperatorNamespace(String prometheusOperatorNamespace) {
        this.prometheusOperatorNamespace = prometheusOperatorNamespace;
    }
}
