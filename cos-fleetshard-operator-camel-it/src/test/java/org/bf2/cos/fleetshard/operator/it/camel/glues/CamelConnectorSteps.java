package org.bf2.cos.fleetshard.operator.it.camel.glues;

import javax.inject.Inject;

import org.bf2.cos.fleetshard.it.cucumber.ConnectorContext;
import org.bf2.cos.fleetshard.support.resources.Secrets;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.And;
import io.fabric8.kubernetes.client.utils.Serialization;

public class CamelConnectorSteps {
    @Inject
    ConnectorContext ctx;

    @ParameterType("true|false")
    public Boolean bool(String value) {
        return Boolean.valueOf(value);
    }

    @And("with sample camel connector")
    public void with_sample_camel_connector() {

        var connector = Serialization.jsonMapper().createObjectNode();
        connector.with("connector").put("multiLine", true).put("showAll", true);
        connector.with("kafka").put("topic", "dbz_pg.inventory.customers");

        var meta = Serialization.jsonMapper().createObjectNode();
        meta.put("connector_image", "quay.io/lburgazzoli/mci:0.1.2-log-sink-0.1");
        meta.put("connector_type", "sink");
        meta.with("kamelets").put("connector", "log-sink");
        meta.with("kamelets").put("kafka", "managed-kafka-source");

        Secrets.set(ctx.secret(), Secrets.SECRET_ENTRY_CONNECTOR, connector);
        Secrets.set(ctx.secret(), Secrets.SECRET_ENTRY_META, meta);
    }
}
