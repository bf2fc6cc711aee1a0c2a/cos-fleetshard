package com.redhat.observability.v1.observabilityspec.affinity.podantiaffinity;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({ "labelSelector", "namespaces", "topologyKey" })
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class RequiredDuringSchedulingIgnoredDuringExecution implements io.fabric8.kubernetes.api.model.KubernetesResource {

    /**
     * A label query over a set of resources, in this case pods.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("labelSelector")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A label query over a set of resources, in this case pods.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private com.redhat.observability.v1.observabilityspec.affinity.podantiaffinity.requiredduringschedulingignoredduringexecution.LabelSelector labelSelector;

    public com.redhat.observability.v1.observabilityspec.affinity.podantiaffinity.requiredduringschedulingignoredduringexecution.LabelSelector getLabelSelector() {
        return labelSelector;
    }

    public void setLabelSelector(
        com.redhat.observability.v1.observabilityspec.affinity.podantiaffinity.requiredduringschedulingignoredduringexecution.LabelSelector labelSelector) {
        this.labelSelector = labelSelector;
    }

    /**
     * namespaces specifies which namespaces the labelSelector applies to (matches against); null or empty list means "this
     * pod's namespace"
     */
    @com.fasterxml.jackson.annotation.JsonProperty("namespaces")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("namespaces specifies which namespaces the labelSelector applies to (matches against); null or empty list means \"this pod's namespace\"")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private java.util.List<String> namespaces;

    public java.util.List<String> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(java.util.List<String> namespaces) {
        this.namespaces = namespaces;
    }

    /**
     * This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in
     * the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey
     * matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("topologyKey")
    @javax.validation.constraints.NotNull()
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private String topologyKey;

    public String getTopologyKey() {
        return topologyKey;
    }

    public void setTopologyKey(String topologyKey) {
        this.topologyKey = topologyKey;
    }
}
