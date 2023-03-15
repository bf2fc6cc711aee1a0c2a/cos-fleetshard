package org.bf2.cos.fleetshard.operator.debezium;

import java.util.Map;

import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.Quantity;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "cos.operator.debezium")
public interface DebeziumOperandConfiguration {

    @WithDefault(DebeziumConstants.DEFAULT_IMAGE_PULL_SECRET_NAME)
    LocalObjectReference imagePullSecretsName();

    @WithDefault(DebeziumConstants.DEFAULT_APICURIO_AUTH_SERVICE_URL)
    String apicurioAuthServiceUrl();

    @WithDefault(DebeziumConstants.DEFAULT_APICURIO_AUTH_REALM)
    String apicurioAuthRealm();

    KafkaConnect kafkaConnect();

    KafkaConnector kafkaConnector();

    interface KafkaConnect {
        Map<String, String> config();

        Offset offset();

        interface Offset {
            @WithDefault("50Mi")
            Quantity storage();
        }
    }

    interface KafkaConnector {
        Map<String, String> config();
    }
}
