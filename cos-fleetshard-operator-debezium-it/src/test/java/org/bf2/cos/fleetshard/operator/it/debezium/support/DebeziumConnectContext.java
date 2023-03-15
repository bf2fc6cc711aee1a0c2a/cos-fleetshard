package org.bf2.cos.fleetshard.operator.it.debezium.support;

import javax.enterprise.context.ApplicationScoped;

import org.bf2.cos.fleetshard.operator.debezium.client.KafkaConnectorDetail;

@ApplicationScoped
public class DebeziumConnectContext {
    private KafkaConnectorDetail detail;

    public KafkaConnectorDetail getDetail() {
        return detail;
    }

    public void setDetail(KafkaConnectorDetail detail) {
        this.detail = detail;
    }
}
