package org.bf2.cos.fleetshard.operator.operand;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import org.bf2.cos.fleet.manager.model.KafkaConnectionSettings;
import org.bf2.cos.fleetshard.api.KafkaSpec;
import org.bf2.cos.fleetshard.api.KafkaSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpec;

import static org.bf2.cos.fleetshard.support.resources.Secrets.SECRET_ENTRY_CONNECTOR;
import static org.bf2.cos.fleetshard.support.resources.Secrets.SECRET_ENTRY_KAFKA;
import static org.bf2.cos.fleetshard.support.resources.Secrets.SECRET_ENTRY_META;
import static org.bf2.cos.fleetshard.support.resources.Secrets.get;

public abstract class AbstractOperandController<M, S> implements OperandController {
    private final Class<M> metadataType;
    private final Class<S> connectorSpecType;

    public AbstractOperandController(Class<M> metadataType, Class<S> connectorSpecType) {
        this.metadataType = metadataType;
        this.connectorSpecType = connectorSpecType;
    }

    @Override
    public List<HasMetadata> reify(ManagedConnectorSpec connector, Secret secret) {
        final KafkaConnectionSettings kafkaSettings = get(
            secret,
            SECRET_ENTRY_KAFKA,
            KafkaConnectionSettings.class);

        return doReify(
            connector,
            get(secret, SECRET_ENTRY_META, metadataType),
            get(secret, SECRET_ENTRY_CONNECTOR, connectorSpecType),
            new KafkaSpecBuilder()
                .withClientId(kafkaSettings.getClientId())
                .withClientSecret(kafkaSettings.getClientSecret())
                .withBootstrapServers(kafkaSettings.getBootstrapServer())
                .build());
    }

    protected abstract List<HasMetadata> doReify(
        ManagedConnectorSpec connector,
        M shardMetadata,
        S connectorSpec,
        KafkaSpec kafkaSpec);
}
