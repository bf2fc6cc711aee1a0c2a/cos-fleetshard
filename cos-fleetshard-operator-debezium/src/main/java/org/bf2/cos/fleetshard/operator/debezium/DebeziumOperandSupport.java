package org.bf2.cos.fleetshard.operator.debezium;

import java.util.Objects;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.strimzi.api.kafka.model.Constants;
import io.strimzi.api.kafka.model.KafkaConnect;
import io.strimzi.api.kafka.model.KafkaConnector;
import org.bf2.cos.fleetshard.api.ResourceRef;

public class DebeziumOperandSupport {
    public static final String AV_KAFKA_CONNECT = Constants.STRIMZI_GROUP + "/" + KafkaConnect.CONSUMED_VERSION;
    public static final String AV_KAFKA_CONNECTOR = Constants.STRIMZI_GROUP + "/" + KafkaConnector.CONSUMED_VERSION;

    public static boolean isSecret(HasMetadata ref) {
        return Objects.equals("v1", ref.getApiVersion())
            && Objects.equals("Secret", ref.getKind());
    }

    public static boolean isSecret(ResourceRef ref) {
        return Objects.equals("v1", ref.getApiVersion())
            && Objects.equals("Secret", ref.getKind());
    }

    public static boolean isKafkaConnect(HasMetadata ref) {
        return Objects.equals(AV_KAFKA_CONNECT, ref.getApiVersion())
            && Objects.equals(KafkaConnect.RESOURCE_KIND, ref.getKind());
    }

    public static boolean isKafkaConnect(ResourceRef ref) {
        return Objects.equals(AV_KAFKA_CONNECT, ref.getApiVersion())
            && Objects.equals(KafkaConnect.RESOURCE_KIND, ref.getKind());
    }

    public static boolean isKafkaConnector(HasMetadata ref) {
        return Objects.equals(AV_KAFKA_CONNECTOR, ref.getApiVersion())
            && Objects.equals(KafkaConnector.RESOURCE_KIND, ref.getKind());
    }

    public static boolean isKafkaConnector(ResourceRef ref) {
        return Objects.equals(AV_KAFKA_CONNECTOR, ref.getApiVersion())
            && Objects.equals(KafkaConnector.RESOURCE_KIND, ref.getKind());
    }
}
