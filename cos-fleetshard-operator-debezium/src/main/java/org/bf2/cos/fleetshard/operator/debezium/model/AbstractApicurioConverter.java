package org.bf2.cos.fleetshard.operator.debezium.model;

import java.util.HashMap;
import java.util.Map;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.SchemaRegistrySpec;
import org.bf2.cos.fleetshard.api.ServiceAccountSpec;
import org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants;

public abstract class AbstractApicurioConverter implements KafkaConnectConverter {

    public static final String APICURIO_AUTH_SERVICE_URL = "https://identity.api.openshift.com/auth";

    @Override
    public Map<String, String> getAdditionalConfig(ManagedConnector config, ServiceAccountSpec serviceAccountSpec) {
        Map<String, String> additionalConfig = new HashMap<>();
        additionalConfig.put("apicurio.auth.service.url", APICURIO_AUTH_SERVICE_URL);
        additionalConfig.put("apicurio.auth.realm", "rhoas");

        SchemaRegistrySpec schemaRegistrySpec = config.getSpec().getDeployment().getSchemaRegistry();
        if (null == schemaRegistrySpec || null == schemaRegistrySpec.getUrl() || schemaRegistrySpec.getUrl().isBlank()) {
            throw new RuntimeException("Can't create a schema-based connector without providing a valid 'schema_registry'");
        }
        String schemaRegistryURL = schemaRegistrySpec.getUrl();

        additionalConfig.put("apicurio.registry.url", schemaRegistryURL);
        additionalConfig.put("apicurio.auth.client.id", serviceAccountSpec.getClientId());
        additionalConfig.put("apicurio.auth.client.secret",
            "${dir:/opt/kafka/external-configuration/" + DebeziumConstants.EXTERNAL_CONFIG_DIRECTORY + ":"
                + DebeziumConstants.KAFKA_CLIENT_SECRET_KEY + "}");
        additionalConfig.put("apicurio.registry.auto-register", "true");
        additionalConfig.put("apicurio.registry.find-latest", "true");
        return additionalConfig;
    }
}
