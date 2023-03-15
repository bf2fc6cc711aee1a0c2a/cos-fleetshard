package org.bf2.cos.fleetshard.operator.operand;

import java.util.List;
import java.util.Map;

import org.bf2.cos.fleetshard.api.ConnectorStatusSpec;
import org.bf2.cos.fleetshard.api.ManagedConnector;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

public interface OperandController {
    /**
     * @return a list of {@link ResourceDefinitionContext} describing the Kubernetes types the connector generates.
     */
    List<ResourceDefinitionContext> getResourceTypes();

    /**
     * Event sources
     */
    Map<String, EventSource> getEventSources();

    /**
     * Materialize the connector.
     *
     * @param connector the connector descriptor.
     * @param secret    the secret holding the connector specific data.
     * @param configMap the config map used for connector ad hoc configurations.
     */
    List<HasMetadata> reify(ManagedConnector connector, Secret secret, ConfigMap configMap);

    /**
     * Extract the status of a connector.
     *
     * @param  connector the connector descriptor.
     * @return           an optional {@link ConnectorStatusSpec}.
     */
    void status(ManagedConnector connector);

    /**
     * Stop the connector.
     *
     * @param  connector the connector to stop.
     * @return           true if the connector has been stopped, false otherwise.
     */
    boolean stop(ManagedConnector connector);

    /**
     * Delete the connector.
     *
     * @param  connector the connector to delete.
     * @return           true if the connector has been deleted, false otherwise.
     */
    boolean delete(ManagedConnector connector);
}
