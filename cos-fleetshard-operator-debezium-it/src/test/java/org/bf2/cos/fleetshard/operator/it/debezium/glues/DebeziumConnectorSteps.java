package org.bf2.cos.fleetshard.operator.it.debezium.glues;

import org.bf2.cos.fleetshard.it.cucumber.support.StepsSupport;
import org.bf2.cos.fleetshard.support.resources.Secrets;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.And;
import io.fabric8.kubernetes.client.utils.Serialization;

public class DebeziumConnectorSteps extends StepsSupport {
    @ParameterType("true|false")
    public Boolean bool(String value) {
        return Boolean.valueOf(value);
    }

    private ObjectNode baseConfig() {
        var connector = Serialization.jsonMapper().createObjectNode();
        connector.put("database.dbname", "postgres");
        connector.put("database.hostname", "debezium-postgres");
        connector.withObject("/database.password").put("kind", "base64");
        connector.withObject("/database.password").put("value", "cG9zdGdyZXM=");
        connector.put("database.server.name", "dbz_pg");
        connector.put("database.user", "postgres");
        connector.put("slot.drop.on.stop", "true");
        connector.put("slot.name", "cos_dbz_pg");
        connector.put("table.include.list", "inventory.customers");
        return connector;
    }

    @And("with a simple Debezium connector")
    public void with_simple_debezium_connector() {
        with_debezium_connector_using_datashape("JSON without schema");
    }

    @And("with Debezium connector using {string} datashape")
    public void with_debezium_connector_using_datashape(String dataShape) {
        var connector = baseConfig();
        connector.withObject("/data_shape").put("key", dataShape).put("value", dataShape);
        Secrets.set(ctx.secret(), Secrets.SECRET_ENTRY_CONNECTOR, connector);

        var meta = Serialization.jsonMapper().createObjectNode();
        meta.put("container_image",
            "quay.io/rhoas/cos-connector-debezium-postgres@sha256:d64ee218a912c27b06350290fad003acf196b7739c5a3ba16426a3cdef0b21f2");
        meta.put("connector_class", "io.debezium.connector.postgresql.PostgresConnector");
        meta.put("connector_type", "source");
        Secrets.set(ctx.secret(), Secrets.SECRET_ENTRY_META, meta);
    }
}
