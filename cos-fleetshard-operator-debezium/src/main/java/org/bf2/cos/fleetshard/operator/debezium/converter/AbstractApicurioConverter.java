package org.bf2.cos.fleetshard.operator.debezium.converter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.SchemaRegistrySpec;
import org.bf2.cos.fleetshard.api.ServiceAccountSpec;
import org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandConfiguration;

public abstract class AbstractApicurioConverter implements KafkaConnectConverter {

    @Override
    public Map<String, String> getAdditionalConfig(
        ManagedConnector config,
        ServiceAccountSpec serviceAccountSpec,
        DebeziumOperandConfiguration configuration) {

        Map<String, String> additionalConfig = new HashMap<>();
        additionalConfig.put("apicurio.auth.service.url", configuration.apicurioAuthServiceUrl());
        additionalConfig.put("apicurio.auth.realm", configuration.apicurioAuthRealm());

        SchemaRegistrySpec schemaRegistrySpec = config.getSpec().getDeployment().getSchemaRegistry();

        if (null == schemaRegistrySpec || null == schemaRegistrySpec.getUrl() || schemaRegistrySpec.getUrl().isBlank()) {
            throw new RuntimeException("Can't create a schema-based connector without providing a valid 'schema_registry'");
        }

        String schemaRegistryURL = schemaRegistrySpec.getUrl();
        String saSecret = new String(Base64.getDecoder().decode(serviceAccountSpec.getClientSecret()), StandardCharsets.UTF_8);

        additionalConfig.put("apicurio.registry.url", schemaRegistryURL);
        additionalConfig.put("apicurio.auth.client.id", serviceAccountSpec.getClientId());
        additionalConfig.put("apicurio.auth.client.secret", saSecret);
        additionalConfig.put("apicurio.registry.auto-register", "true");
        additionalConfig.put("apicurio.registry.find-latest", "true");

        return additionalConfig;
    }
}
