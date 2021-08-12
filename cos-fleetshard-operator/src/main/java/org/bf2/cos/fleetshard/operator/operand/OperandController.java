package org.bf2.cos.fleetshard.operator.operand;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpec;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;

public interface OperandController {
    List<ResourceDefinitionContext> getResourceTypes();

    List<HasMetadata> reify(ManagedConnectorSpec connector, Secret secret);

    void status(ManagedConnectorStatus connector);
}
