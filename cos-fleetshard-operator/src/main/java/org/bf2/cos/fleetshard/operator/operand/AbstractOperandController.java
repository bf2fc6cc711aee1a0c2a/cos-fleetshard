package org.bf2.cos.fleetshard.operator.operand;

import java.util.List;

import org.bf2.cos.fleet.manager.model.ServiceAccount;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ServiceAccountSpec;
import org.bf2.cos.fleetshard.api.ServiceAccountSpecBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

import static org.bf2.cos.fleetshard.support.resources.Secrets.SECRET_ENTRY_CONNECTOR;
import static org.bf2.cos.fleetshard.support.resources.Secrets.SECRET_ENTRY_META;
import static org.bf2.cos.fleetshard.support.resources.Secrets.SECRET_ENTRY_SERVICE_ACCOUNT;
import static org.bf2.cos.fleetshard.support.resources.Secrets.extract;

public abstract class AbstractOperandController<M, S> implements OperandController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOperandController.class);
    private final KubernetesClient kubernetesClient;
    private final Class<M> metadataType;
    private final Class<S> connectorSpecType;

    public AbstractOperandController(KubernetesClient kubernetesClient, Class<M> metadataType, Class<S> connectorSpecType) {
        this.kubernetesClient = kubernetesClient;
        this.metadataType = metadataType;
        this.connectorSpecType = connectorSpecType;
    }

    protected KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }

    @Override
    public List<HasMetadata> reify(ManagedConnector connector, Secret secret) {
        LOGGER.debug("Reifying connector: {} and secret: {}", connector, secret);
        final ServiceAccount serviceAccountSettings = extract(
            secret,
            SECRET_ENTRY_SERVICE_ACCOUNT,
            ServiceAccount.class);
        LOGGER.debug("Extracted serviceAccount: {}", serviceAccountSettings);

        return doReify(
            connector,
            extract(secret, SECRET_ENTRY_META, metadataType),
            extract(secret, SECRET_ENTRY_CONNECTOR, connectorSpecType),
            new ServiceAccountSpecBuilder()
                .withClientId(serviceAccountSettings.getClientId())
                .withClientSecret(serviceAccountSettings.getClientSecret())
                .build());
    }

    protected abstract List<HasMetadata> doReify(
        ManagedConnector connector,
        M shardMetadata,
        S connectorSpec,
        ServiceAccountSpec serviceAccountSpec);
}
