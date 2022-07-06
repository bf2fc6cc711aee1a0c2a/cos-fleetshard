package com.redhat.observability.v1.observabilityspec.storage.prometheus;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({ "medium", "sizeLimit" })
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class EmptyDir implements io.fabric8.kubernetes.api.model.KubernetesResource {

    /**
     * What type of storage medium should back this directory. The default is "" which means to use the node's default medium.
     * Must be an empty string (default) or Memory. More info: https://kubernetes.io/docs/concepts/storage/volumes#emptydir
     */
    @com.fasterxml.jackson.annotation.JsonProperty("medium")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("What type of storage medium should back this directory. The default is \"\" which means to use the node's default medium. Must be an empty string (default) or Memory. More info: https://kubernetes.io/docs/concepts/storage/volumes#emptydir")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private String medium;

    public String getMedium() {
        return medium;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }

    /**
     * Total amount of local storage required for this EmptyDir volume. The size limit is also applicable for memory medium. The
     * maximum usage on memory medium EmptyDir would be the minimum value between the SizeLimit specified here and the sum of
     * memory limits of all containers in a pod. The default is nil which means that the limit is undefined. More info:
     * http://kubernetes.io/docs/user-guide/volumes#emptydir
     */
    @com.fasterxml.jackson.annotation.JsonProperty("sizeLimit")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Total amount of local storage required for this EmptyDir volume. The size limit is also applicable for memory medium. The maximum usage on memory medium EmptyDir would be the minimum value between the SizeLimit specified here and the sum of memory limits of all containers in a pod. The default is nil which means that the limit is undefined. More info: http://kubernetes.io/docs/user-guide/volumes#emptydir")
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    private io.fabric8.kubernetes.api.model.IntOrString sizeLimit;

    public io.fabric8.kubernetes.api.model.IntOrString getSizeLimit() {
        return sizeLimit;
    }

    public void setSizeLimit(io.fabric8.kubernetes.api.model.IntOrString sizeLimit) {
        this.sizeLimit = sizeLimit;
    }
}
