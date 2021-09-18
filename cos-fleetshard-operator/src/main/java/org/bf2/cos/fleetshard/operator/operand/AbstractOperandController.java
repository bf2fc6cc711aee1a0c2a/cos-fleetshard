package org.bf2.cos.fleetshard.operator.operand;

import java.util.List;

import org.bf2.cos.fleet.manager.model.KafkaConnectionSettings;
import org.bf2.cos.fleetshard.api.KafkaSpec;
import org.bf2.cos.fleetshard.api.KafkaSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.support.resources.UnstructuredClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

import static org.bf2.cos.fleetshard.support.resources.Secrets.SECRET_ENTRY_CONNECTOR;
import static org.bf2.cos.fleetshard.support.resources.Secrets.SECRET_ENTRY_KAFKA;
import static org.bf2.cos.fleetshard.support.resources.Secrets.SECRET_ENTRY_META;
import static org.bf2.cos.fleetshard.support.resources.Secrets.extract;

public abstract class AbstractOperandController<M, S> implements OperandController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOperandController.class);

    private final UnstructuredClient uc;
    private final Class<M> metadataType;
    private final Class<S> connectorSpecType;

    public AbstractOperandController(UnstructuredClient uc, Class<M> metadataType, Class<S> connectorSpecType) {
        this.uc = uc;
        this.metadataType = metadataType;
        this.connectorSpecType = connectorSpecType;
    }

    protected UnstructuredClient getUnstructuredClient() {
        return uc;
    }

    protected KubernetesClient getKubernetesClient() {
        return uc.getKubernetesClient();
    }

    @Override
    public List<HasMetadata> reify(ManagedConnector connector, Secret secret) {
        final KafkaConnectionSettings kafkaSettings = extract(
            secret,
            SECRET_ENTRY_KAFKA,
            KafkaConnectionSettings.class);

        return doReify(
            connector,
            extract(secret, SECRET_ENTRY_META, metadataType),
            extract(secret, SECRET_ENTRY_CONNECTOR, connectorSpecType),
            new KafkaSpecBuilder()
                .withClientId(kafkaSettings.getClientId())
                .withClientSecret(kafkaSettings.getClientSecret())
                .withBootstrapServers(kafkaSettings.getBootstrapServer())
                .build());
    }

    protected abstract List<HasMetadata> doReify(
        ManagedConnector connector,
        M shardMetadata,
        S connectorSpec,
        KafkaSpec kafkaSpec);
}
