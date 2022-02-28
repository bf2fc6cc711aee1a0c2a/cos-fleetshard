package org.bf2.cos.fleetshard.operator.camel


import org.bf2.cos.fleetshard.operator.camel.model.Kamelet
import org.bf2.cos.fleetshard.operator.camel.support.BaseSpec
import org.bf2.cos.fleetshard.support.resources.Secrets

class ReifyDataShapeTest extends BaseSpec {

    def "reify with data shape (source)"() {
        given:
            def sa = serviceAccount()
            def connector = connector()

            def sm = sharedMeta()
            sm.connectorType = 'source'

        when:
            def resources = reify(
                connector,
                sm,
                sa,
                [
                    kafka_topic: DEFAULT_KAFKA_TOPIC,
                    aws_foo: "bar",
                    processors: [
                        [ extract_field: [ field: 'field' ]]
                    ],
                    data_shape: [
                        produces: [ format: 'application/json' ],
                        consumes: [ format: 'application/json' ]
                    ]
                ])
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
        given:
            def sa = serviceAccount()
            def connector = connector()

            def sm = sharedMeta()
            sm.connectorType = 'sink'

        when:
            def resources = reify(
                connector,
                sm,
                sa,
                [
                    kafka_topic: DEFAULT_KAFKA_TOPIC,
                    aws_foo: "bar",
                    processors: [
                        [ extract_field: [ field: 'field' ]]
                    ],
                    data_shape: [
                        produces: [ format: 'application/json' ],
                        consumes: [ format: 'application/json' ]
                    ]
                ])

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
        given:
            def sa = serviceAccount()
            def connector = connector()

            def sm = sharedMeta()
            sm.consumes = 'application/x-java-object'

        when:
            def resources = reify(
                    connector,
                    sm,
                    sa,
                    [
                        kafka_topic: DEFAULT_KAFKA_TOPIC,
                        aws_foo: "bar",
                        processors: [
                          [ extract_field: [ field: 'field' ]]
                        ],
                        data_shape: [
                          produces: [ format: 'application/json' ]
                        ]
                    ])
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

    def "reify with bytes"() {
        given:
            def sa = serviceAccount()
            def connector = connector()
            def sm = sharedMeta()

        when:
            def resources = reify(connector, sm, sa, [
                kafka_topic: DEFAULT_KAFKA_TOPIC,
                aws_foo: 'bar'
            ])

        then:
            resources.size() == 2

            with(klb(resources)) {
                spec.steps.size() == 0
            }

            with(secret(resources)) {
                def props = Secrets.extract(it, 'application.properties', Properties.class)

                props["camel.kamelet.test-kafka.topic"] == "kafka-foo"
                props["camel.kamelet.test-kafka.bootstrapServers"] == DEFAULT_KAFKA_SERVER
                props["camel.kamelet.test-kafka.user"] == "kcid"
                props["camel.kamelet.test-kafka.password"] == "kcs"
                props["camel.kamelet.test-kafka.valueSerializer"] == "org.bf2.cos.connector.camel.serdes.bytes.ByteArraySerializer"
                props["camel.kamelet.test-kafka.registryUrl"] == DEFAULT_KAFKA_REGISTRY
                props["camel.kamelet.test-adapter.foo"] == "bar"
            }
    }
}

