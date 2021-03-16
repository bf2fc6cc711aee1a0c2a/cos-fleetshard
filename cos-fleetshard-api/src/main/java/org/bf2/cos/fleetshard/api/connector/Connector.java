package org.bf2.cos.fleetshard.api.connector;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespaced;

@JsonInclude(JsonInclude.Include.NON_NULL)
public interface Connector<Spec extends ConnectorSpec, Status extends ConnectorStatus> extends HasMetadata, Namespaced {
    Spec getSpec();

    void setSpec(Spec spec);

    Status getStatus();

    void setStatus(Status status);
}
