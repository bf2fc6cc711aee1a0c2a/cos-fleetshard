package com.redhat.observability.v1.observabilityspec.affinity.podaffinity;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({ "podAffinityTerm", "weight" })
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class PreferredDuringSchedulingIgnoredDuringExecution implements io.fabric8.kubernetes.api.model.KubernetesResource {

    /**
     * Required. A pod affinity term, associated with the corresponding weight.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("podAffinityTerm")
    @javax.validation.constraints.NotNull()
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Required. A pod affinity term, associated with the corresponding weight.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.affinity.podaffinity.preferredduringschedulingignoredduringexecution.PodAffinityTerm podAffinityTerm;

    public com.redhat.observability.v1.observabilityspec.affinity.podaffinity.preferredduringschedulingignoredduringexecution.PodAffinityTerm getPodAffinityTerm() {
        return podAffinityTerm;
    }

    public void setPodAffinityTerm(
        com.redhat.observability.v1.observabilityspec.affinity.podaffinity.preferredduringschedulingignoredduringexecution.PodAffinityTerm podAffinityTerm) {
        this.podAffinityTerm = podAffinityTerm;
    }

    /**
     * weight associated with matching the corresponding podAffinityTerm, in the range 1-100.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("weight")
    @javax.validation.constraints.NotNull()
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("weight associated with matching the corresponding podAffinityTerm, in the range 1-100.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private Integer weight;

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }
}
