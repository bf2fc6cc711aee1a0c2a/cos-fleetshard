package com.redhat.observability.v1;

@io.fabric8.kubernetes.model.annotation.Version(value = "v1", storage = true, served = true)
@io.fabric8.kubernetes.model.annotation.Group("observability.redhat.com")
public class Observability extends
    io.fabric8.kubernetes.client.CustomResource<com.redhat.observability.v1.ObservabilitySpec, com.redhat.observability.v1.ObservabilityStatus>
    implements io.fabric8.kubernetes.api.model.Namespaced {
}
