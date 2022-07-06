package com.redhat.observability.v1.observabilityspec.affinity.nodeaffinity.preferredduringschedulingignoredduringexecution;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({ "matchExpressions", "matchFields" })
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class Preference implements io.fabric8.kubernetes.api.model.KubernetesResource {

    /**
     * A list of node selector requirements by node's labels.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("matchExpressions")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A list of node selector requirements by node's labels.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private java.util.List<com.redhat.observability.v1.observabilityspec.affinity.nodeaffinity.preferredduringschedulingignoredduringexecution.preference.MatchExpressions> matchExpressions;

    public java.util.List<com.redhat.observability.v1.observabilityspec.affinity.nodeaffinity.preferredduringschedulingignoredduringexecution.preference.MatchExpressions> getMatchExpressions() {
        return matchExpressions;
    }

    public void setMatchExpressions(
        java.util.List<com.redhat.observability.v1.observabilityspec.affinity.nodeaffinity.preferredduringschedulingignoredduringexecution.preference.MatchExpressions> matchExpressions) {
        this.matchExpressions = matchExpressions;
    }

    /**
     * A list of node selector requirements by node's fields.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("matchFields")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A list of node selector requirements by node's fields.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private java.util.List<com.redhat.observability.v1.observabilityspec.affinity.nodeaffinity.preferredduringschedulingignoredduringexecution.preference.MatchFields> matchFields;

    public java.util.List<com.redhat.observability.v1.observabilityspec.affinity.nodeaffinity.preferredduringschedulingignoredduringexecution.preference.MatchFields> getMatchFields() {
        return matchFields;
    }

    public void setMatchFields(
        java.util.List<com.redhat.observability.v1.observabilityspec.affinity.nodeaffinity.preferredduringschedulingignoredduringexecution.preference.MatchFields> matchFields) {
        this.matchFields = matchFields;
    }
}
