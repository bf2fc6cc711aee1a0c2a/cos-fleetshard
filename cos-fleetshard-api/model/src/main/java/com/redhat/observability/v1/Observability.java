package com.redhat.observability.v1;

import io.fabric8.kubernetes.client.CustomResource;

@io.fabric8.kubernetes.model.annotation.Version(value = "v1", storage = true, served = true)
@io.fabric8.kubernetes.model.annotation.Group("observability.redhat.com")
public class Observability extends CustomResource<ObservabilitySpec, ObservabilityStatus>
    implements io.fabric8.kubernetes.api.model.Namespaced {
}
