package org.bf2.cos.fleetshard.operator.debezium;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.bf2.cos.fleetshard.support.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;

import io.fabric8.kubernetes.client.utils.Serialization;

public final class DebeziumConstants {
    public static final String OPERATOR_TYPE = "debezium-connector-operator";
    public static final String OPERATOR_RUNTIME = "kafka-connect";

    public static final String EXTERNAL_CONFIG_DIRECTORY = "connector-configuration";
    public static final String EXTERNAL_CONFIG_FILE = "debezium-connector.properties";
    public static final String KAFKA_CLIENT_SECRET_KEY = "_kafka.client.secret";

    public static final String STRIMZI_DOMAIN = "strimzi.io/";
    public static final String STRIMZI_IO_USE_CONNECTOR_RESOURCES = STRIMZI_DOMAIN + "use-connector-resources";

    public static final String DEFAULT_IMAGE_PULL_SECRET_NAME = "addon-pullsecret";

    public static final String DEFAULT_APICURIO_AUTH_SERVICE_URL = "https://identity.api.openshift.com/auth";
    public static final String DEFAULT_APICURIO_AUTH_REALM = "rhoas";

    public static final Map<String, String> DEFAULT_CONFIG_OPTIONS = CollectionUtils.mapOf(
        "request.timeout.ms", "20000",
        "retry.backoff.ms", "500",
        "consumer.request.timeout.ms", "20000",
        "consumer.retry.backoff.ms", "500",
        "producer.request.timeout.ms", "20000",
        "producer.retry.backoff.ms", "500",
        "producer.compression.type", "lz4",
        "config.storage.replication.factor", "-1",
        "offset.storage.replication.factor", "-1",
        "status.storage.replication.factor", "-1",
        "key.converter.schemas.enable", "true",
        "value.converter.schemas.enable", "true",
        "config.providers", "file,dir",
        "config.providers.file.class", "org.apache.kafka.common.config.provider.FileConfigProvider",
        "config.providers.dir.class", "org.apache.kafka.common.config.provider.DirectoryConfigProvider");

    public static final String KAFKA_HOME = "/opt/kafka";

    public static final String RHOC_HOME = "/etc/rhoc";
    public static final String RHOC_SECRETS_HOME = RHOC_HOME + "/secrets";
    public static final String RHOC_CONFIG_HOME = RHOC_HOME + "/config";
    public static final String RHOC_DATA_HOME = RHOC_HOME + "/data";;

    public static final String LOGGING_CONFIG_FILENAME = "logging.properties";
    public static final String LOGGING_CONFIG_PATH = RHOC_CONFIG_HOME + "/" + LOGGING_CONFIG_FILENAME;
    public static final String LOGGING_CONFIG_RESOURCE = "/kafka_connect_logging.properties";

    public static final String METRICS_CONFIG_RESOURCE = "/kafka_connect_metrics.yml";
    public static final String METRICS_CONFIG_FILENAME = "metrics-config.json";
    public static final String METRICS_CONFIG_PATH = RHOC_CONFIG_HOME + "/" + METRICS_CONFIG_FILENAME;

    public static final String KAFKA_CONNECT_METRICS_CONFIGMAP_NAME_SUFFIX = "-metrics";
    public static final String CLASS_NAME_POSTGRES_CONNECTOR = "io.debezium.connector.postgresql.PostgresConnector";
    public static final String CLASS_NAME_MONGODB_CONNECTOR = "io.debezium.connector.mongodb.MongoDbConnector";
    public static final String CLASS_NAME_MYSQL_CONNECTOR = "io.debezium.connector.mysql.MySqlConnector";
    public static final String CLASS_NAME_SQLSERVER_CONNECTOR = "io.debezium.connector.sqlserver.SqlServerConnector";
    public static final String CONFIG_OPTION_POSTGRES_PLUGIN_NAME = "plugin.name";
    public static final String CONFIG_OPTION_SQLSERVER_DATABASE_ENCRYPT = "database.encrypt";
    public static final String CONFIG_OPTION_SQLSERVER_DATABASE_TRUST_CERTIFICATE = "database.trustServerCertificate";
    public static final String PLUGIN_NAME_PGOUTPUT = "pgoutput";

    public static final String CONNECT_CONFIG_FILENAME = "connect.properties";
    public static final String CONNECT_CONFIG_PATH = RHOC_SECRETS_HOME + "/" + CONNECT_CONFIG_FILENAME;
    public static final String CONNECTOR_CONFIG_FILENAME = "connector.properties";
    public static final String CONNECTOR_CONFIG_PATH = RHOC_SECRETS_HOME + "/" + CONNECTOR_CONFIG_FILENAME;

    public static final String CONNECTOR_OFFSET_FILENAME = "connector.offsets";
    public static final String CONNECTOR_OFFSET_PATH = RHOC_DATA_HOME + "/" + CONNECTOR_OFFSET_FILENAME;

    public static final String METRICS_CONFIG;
    public static final Properties LOGGING_CONFIG;

    public static final String CONDITION_TYPE_KAFKA_CONNECT_READY = "KafkaConnectReady";
    public static final String CONDITION_TYPE_KAFKA_CONNECTOR_READY = "KafkaConnectorReady";

    // this could eventually go to the bundle

    static {
        try (InputStream is = DebeziumConstants.class.getResourceAsStream(DebeziumConstants.METRICS_CONFIG_RESOURCE)) {
            Objects.requireNonNull(is, "Unable to read " + METRICS_CONFIG_RESOURCE);

            // convert from yaml ...
            JsonNode doc = Serialization.yamlMapper().readValue(is, JsonNode.class);

            // .. to json
            METRICS_CONFIG = Serialization.jsonMapper().writeValueAsString(doc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (InputStream is = DebeziumConstants.class.getResourceAsStream(DebeziumConstants.LOGGING_CONFIG_RESOURCE)) {
            Objects.requireNonNull(is, "Unable to read " + LOGGING_CONFIG_RESOURCE);

            Properties props = new Properties();
            props.load(is);

            LOGGING_CONFIG = props;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private DebeziumConstants() {
    }

}
