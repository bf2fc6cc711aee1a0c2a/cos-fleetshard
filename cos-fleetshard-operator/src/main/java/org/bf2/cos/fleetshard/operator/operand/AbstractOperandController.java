package org.bf2.cos.fleetshard.operator.operand;

import java.util.List;

import org.bf2.cos.fleet.manager.model.ServiceAccount;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ServiceAccountSpec;
import org.bf2.cos.fleetshard.api.ServiceAccountSpecBuilder;
import org.bf2.cos.fleetshard.operator.connector.ConnectorConfiguration;
import org.bf2.cos.fleetshard.operator.connector.IncompleteConnectorSpecException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

import static org.bf2.cos.fleetshard.support.resources.Secrets.SECRET_ENTRY_CONNECTOR;
import static org.bf2.cos.fleetshard.support.resources.Secrets.SECRET_ENTRY_META;
import static org.bf2.cos.fleetshard.support.resources.Secrets.SECRET_ENTRY_SERVICE_ACCOUNT;
import static org.bf2.cos.fleetshard.support.resources.Secrets.extract;

public abstract class AbstractOperandController<M, S, D> implements OperandController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOperandController.class);
    private final KubernetesClient kubernetesClient;
    private final Class<M> metadataType;
    private final Class<S> connectorSpecType;
    private Class<D> dataShapeType;

    public AbstractOperandController(KubernetesClient kubernetesClient, Class<M> metadataType, Class<S> connectorSpecType,
        Class<D> dataShapeType) {
        this.kubernetesClient = kubernetesClient;
        this.metadataType = metadataType;
        this.connectorSpecType = connectorSpecType;
        this.dataShapeType = dataShapeType;
    }

    protected KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }

    @Override
    public List<HasMetadata> reify(ManagedConnector connector, Secret secret) {
        LOGGER.debug("Reifying connector: {} and secret.metadata: {}", connector, secret.getMetadata());
        final ServiceAccount serviceAccountSettings = extract(
            secret,
            SECRET_ENTRY_SERVICE_ACCOUNT,
            ServiceAccount.class);
        LOGGER.debug("Extracted serviceAccount {}",
            serviceAccountSettings == null ? "is null" : "with clientId: " + serviceAccountSettings.getClientId());

        ServiceAccountSpec sas = serviceAccountSettings == null
            ? new ServiceAccountSpecBuilder().build()
            : new ServiceAccountSpecBuilder()
                .withClientId(serviceAccountSettings.getClientId())
                .withClientSecret(serviceAccountSettings.getClientSecret())
                .build();

        ConnectorConfiguration<S, D> connectorConfig;
        try {
            connectorConfig = new ConnectorConfiguration<>(
                extract(secret, SECRET_ENTRY_CONNECTOR, ObjectNode.class),
                connectorSpecType,
                dataShapeType);
        } catch (IncompleteConnectorSpecException e) {
            throw new RuntimeException("Incomplete connectorSpec for connector \""
                + connector.getSpec().getConnectorId() + "@" + connector.getSpec().getDeploymentId()
                + "#" + connector.getSpec().getDeployment().getDeploymentResourceVersion()
                + "\": " + e.getLocalizedMessage(),
                e);
        }

        return doReify(
            connector,
            extract(secret, SECRET_ENTRY_META, metadataType),
            connectorConfig,
            sas);
    }

    protected abstract List<HasMetadata> doReify(
        ManagedConnector connector,
        M shardMetadata,
        ConnectorConfiguration<S, D> connectorSpec,
        ServiceAccountSpec serviceAccountSpec);
}
