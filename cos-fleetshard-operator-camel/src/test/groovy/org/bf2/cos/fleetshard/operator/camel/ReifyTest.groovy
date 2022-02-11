package org.bf2.cos.fleetshard.operator.camel


import org.bf2.cos.fleetshard.operator.camel.model.Kamelet
import org.bf2.cos.fleetshard.support.resources.Secrets

import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.CONNECTOR_TYPE_SINK
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.CONNECTOR_TYPE_SOURCE

class ReifyTest extends Spec {

    private static final String DEFAULT_MANAGED_CONNECTOR_ID = "mid";
    private static final Long DEFAULT_CONNECTOR_REVISION = 1L;
    private static final String DEFAULT_CONNECTOR_TYPE_ID = "ctid";
    private static final String DEFAULT_CONNECTOR_IMAGE = "quay.io/cos/s3:1";
    private static final String DEFAULT_DEPLOYMENT_ID = "1";
    private static final Long DEFAULT_DEPLOYMENT_REVISION = 1L;
    private static final String DEFAULT_KAFKA_CLIENT_ID = "kcid";
    private static final String DEFAULT_KAFKA_SERVER = "kafka.acme.com:2181"
    private static final String DEFAULT_KAFKA_REGISTRY= "http://foo.bar:443"

    // ***********************************
    //
    // Tests
    //
    // ***********************************

    def "reify with data shape (source)"() {
        when:
            def resources = reify(CONNECTOR_TYPE_SOURCE)
        then:
            resources.size() == 2

            with(klb(resources)) {
                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-decoder-json-action'
                spec.steps[0].properties['id'] == 'cos-decoder-json-action-0'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'extract-field-action'
                spec.steps[1].properties['id'] == 'extract-field-action-1'

                spec.steps[2].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[2].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[2].ref.name == 'cos-encoder-json-action'
                spec.steps[2].properties['id'] == 'cos-encoder-json-action-2'
            }

            with(secret(resources)) {
                def props = Secrets.extract(it, 'application.properties', Properties.class)

                props["camel.kamelet.test-kafka.topic"] == "kafka-foo"
                props["camel.kamelet.test-kafka.bootstrapServers"] == DEFAULT_KAFKA_SERVER
                props["camel.kamelet.test-kafka.user"] == "kcid"
                props["camel.kamelet.test-kafka.password"] == "kcs"
                props["camel.kamelet.test-kafka.valueSerializer"] == "org.bf2.cos.connector.camel.serdes.json.JsonSerializer"
                props["camel.kamelet.test-kafka.registryUrl"] == DEFAULT_KAFKA_REGISTRY
                props["camel.kamelet.test-adapter.foo"] == "bar"
                props["camel.kamelet.extract-field-action.extract-field-action-1.field"] == "field"
            }
    }

    def "reify with data shape (sink)"() {
        when:
            def resources = reify(CONNECTOR_TYPE_SINK)
        then:
            resources.size() == 2

            with(klb(resources)) {
                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-decoder-json-action'
                spec.steps[0].properties['id'] == 'cos-decoder-json-action-0'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'extract-field-action'
                spec.steps[1].properties['id'] == 'extract-field-action-1'

                spec.steps[2].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[2].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[2].ref.name == 'cos-encoder-json-action'
                spec.steps[2].properties['id'] == 'cos-encoder-json-action-2'
            }

            with(secret(resources)) {
                def props = Secrets.extract(it, 'application.properties', Properties.class)

                props["camel.kamelet.test-kafka.topic"] == "kafka-foo"
                props["camel.kamelet.test-kafka.bootstrapServers"] == DEFAULT_KAFKA_SERVER
                props["camel.kamelet.test-kafka.user"] == "kcid"
                props["camel.kamelet.test-kafka.password"] == "kcs"
                props["camel.kamelet.test-kafka.valueDeserializer"] == "org.bf2.cos.connector.camel.serdes.json.JsonDeserializer"
                props["camel.kamelet.test-kafka.registryUrl"] == DEFAULT_KAFKA_REGISTRY
                props["camel.kamelet.test-adapter.foo"] == "bar"
                props["camel.kamelet.extract-field-action.extract-field-action-1.field"] == "field"
            }
    }

    def "reify with pojo"() {
        when:
            def resources = reify(
                    """
                metadata:
                  name: ${DEFAULT_MANAGED_CONNECTOR_ID}
                spec:
                  connectorId: ${DEFAULT_MANAGED_CONNECTOR_ID}
                  deploymentId: ${DEFAULT_DEPLOYMENT_ID}
                  deployment:
                    connectorTypeId: ${DEFAULT_CONNECTOR_TYPE_ID}
                    connectorResourceVersion: ${DEFAULT_CONNECTOR_REVISION}
                    secret: "secret"
                    deploymentResourceVersion: ${DEFAULT_DEPLOYMENT_REVISION}
                    desiredState: "ready"
                    kafka:
                        url: ${DEFAULT_KAFKA_SERVER}
                """,
                    """
                connector_image: ${DEFAULT_CONNECTOR_IMAGE}
                connector_revision: ${DEFAULT_CONNECTOR_REVISION}
                connector_type: ${CONNECTOR_TYPE_SOURCE}
                kamelets:
                  adapter:
                    name: "test-adapter"
                    prefix: "aws"
                  kafka:
                    name: "test-kafka"
                    prefix: "kafka"
                  processors:
                    insert_field: "insert-field-action"
                    extract_field: "extract-field-action"
                consumes: "application/x-java-object"
                """,
                    """
                kafka_topic: "kafka-foo"
                kafka_registry_url: ${DEFAULT_KAFKA_REGISTRY}
                aws_foo: "bar"
                processors:
                  - extract_field:
                      field: field
                data_shape:
                  produces:
                    format: "application/json"
                """,
                    """
                clientId: ${DEFAULT_KAFKA_CLIENT_ID}
                clientSecret: ${Secrets.toBase64("kcs")}
                """)
        then:
            resources.size() == 2

            with(klb(resources)) {
                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-decoder-pojo-action'
                spec.steps[0].properties['id'] == 'cos-decoder-pojo-action-0'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'extract-field-action'
                spec.steps[1].properties['id'] == 'extract-field-action-1'

                spec.steps[2].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[2].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[2].ref.name == 'cos-encoder-json-action'
                spec.steps[2].properties['id'] == 'cos-encoder-json-action-2'
            }

            with(secret(resources)) {
                def props = Secrets.extract(it, 'application.properties', Properties.class)

                props["camel.kamelet.test-kafka.topic"] == "kafka-foo"
                props["camel.kamelet.test-kafka.bootstrapServers"] == DEFAULT_KAFKA_SERVER
                props["camel.kamelet.test-kafka.user"] == "kcid"
                props["camel.kamelet.test-kafka.password"] == "kcs"
                props["camel.kamelet.test-kafka.valueSerializer"] == "org.bf2.cos.connector.camel.serdes.json.JsonSerializer"
                props["camel.kamelet.test-kafka.registryUrl"] == DEFAULT_KAFKA_REGISTRY
                props["camel.kamelet.test-adapter.foo"] == "bar"
                props["camel.kamelet.extract-field-action.extract-field-action-1.field"] == "field"
                props["camel.kamelet.cos-decoder-pojo-action.cos-decoder-pojo-action-0.mimeType"] == "application/json"
            }
    }

    // ***********************************
    //
    // Helpers
    //
    // ***********************************

    def reify(String type) {
        return reify(
                """
                metadata:
                  name: ${DEFAULT_MANAGED_CONNECTOR_ID}
                spec:
                  connectorId: ${DEFAULT_MANAGED_CONNECTOR_ID}
                  deploymentId: ${DEFAULT_DEPLOYMENT_ID}
                  deployment:
                    connectorTypeId: ${DEFAULT_CONNECTOR_TYPE_ID}
                    connectorResourceVersion: ${DEFAULT_CONNECTOR_REVISION}
                    secret: "secret"
                    deploymentResourceVersion: ${DEFAULT_DEPLOYMENT_REVISION}
                    desiredState: "ready"
                    kafka:
                        url: ${DEFAULT_KAFKA_SERVER}
                """,
                """
                connector_image: ${DEFAULT_CONNECTOR_IMAGE}
                connector_revision: ${DEFAULT_CONNECTOR_REVISION}
                connector_type: ${type}
                kamelets:
                  adapter:
                    name: "test-adapter"
                    prefix: "aws"
                  kafka:
                    name: "test-kafka"
                    prefix: "kafka"
                  processors:
                    insert_field: "insert-field-action"
                    extract_field: "extract-field-action"
                """,
                """
                kafka_topic: "kafka-foo"
                kafka_registry_url: ${DEFAULT_KAFKA_REGISTRY}
                aws_foo: "bar"
                processors:
                  - extract_field:
                      field: field
                data_shape:
                  consumes:
                    format: "application/json"
                  produces:
                    format: "application/json"
                """,
                """
                clientId: ${DEFAULT_KAFKA_CLIENT_ID}
                clientSecret: ${Secrets.toBase64("kcs")}
                """)
    }
}

