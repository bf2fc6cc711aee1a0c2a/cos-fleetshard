package org.bf2.cos.fleetshard.operator.debezium.model;

import java.util.Map;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ServiceAccountSpec;
import org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandConfiguration;

interface KafkaConnectConverter {

    String getConverterClass();

    Map<String, String> getAdditionalConfig(ManagedConnector config, ServiceAccountSpec serviceAccountSpec,
        DebeziumOperandConfiguration configuration);
}
