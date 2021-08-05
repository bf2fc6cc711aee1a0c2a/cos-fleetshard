package org.bf2.cos.fleetshard.operator.debezium;

public final class DebeziumConstants {
    public static final String OPERATOR_TYPE = "debezium-connector-operator";

    public static final String DELETION_MODE_ANNOTATION = "cos.bf2.org/resource.deletion.mode";
    public static final String DELETION_MODE_CONNECTOR = "connector";
    public static final String DELETION_MODE_DEPLOYMENT = "deployment";
    public static final String EXTERNAL_CONFIG_DIRECTORY = "connector-configuration";
    public static final String EXTERNAL_CONFIG_FILE = "debezium-connector.properties";
    public static final String KAFKA_PASSWORD_SECRET_KEY = "_kafka.client.secret";

    private DebeziumConstants() {
    }

}
