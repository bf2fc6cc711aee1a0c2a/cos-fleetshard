package org.bf2.cos.fleetshard.operator.camel

import org.bf2.cos.fleetshard.operator.camel.model.Kamelet
import org.bf2.cos.fleetshard.operator.camel.support.BaseSpec

class ReifyDataShapeTest extends BaseSpec {

    // *****************************************************
    //
    // JSON
    //
    // *****************************************************

    def "reify with data shape (source, json, explicit)"() {
        given:
            def sa = serviceAccount()
            def connector = connector()

            def sm = sharedMeta()
            sm.produces = 'application/octet-stream'
            sm.consumes = 'application/octet-stream'
            sm.connectorType = 'source'

        when:
            def resources = reify(connector, sm, sa, [
                data_shape: [
                    produces: [ format: 'application/json' ],
                    consumes: [ format: 'application/json' ]
                ]
            ])

        then:
            resources.size() == 2

            with(klb(resources)) {
                spec.steps.size() == 2

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-decoder-json-action'
                spec.steps[0].properties['id'] == 'cos-decoder-json-action-0'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-encoder-json-action'
                spec.steps[1].properties['id'] == 'cos-encoder-json-action-1'
            }

            with(applicationProperties(resources)) { props ->
                props["camel.kamelet.test-kafka.valueSerializer"] == "org.bf2.cos.connector.camel.serdes.json.JsonSerializer"
                props["camel.kamelet.test-kafka.registryUrl"] == DEFAULT_KAFKA_REGISTRY
            }
    }

    def "reify with data shape (source, json, default)"() {
        given:
            def sa = serviceAccount()
            def connector = connector()

            def sm = sharedMeta()
            sm.produces = 'application/json'
            sm.consumes = 'application/json'
            sm.connectorType = 'source'

        when:
            def resources = reify(connector, sm, sa, [
                    data_shape: [:]
            ])

        then:
            resources.size() == 2

            with(klb(resources)) {
                spec.steps.size() == 2

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-decoder-json-action'
                spec.steps[0].properties['id'] == 'cos-decoder-json-action-0'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-encoder-json-action'
                spec.steps[1].properties['id'] == 'cos-encoder-json-action-1'
            }


            with(applicationProperties(resources)) { props ->
                props["camel.kamelet.test-kafka.valueSerializer"] == "org.bf2.cos.connector.camel.serdes.json.JsonSerializer"
                props["camel.kamelet.test-kafka.registryUrl"] == DEFAULT_KAFKA_REGISTRY
            }
    }

    def "reify with data shape (sink, json, explicit)"() {
        given:
            def sa = serviceAccount()
            def connector = connector()

            def sm = sharedMeta()
            sm.produces = 'application/octet-stream'
            sm.consumes = 'application/octet-stream'
            sm.connectorType = 'sink'

        when:
            def resources = reify(connector, sm, sa, [
                    data_shape: [
                            produces: [ format: 'application/json' ],
                            consumes: [ format: 'application/json' ]
                    ]
            ])

        then:
            resources.size() == 2

            with(klb(resources)) {
                spec.steps.size() == 2

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-decoder-json-action'
                spec.steps[0].properties['id'] == 'cos-decoder-json-action-0'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-encoder-json-action'
                spec.steps[1].properties['id'] == 'cos-encoder-json-action-1'
            }


            with(applicationProperties(resources)) { props ->
                props["camel.kamelet.test-kafka.valueDeserializer"] == "org.bf2.cos.connector.camel.serdes.json.JsonDeserializer"
                props["camel.kamelet.test-kafka.registryUrl"] == DEFAULT_KAFKA_REGISTRY
            }
    }

    def "reify with data shape (sink, json, default)"() {
        given:
            def sa = serviceAccount()
            def connector = connector()

            def sm = sharedMeta()
            sm.produces = 'application/json'
            sm.consumes = 'application/json'
            sm.connectorType = 'sink'

        when:
            def resources = reify(connector, sm, sa, [
                    data_shape: [:]
            ])

        then:
            resources.size() == 2

            with(klb(resources)) {
                spec.steps.size() == 2

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-decoder-json-action'
                spec.steps[0].properties['id'] == 'cos-decoder-json-action-0'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-encoder-json-action'
                spec.steps[1].properties['id'] == 'cos-encoder-json-action-1'
            }

            with(applicationProperties(resources)) { props ->
                props["camel.kamelet.test-kafka.valueDeserializer"] == "org.bf2.cos.connector.camel.serdes.json.JsonDeserializer"
                props["camel.kamelet.test-kafka.registryUrl"] == DEFAULT_KAFKA_REGISTRY
            }
    }

    // *****************************************************
    //
    // Bytes
    //
    // *****************************************************

    def "reify with data shape (source, bytes, explicit)"() {
        given:
            def sa = serviceAccount()
            def connector = connector()

            def sm = sharedMeta()
            sm.produces = 'application/json'
            sm.consumes = 'application/json'
            sm.connectorType = 'source'

        when:
            def resources = reify(connector, sm, sa, [
                    data_shape: [
                            produces: [ format: 'application/octet-stream' ],
                            consumes: [ format: 'application/octet-stream' ]
                    ]
            ])

        then:
            resources.size() == 2

            with(klb(resources)) {
                spec.steps.size() == 1

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-encoder-bytearray-action'
                spec.steps[0].properties['id'] == 'cos-encoder-bytearray-action-0'
            }

            with(applicationProperties(resources)) { props ->
                props["camel.kamelet.test-kafka.valueSerializer"] == "org.bf2.cos.connector.camel.serdes.bytes.ByteArraySerializer"
                props["camel.kamelet.test-kafka.registryUrl"] == DEFAULT_KAFKA_REGISTRY
            }
    }

    def "reify with data shape (source, bytes, defaults)"() {
        given:
            def sa = serviceAccount()
            def connector = connector()

            def sm = sharedMeta()
            sm.produces = 'application/octet-stream'
            sm.consumes = 'application/octet-stream'
            sm.connectorType = 'source'

        when:
            def resources = reify(connector, sm, sa, [
                    data_shape: [:]
            ])

        then:
            resources.size() == 2

            with(klb(resources)) {
                spec.steps.size() == 1

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-encoder-bytearray-action'
                spec.steps[0].properties['id'] == 'cos-encoder-bytearray-action-0'
            }

            with(applicationProperties(resources)) { props ->
                props["camel.kamelet.test-kafka.valueSerializer"] == "org.bf2.cos.connector.camel.serdes.bytes.ByteArraySerializer"
                props["camel.kamelet.test-kafka.registryUrl"] == DEFAULT_KAFKA_REGISTRY
            }
    }


    def "reify with data shape (sink, bytes, explicit)"() {
        given:
            def sa = serviceAccount()
            def connector = connector()

            def sm = sharedMeta()
            sm.produces = 'application/json'
            sm.consumes = 'application/json'
            sm.connectorType = 'source'

        when:
            def resources = reify(connector, sm, sa, [
                    data_shape: [
                            produces: [ format: 'application/octet-stream' ],
                            consumes: [ format: 'application/octet-stream' ]
                    ]
            ])

        then:
            resources.size() == 2

            with(klb(resources)) {
                spec.steps.size() == 1

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-encoder-bytearray-action'
                spec.steps[0].properties['id'] == 'cos-encoder-bytearray-action-0'
            }

            with(applicationProperties(resources)) { props ->
                props["camel.kamelet.test-kafka.valueSerializer"] == "org.bf2.cos.connector.camel.serdes.bytes.ByteArraySerializer"
                props["camel.kamelet.test-kafka.registryUrl"] == DEFAULT_KAFKA_REGISTRY
            }
    }

    def "reify with data shape (sink, bytes, defaults)"() {
        given:
            def sa = serviceAccount()
            def connector = connector()

            def sm = sharedMeta()
            sm.produces = 'application/octet-stream'
            sm.consumes = 'application/octet-stream'
            sm.connectorType = 'source'

        when:
            def resources = reify(connector, sm, sa, [
                    data_shape: [:]
            ])

        then:
            resources.size() == 2

            with(klb(resources)) {
                spec.steps.size() == 1

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-encoder-bytearray-action'
                spec.steps[0].properties['id'] == 'cos-encoder-bytearray-action-0'
            }

            with(applicationProperties(resources)) { props ->
                props["camel.kamelet.test-kafka.valueSerializer"] == "org.bf2.cos.connector.camel.serdes.bytes.ByteArraySerializer"
                props["camel.kamelet.test-kafka.registryUrl"] == DEFAULT_KAFKA_REGISTRY
            }
    }

    // *****************************************************
    //
    // POJO
    //
    // *****************************************************

    def "reify with pojo"() {
        given:
            def sa = serviceAccount()
            def connector = connector()

            def sm = sharedMeta()
            sm.consumes = 'application/x-java-object'

        when:
            def resources = reify(connector, sm, sa, [
                data_shape: [
                  produces: [ format: 'application/json' ]
                ]
            ])

        then:
            resources.size() == 2

            with(klb(resources)) {
                spec.steps.size() == 2

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-decoder-pojo-action'
                spec.steps[0].properties['id'] == 'cos-decoder-pojo-action-0'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-encoder-json-action'
                spec.steps[1].properties['id'] == 'cos-encoder-json-action-1'
            }

            with(applicationProperties(resources)) { props ->
                props["camel.kamelet.test-kafka.valueSerializer"] == "org.bf2.cos.connector.camel.serdes.json.JsonSerializer"
                props["camel.kamelet.test-kafka.registryUrl"] == DEFAULT_KAFKA_REGISTRY
                props["camel.kamelet.cos-decoder-pojo-action.cos-decoder-pojo-action-0.mimeType"] == "application/json"
            }
    }

    // *****************************************************
    //
    // JSON / AVRO / POJO
    //
    // *****************************************************

    def "reify with data shape (source, json2avro)"() {
        given:
            def sa = serviceAccount()
            def connector = connector()

            def sm = sharedMeta()
            sm.connectorType = 'source'

        when:
            def resources = reify(connector, sm, sa, [
                    data_shape: [
                            produces: [ format: 'avro/binary' ],
                            consumes: [ format: 'application/json' ]
                    ]
            ])

        then:
            resources.size() == 2

            with(klb(resources)) {
                spec.steps.size() == 2

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-decoder-json-action'
                spec.steps[0].properties['id'] == 'cos-decoder-json-action-0'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-encoder-avro-action'
                spec.steps[1].properties['id'] == 'cos-encoder-avro-action-1'
            }

            with(applicationProperties(resources)) { props ->
                props["camel.kamelet.test-kafka.valueSerializer"] == "org.bf2.cos.connector.camel.serdes.avro.AvroSerializer"
                props["camel.kamelet.test-kafka.registryUrl"] == DEFAULT_KAFKA_REGISTRY
            }
    }

    def "reify with data shape (source, avro2json)"() {
        given:
            def sa = serviceAccount()
            def connector = connector()

            def sm = sharedMeta()
            sm.connectorType = 'source'

        when:
            def resources = reify(connector, sm, sa, [
                    data_shape: [
                            produces: [ format: 'application/json' ],
                            consumes: [ format: 'avro/binary' ]
                    ]
            ])

        then:
            resources.size() == 2

            with(klb(resources)) {
                spec.steps.size() == 2

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-decoder-avro-action'
                spec.steps[0].properties['id'] == 'cos-decoder-avro-action-0'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-encoder-json-action'
                spec.steps[1].properties['id'] == 'cos-encoder-json-action-1'
            }

            with(applicationProperties(resources)) { props ->
                props["camel.kamelet.test-kafka.valueSerializer"] == "org.bf2.cos.connector.camel.serdes.json.JsonSerializer"
                props["camel.kamelet.test-kafka.registryUrl"] == DEFAULT_KAFKA_REGISTRY
            }
    }

    def "reify with data shape (source, pojo2json)"() {
        given:
            def sa = serviceAccount()
            def connector = connector()

            def sm = sharedMeta()
            sm.connectorType = 'source'

        when:
            def resources = reify(connector, sm, sa, [
                    data_shape: [
                            produces: [ format: 'application/json' ],
                            consumes: [ format: 'application/x-java-object' ]
                    ]
            ])

        then:
            resources.size() == 2

            with(klb(resources)) {
                spec.steps.size() == 2

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-decoder-pojo-action'
                spec.steps[0].properties['id'] == 'cos-decoder-pojo-action-0'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-encoder-json-action'
                spec.steps[1].properties['id'] == 'cos-encoder-json-action-1'
            }

            with(applicationProperties(resources)) { props ->
                props["camel.kamelet.test-kafka.valueSerializer"] == "org.bf2.cos.connector.camel.serdes.json.JsonSerializer"
                props["camel.kamelet.test-kafka.registryUrl"] == DEFAULT_KAFKA_REGISTRY
            }
    }

    def "reify with data shape (source, pojo2avro)"() {
        given:
            def sa = serviceAccount()
            def connector = connector()

            def sm = sharedMeta()
            sm.connectorType = 'source'

        when:
            def resources = reify(connector, sm, sa, [
                    data_shape: [
                            produces: [ format: 'avro/binary' ],
                            consumes: [ format: 'application/x-java-object' ]
                    ]
            ])

        then:
            resources.size() == 2

            with(klb(resources)) {
                spec.steps.size() == 2

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-decoder-pojo-action'
                spec.steps[0].properties['id'] == 'cos-decoder-pojo-action-0'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-encoder-avro-action'
                spec.steps[1].properties['id'] == 'cos-encoder-avro-action-1'
            }

            with(applicationProperties(resources)) { props ->
                props["camel.kamelet.test-kafka.valueSerializer"] == "org.bf2.cos.connector.camel.serdes.avro.AvroSerializer"
                props["camel.kamelet.test-kafka.registryUrl"] == DEFAULT_KAFKA_REGISTRY
            }
    }
}

