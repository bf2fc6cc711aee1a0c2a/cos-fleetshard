package org.bf2.cos.fleetshard.operator.camel

import groovy.util.logging.Slf4j
import org.bf2.cos.fleetshard.operator.camel.model.Kamelet
import org.bf2.cos.fleetshard.operator.camel.support.BaseSpec

@Slf4j
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
                spec.steps.size() == 4

                with(it.spec.sink) {
                    it.ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it.ref.kind == Kamelet.RESOURCE_KIND
                    it.ref.name == 'test-kafka'
                    it.properties['registryUrl'] == DEFAULT_KAFKA_REGISTRY
                    it.properties['valueSerializer'] == 'org.bf2.cos.connector.camel.serdes.json.JsonSerializer'
                }

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-resolve-schema-action'
                spec.steps[0].properties['mimeType'] == 'application/json'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-data-type-action'
                spec.steps[1].properties['format'] == 'application-json'

                spec.steps[2].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[2].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[2].ref.name == 'cos-resolve-schema-action'
                spec.steps[2].properties['mimeType'] == 'application/json'

                spec.steps[3].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[3].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[3].ref.name == 'cos-data-type-action'
                spec.steps[3].properties['format'] == 'application-json'
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
                spec.steps.size() == 4

                with(it.spec.sink) {
                    it.ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it.ref.kind == Kamelet.RESOURCE_KIND
                    it.ref.name == 'test-kafka'
                    it.properties['registryUrl'] == DEFAULT_KAFKA_REGISTRY
                    it.properties['valueSerializer'] == 'org.bf2.cos.connector.camel.serdes.json.JsonSerializer'
                }

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-resolve-schema-action'
                spec.steps[0].properties['mimeType'] == 'application/json'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-data-type-action'
                spec.steps[1].properties['format'] == 'application-json'

                spec.steps[2].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[2].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[2].ref.name == 'cos-resolve-schema-action'
                spec.steps[2].properties['mimeType'] == 'application/json'

                spec.steps[3].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[3].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[3].ref.name == 'cos-data-type-action'
                spec.steps[3].properties['format'] == 'application-json'
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
                spec.steps.size() == 4

                with(it.spec.source) {
                    it.ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it.ref.kind == Kamelet.RESOURCE_KIND
                    it.ref.name == 'test-kafka'
                    it.properties['registryUrl'] == DEFAULT_KAFKA_REGISTRY
                    it.properties['valueDeserializer'] == 'org.bf2.cos.connector.camel.serdes.json.JsonDeserializer'
                }

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-resolve-schema-action'
                spec.steps[0].properties['mimeType'] == 'application/json'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-data-type-action'
                spec.steps[1].properties['format'] == 'application-json'

                spec.steps[2].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[2].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[2].ref.name == 'cos-resolve-schema-action'
                spec.steps[2].properties['mimeType'] == 'application/json'

                spec.steps[3].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[3].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[3].ref.name == 'cos-data-type-action'
                spec.steps[3].properties['format'] == 'application-json'
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
                spec.steps.size() == 4

                with(it.spec.source) {
                    it.ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it.ref.kind == Kamelet.RESOURCE_KIND
                    it.ref.name == 'test-kafka'
                    it.properties['registryUrl'] == DEFAULT_KAFKA_REGISTRY
                    it.properties['valueDeserializer'] == 'org.bf2.cos.connector.camel.serdes.json.JsonDeserializer'
                }

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-resolve-schema-action'
                spec.steps[0].properties['mimeType'] == 'application/json'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-data-type-action'
                spec.steps[1].properties['format'] == 'application-json'

                spec.steps[2].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[2].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[2].ref.name == 'cos-resolve-schema-action'
                spec.steps[2].properties['mimeType'] == 'application/json'

                spec.steps[3].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[3].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[3].ref.name == 'cos-data-type-action'
                spec.steps[3].properties['format'] == 'application-json'
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
                spec.steps.size() == 4

                with(it.spec.sink) {
                    it.ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it.ref.kind == Kamelet.RESOURCE_KIND
                    it.ref.name == 'test-kafka'
                    it.properties['registryUrl'] == DEFAULT_KAFKA_REGISTRY
                    it.properties['valueSerializer'] == 'org.bf2.cos.connector.camel.serdes.bytes.ByteArraySerializer'
                }

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-resolve-schema-action'
                spec.steps[0].properties['mimeType'] == 'application/octet-stream'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-data-type-action'
                spec.steps[1].properties['format'] == 'application-octet-stream'

                spec.steps[2].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[2].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[2].ref.name == 'cos-resolve-schema-action'
                spec.steps[2].properties['mimeType'] == 'application/octet-stream'

                spec.steps[3].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[3].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[3].ref.name == 'cos-data-type-action'
                spec.steps[3].properties['format'] == 'application-octet-stream'
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
                spec.steps.size() == 4

                with(it.spec.sink) {
                    it.ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it.ref.kind == Kamelet.RESOURCE_KIND
                    it.ref.name == 'test-kafka'
                    it.properties['registryUrl'] == DEFAULT_KAFKA_REGISTRY
                    it.properties['valueSerializer'] == 'org.bf2.cos.connector.camel.serdes.bytes.ByteArraySerializer'
                }

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-resolve-schema-action'
                spec.steps[0].properties['mimeType'] == 'application/octet-stream'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-data-type-action'
                spec.steps[1].properties['format'] == 'application-octet-stream'

                spec.steps[2].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[2].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[2].ref.name == 'cos-resolve-schema-action'
                spec.steps[2].properties['mimeType'] == 'application/octet-stream'

                spec.steps[3].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[3].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[3].ref.name == 'cos-data-type-action'
                spec.steps[3].properties['format'] == 'application-octet-stream'
            }
    }


    def "reify with data shape (sink, bytes, explicit)"() {
        given:
            def sa = serviceAccount()
            def connector = connector()

            def sm = sharedMeta()
            sm.produces = 'application/json'
            sm.consumes = 'application/json'
            sm.connectorType = 'sink'

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
                spec.steps.size() == 4

                with(it.spec.source) {
                    it.ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it.ref.kind == Kamelet.RESOURCE_KIND
                    it.ref.name == 'test-kafka'
                    it.properties['registryUrl'] == DEFAULT_KAFKA_REGISTRY
                    it.properties['valueDeserializer'] == 'org.bf2.cos.connector.camel.serdes.bytes.ByteArrayDeserializer'
                }

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-resolve-schema-action'
                spec.steps[0].properties['mimeType'] == 'application/octet-stream'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-data-type-action'
                spec.steps[1].properties['format'] == 'application-octet-stream'

                spec.steps[2].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[2].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[2].ref.name == 'cos-resolve-schema-action'
                spec.steps[2].properties['mimeType'] == 'application/octet-stream'

                spec.steps[3].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[3].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[3].ref.name == 'cos-data-type-action'
                spec.steps[3].properties['format'] == 'application-octet-stream'
            }
    }

    def "reify with data shape (sink, bytes, defaults)"() {
        given:
            def sa = serviceAccount()
            def connector = connector()

            def sm = sharedMeta()
            sm.produces = 'application/octet-stream'
            sm.consumes = 'application/octet-stream'
            sm.connectorType = 'sink'

        when:
            def resources = reify(connector, sm, sa, [
                    data_shape: [:]
            ])

        then:
            resources.size() == 2

            with(klb(resources)) {
                spec.steps.size() == 4

                with(it.spec.source) {
                    it.ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it.ref.kind == Kamelet.RESOURCE_KIND
                    it.ref.name == 'test-kafka'
                    it.properties['registryUrl'] == DEFAULT_KAFKA_REGISTRY
                    it.properties['valueDeserializer'] == 'org.bf2.cos.connector.camel.serdes.bytes.ByteArrayDeserializer'
                }

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-resolve-schema-action'
                spec.steps[0].properties['mimeType'] == 'application/octet-stream'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-data-type-action'
                spec.steps[1].properties['format'] == 'application-octet-stream'

                spec.steps[2].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[2].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[2].ref.name == 'cos-resolve-schema-action'
                spec.steps[2].properties['mimeType'] == 'application/octet-stream'

                spec.steps[3].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[3].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[3].ref.name == 'cos-data-type-action'
                spec.steps[3].properties['format'] == 'application-octet-stream'
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
            sm.consumesClass = 'org.sample.Foo'

        when:
            def resources = reify(connector, sm, sa, [
                data_shape: [
                  produces: [ format: 'application/json' ]
                ]
            ])

        then:
            resources.size() == 2

            with(klb(resources)) {
                spec.steps.size() == 4

                with(it.spec.sink) {
                    it.ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it.ref.kind == Kamelet.RESOURCE_KIND
                    it.ref.name == 'test-kafka'
                    it.properties['registryUrl'] == DEFAULT_KAFKA_REGISTRY
                    it.properties['valueSerializer'] == 'org.bf2.cos.connector.camel.serdes.json.JsonSerializer'
                }

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-resolve-schema-action'
                spec.steps[0].properties['mimeType'] == 'application/x-java-object'
                spec.steps[0].properties['contentClass'] == 'org.sample.Foo'
                spec.steps[0].properties['targetMimeType'] == 'application/json'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-data-type-action'
                spec.steps[1].properties['format'] == 'application-x-java-object'

                spec.steps[2].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[2].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[2].ref.name == 'cos-resolve-schema-action'
                spec.steps[2].properties['mimeType'] == 'application/json'

                spec.steps[3].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[3].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[3].ref.name == 'cos-data-type-action'
                spec.steps[3].properties['format'] == 'application-json'
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
                with(it.spec.sink) {
                    it.ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it.ref.kind == Kamelet.RESOURCE_KIND
                    it.ref.name == 'test-kafka'
                    it.properties['registryUrl'] == DEFAULT_KAFKA_REGISTRY
                    it.properties['valueSerializer'] == 'org.bf2.cos.connector.camel.serdes.avro.AvroSerializer'
                }

                spec.steps.size() == 4

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-resolve-schema-action'
                spec.steps[0].properties['mimeType'] == 'application/json'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-data-type-action'
                spec.steps[1].properties['format'] == 'application-json'

                spec.steps[2].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[2].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[2].ref.name == 'cos-resolve-schema-action'
                spec.steps[2].properties['mimeType'] == 'avro/binary'

                spec.steps[3].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[3].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[3].ref.name == 'cos-data-type-action'
                spec.steps[3].properties['format'] == 'avro-binary'
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
                            consumes: [ format: 'avro/x-struct', contentClass: 'org.sample.Foo' ],
                    ]
            ])

        then:
            resources.size() == 2

            with(klb(resources)) {
                spec.steps.size() == 4

                with(it.spec.sink) {
                    it.ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it.ref.kind == Kamelet.RESOURCE_KIND
                    it.ref.name == 'test-kafka'
                    it.properties['registryUrl'] == DEFAULT_KAFKA_REGISTRY
                    it.properties['valueSerializer'] == 'org.bf2.cos.connector.camel.serdes.json.JsonSerializer'
                }

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-resolve-schema-action'
                spec.steps[0].properties['mimeType'] == 'avro/x-struct'
                spec.steps[0].properties['contentClass'] == 'org.sample.Foo'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-data-type-action'
                spec.steps[1].properties['format'] == 'avro-x-struct'

                spec.steps[2].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[2].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[2].ref.name == 'cos-resolve-schema-action'
                spec.steps[2].properties['mimeType'] == 'application/json'

                spec.steps[3].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[3].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[3].ref.name == 'cos-data-type-action'
                spec.steps[3].properties['format'] == 'application-json'
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
                            consumes: [ format: 'application/x-java-object', contentClass: 'org.sample.Foo' ],
                    ]
            ])

        then:
            resources.size() == 2

            with(klb(resources)) {
                spec.steps.size() == 4

                with(it.spec.sink) {
                    it.ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it.ref.kind == Kamelet.RESOURCE_KIND
                    it.ref.name == 'test-kafka'
                    it.properties['registryUrl'] == DEFAULT_KAFKA_REGISTRY
                    it.properties['valueSerializer'] == 'org.bf2.cos.connector.camel.serdes.json.JsonSerializer'
                }

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-resolve-schema-action'
                spec.steps[0].properties['mimeType'] == 'application/x-java-object'
                spec.steps[0].properties['targetMimeType'] == 'application/json'
                spec.steps[0].properties['contentClass'] == 'org.sample.Foo'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-data-type-action'
                spec.steps[1].properties['format'] == 'application-x-java-object'

                spec.steps[2].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[2].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[2].ref.name == 'cos-resolve-schema-action'
                spec.steps[2].properties['mimeType'] == 'application/json'

                spec.steps[3].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[3].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[3].ref.name == 'cos-data-type-action'
                spec.steps[3].properties['format'] == 'application-json'
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
                spec.steps.size() == 4

                with(it.spec.sink) {
                    it.ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it.ref.kind == Kamelet.RESOURCE_KIND
                    it.ref.name == 'test-kafka'
                    it.properties['registryUrl'] == DEFAULT_KAFKA_REGISTRY
                    it.properties['valueSerializer'] == 'org.bf2.cos.connector.camel.serdes.avro.AvroSerializer'
                }

                spec.steps[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[0].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[0].ref.name == 'cos-resolve-schema-action'
                spec.steps[0].properties['mimeType'] == 'application/x-java-object'
                spec.steps[0].properties['targetMimeType'] == 'avro/binary'

                spec.steps[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[1].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[1].ref.name == 'cos-data-type-action'
                spec.steps[1].properties['format'] == 'application-x-java-object'

                spec.steps[2].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[2].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[2].ref.name == 'cos-resolve-schema-action'
                spec.steps[2].properties['mimeType'] == 'avro/binary'

                spec.steps[3].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                spec.steps[3].ref.kind == Kamelet.RESOURCE_KIND
                spec.steps[3].ref.name == 'cos-data-type-action'
                spec.steps[3].properties['format'] == 'avro-binary'

                spec.integration.flows == null
            }
    }

}

