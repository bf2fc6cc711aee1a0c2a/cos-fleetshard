package org.bf2.cos.fleetshard.operator.it.debezium.glues;

import javax.inject.Inject;

import org.bf2.cos.fleetshard.it.cucumber.ConnectorContext;
import org.bf2.cos.fleetshard.support.resources.Secrets;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.And;
import io.fabric8.kubernetes.client.utils.Serialization;

public class DebeziumConnectorSteps {
    @Inject
    ConnectorContext ctx;

    @ParameterType("true|false")
    public Boolean bool(String value) {
        return Boolean.valueOf(value);
    }

    @And("with sample debezium connector")
    public void with_sample_debezium_connector() {

        var connector = Serialization.jsonMapper().createObjectNode();
        connector.put("database.dbname", "postgres");
        connector.put("database.hostname", "debezium-postgres");
        connector.with("database.password").put("kind", "base64");
        connector.with("database.password").put("value", "cG9zdGdyZXM=");
        connector.put("database.server.name", "dbz_pg");
        connector.put("database.user", "postgres");
        connector.put("slot.drop.on.stop", "true");
        connector.put("slot.name", "cos_dbz_pg");
        connector.put("table.include.list", "inventory.customers");
        connector.put("tasks.max", "1");

        var meta = Serialization.jsonMapper().createObjectNode();
        meta.put("container_image", "quay.io/asansari/debezium-connector-postgres:1.5.3.Final");
        meta.put("connector_class", "io.debezium.connector.postgresql.PostgresConnector");
        meta.put("connector_type", "source");

        Secrets.set(ctx.secret(), Secrets.SECRET_ENTRY_CONNECTOR, connector);
        Secrets.set(ctx.secret(), Secrets.SECRET_ENTRY_META, meta);
    }
}
