package org.bf2.cos.fleetshard.operator.it.camel.glues;

import java.util.Map;

import org.bf2.cos.fleetshard.it.cucumber.support.StepsSupport;
import org.bf2.cos.fleetshard.support.resources.Secrets;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.And;
import io.fabric8.kubernetes.client.utils.Serialization;

public class CamelConnectorSteps extends StepsSupport {
    @ParameterType("true|false")
    public Boolean bool(String value) {
        return Boolean.valueOf(value);
    }

    @And("with sample camel connector")
    public void with_sample_camel_connector() {
        var connector = Serialization.jsonMapper().createObjectNode();
        connector.put("log_multi_line", true);
        connector.put("log_show_all", true);
        connector.put("kafka_topic", "dbz_pg.inventory.customers");

        var meta = Serialization.jsonMapper().createObjectNode();
        meta.put("connector_image", "quay.io/lburgazzoli/mci:0.1.2-log-sink-0.1");
        meta.put("connector_type", "sink");

        meta.withObject("/kamelets/adapter")
            .put("name", "log-sink")
            .put("prefix", "log");

        meta.withObject("/kamelets/kafka")
            .put("name", "managed-kafka-source")
            .put("prefix", "kafka");

        meta.put("error_handler_strategy", "stop");

        Secrets.set(ctx.secret(), Secrets.SECRET_ENTRY_CONNECTOR, connector);
        Secrets.set(ctx.secret(), Secrets.SECRET_ENTRY_META, meta);
    }

    @And("^with error handling configuration:$")
    public void with_error_handling_camel_connector(Map<String, String> entries) {
        final String typeKey = "type";
        final String type = entries.getOrDefault(typeKey, "log");

        var connector = Secrets.extract(ctx.secret(), Secrets.SECRET_ENTRY_CONNECTOR);
        var errorNode = connector.withObject("/error_handler").withObject("/" + type);
        for (String k : entries.keySet()) {
            if (typeKey.equals(k)) {
                continue;
            }
            errorNode.put(k, entries.get(k));
        }

        Secrets.set(ctx.secret(), Secrets.SECRET_ENTRY_CONNECTOR, connector);
    }

    @And("^with processors:$")
    public void with_processors_camel_connector(String json) throws Exception {
        var connector = Secrets.extract(ctx.secret(), Secrets.SECRET_ENTRY_CONNECTOR);
        connector.set("processors", new ObjectMapper().readTree(json));

        Secrets.set(ctx.secret(), Secrets.SECRET_ENTRY_CONNECTOR, connector);
    }
}
