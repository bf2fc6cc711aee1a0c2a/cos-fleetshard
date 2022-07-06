package com.redhat.observability.v1.observabilityspec.storage.prometheus.volumeclaimtemplate;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({ "accessModes", "capacity", "conditions", "phase" })
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class Status implements io.fabric8.kubernetes.api.model.KubernetesResource {

    /**
     * AccessModes contains the actual access modes the volume backing the PVC has. More info:
     * https://kubernetes.io/docs/concepts/storage/persistent-volumes#access-modes-1
     */
    @com.fasterxml.jackson.annotation.JsonProperty("accessModes")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("AccessModes contains the actual access modes the volume backing the PVC has. More info: https://kubernetes.io/docs/concepts/storage/persistent-volumes#access-modes-1")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private java.util.List<String> accessModes;

    public java.util.List<String> getAccessModes() {
        return accessModes;
    }

    public void setAccessModes(java.util.List<String> accessModes) {
        this.accessModes = accessModes;
    }

    /**
     * Represents the actual resources of the underlying volume.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("capacity")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Represents the actual resources of the underlying volume.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private java.util.Map<String, io.fabric8.kubernetes.api.model.IntOrString> capacity;

    public java.util.Map<String, io.fabric8.kubernetes.api.model.IntOrString> getCapacity() {
        return capacity;
    }

    public void setCapacity(java.util.Map<String, io.fabric8.kubernetes.api.model.IntOrString> capacity) {
        this.capacity = capacity;
    }

    /**
     * Current Condition of persistent volume claim. If underlying persistent volume is being resized then the Condition will be
     * set to 'ResizeStarted'.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("conditions")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Current Condition of persistent volume claim. If underlying persistent volume is being resized then the Condition will be set to 'ResizeStarted'.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private java.util.List<com.redhat.observability.v1.observabilityspec.storage.prometheus.volumeclaimtemplate.status.Conditions> conditions;

    public java.util.List<com.redhat.observability.v1.observabilityspec.storage.prometheus.volumeclaimtemplate.status.Conditions> getConditions() {
        return conditions;
    }

    public void setConditions(
        java.util.List<com.redhat.observability.v1.observabilityspec.storage.prometheus.volumeclaimtemplate.status.Conditions> conditions) {
        this.conditions = conditions;
    }

    /**
     * Phase represents the current phase of PersistentVolumeClaim.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("phase")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Phase represents the current phase of PersistentVolumeClaim.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private String phase;

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }
}
