package org.bf2.cos.fleetshard.operator.camel


import org.bf2.cos.fleetshard.operator.camel.model.EndpointKamelet
import org.bf2.cos.fleetshard.operator.camel.model.Kamelet
import org.bf2.cos.fleetshard.operator.camel.support.BaseSpec
import org.bf2.cos.fleetshard.support.resources.Secrets

import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.LABELS_TO_TRANSFER
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_CONTAINER_IMAGE
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_HEALTH_ENABLED
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_PROBE_ENABLED
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_PROBE_ENABLED
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_JVM_ENABLED
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_KAMELETS_ENABLED
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_LOGGING_JSON
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_OWNER_TARGET_LABELS

class ReifyTest extends BaseSpec {

    def 'reify'() {
        given:
            def sa = serviceAccount()
            def connector = connector()

            def sm= sharedMeta()
            sm.kamelets.adapter = new EndpointKamelet('aws-kinesis-source', 'aws')
            sm.kamelets.kafka = new EndpointKamelet('cos-kafka-sink', 'kafka')

            final String barB64 = Secrets.toBase64('bar')
        when:
            def resources = reify(
                connector,
                sm,
                sa,
                [
                    kafka_topic: DEFAULT_KAFKA_TOPIC,
                    aws_foo: 'aws-foo',
                    aws_foo_bar: 'aws-foo-bar',
                    aws_bar: [
                        kind: 'base64',
                        value: barB64
                    ],
                    processors: [
                        [ extract_field: [ field: 'field', 'foo-field': 'foo', bar_field: 'bar']],
                        [ insert_field: [ field: 'a-field', value: 'a-value']],
                        [ insert_field: [ field: 'b-field', value: 'b-value']]
                    ],
                    data_shape: [
                        produces: [ format: 'application/json' ],
                        consumes: [ format: 'application/json' ]
                    ]
                ])
        then:
            resources.size() == 2

            with(klb(resources)) {
                with(it.metadata) {
                    annotations[TRAIT_CAMEL_APACHE_ORG_CONTAINER_IMAGE] == DEFAULT_CONNECTOR_IMAGE
                    annotations[TRAIT_CAMEL_APACHE_ORG_KAMELETS_ENABLED] == 'false'
                    annotations[TRAIT_CAMEL_APACHE_ORG_JVM_ENABLED] == 'false'
                    annotations[TRAIT_CAMEL_APACHE_ORG_LOGGING_JSON] == 'false'
                    annotations[TRAIT_CAMEL_APACHE_ORG_OWNER_TARGET_LABELS] == LABELS_TO_TRANSFER
                    annotations[TRAIT_CAMEL_APACHE_ORG_HEALTH_ENABLED] == 'true'
                    annotations[TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_PROBE_ENABLED] == 'true'
                    annotations[TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_PROBE_ENABLED] == 'true'
                }

                it.spec.integration.get("profile").textValue() == CamelConstants.CAMEL_K_PROFILE_OPENSHIFT

                with(it.spec.source) {
                    it.ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it.ref.kind == Kamelet.RESOURCE_KIND
                    it.ref.name == 'aws-kinesis-source'
                    it.properties['id'] == DEFAULT_DEPLOYMENT_ID + '-source'
                }

                with(it.spec.sink) {
                    it.ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it.ref.kind == Kamelet.RESOURCE_KIND
                    it.ref.name == 'cos-kafka-sink'
                    it.properties['id'] == DEFAULT_DEPLOYMENT_ID + '-sink'
                }

                with(it.spec.steps) {
                    it[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it[0].ref.kind ==  Kamelet.RESOURCE_KIND
                    it[0].ref.name == 'cos-decoder-json-action'
                    it[0].properties['id'] == 'cos-decoder-json-action-0'

                    it[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it[1].ref.kind ==  Kamelet.RESOURCE_KIND
                    it[1].ref.name == 'extract-field-action'
                    it[1].properties['id'] == 'extract-field-action-1'

                    it[2].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it[2].ref.kind ==  Kamelet.RESOURCE_KIND
                    it[2].ref.name == 'insert-field-action'
                    it[2].properties['id'] == 'insert-field-action-2'

                    it[3].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it[3].ref.kind ==  Kamelet.RESOURCE_KIND
                    it[3].ref.name == 'insert-field-action'
                    it[3].properties['id'] == 'insert-field-action-3'

                    it[4].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it[4].ref.kind ==  Kamelet.RESOURCE_KIND
                    it[4].ref.name == 'cos-encoder-json-action'
                    it[4].properties['id'] == 'cos-encoder-json-action-4'
                }
            }

            with(secret(resources)) {
                def props = Secrets.extract(it, 'application.properties', Properties.class)

                props['camel.kamelet.cos-kafka-sink.topic'] == 'kafka-foo'
                props['camel.kamelet.cos-kafka-sink.bootstrapServers'] == 'kafka.acme.com:2181'
                props['camel.kamelet.cos-kafka-sink.user'] == 'kcid'
                props['camel.kamelet.cos-kafka-sink.password'] == 'kcs'
                props['camel.kamelet.aws-kinesis-source.bar'] == 'bar'
                props['camel.kamelet.aws-kinesis-source.foo'] == 'aws-foo'
                props['camel.kamelet.aws-kinesis-source.fooBar'] == 'aws-foo-bar'
                props['camel.kamelet.extract-field-action.extract-field-action-1.field'] == 'field'
                props['camel.kamelet.extract-field-action.extract-field-action-1.foo-field'] == 'foo'
                props['camel.kamelet.extract-field-action.extract-field-action-1.barField'] == 'bar'
                props['camel.kamelet.insert-field-action.insert-field-action-2.field'] == 'a-field'
                props['camel.kamelet.insert-field-action.insert-field-action-2.value'] == 'a-value'
                props['camel.kamelet.insert-field-action.insert-field-action-3.field'] == 'b-field'
                props['camel.kamelet.insert-field-action.insert-field-action-3.value'] == 'b-value'
            }
    }

    def 'reify with annotation'() {
        given:
            def sa = serviceAccount()
            def connector = connector()

            def sm = sharedMeta()
            sm.kamelets.adapter = new EndpointKamelet('aws-kinesis-source', 'aws')
            sm.kamelets.kafka = new EndpointKamelet('cos-kafka-sink', 'kafka')
            sm.annotations[TRAIT_CAMEL_APACHE_ORG_CONTAINER_IMAGE] = ALT_CONNECTOR_IMAGE

            final String barB64 = Secrets.toBase64('bar')
        when:
            def resources = reify(
                    connector,
                    sm,
                    sa,
                    [
                            kafka_topic: DEFAULT_KAFKA_TOPIC,
                            aws_foo: 'aws-foo',
                            aws_foo_bar: 'aws-foo-bar',
                            aws_bar: [
                                    kind: 'base64',
                                    value: barB64
                            ],
                            processors: [
                                    [ extract_field: [ field: 'field', 'foo-field': 'foo', bar_field: 'bar']],
                                    [ insert_field: [ field: 'a-field', value: 'a-value']],
                                    [ insert_field: [ field: 'b-field', value: 'b-value']]
                            ],
                            data_shape: [
                                    produces: [ format: 'application/json' ],
                                    consumes: [ format: 'application/json' ]
                            ]
                    ])
        then:
            resources.size() == 2

            with(klb(resources)) {
                with(it.metadata) {
                    annotations[TRAIT_CAMEL_APACHE_ORG_CONTAINER_IMAGE] == ALT_CONNECTOR_IMAGE
                    annotations[TRAIT_CAMEL_APACHE_ORG_KAMELETS_ENABLED] == 'false'
                    annotations[TRAIT_CAMEL_APACHE_ORG_JVM_ENABLED] == 'false'
                    annotations[TRAIT_CAMEL_APACHE_ORG_LOGGING_JSON] == 'false'
                    annotations[TRAIT_CAMEL_APACHE_ORG_OWNER_TARGET_LABELS] == LABELS_TO_TRANSFER
                    annotations[TRAIT_CAMEL_APACHE_ORG_HEALTH_ENABLED] == 'true'
                    annotations[TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_PROBE_ENABLED] == 'true'
                    annotations[TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_PROBE_ENABLED] == 'true'
                }

                it.spec.integration.get("profile").textValue() == CamelConstants.CAMEL_K_PROFILE_OPENSHIFT

                with(it.spec.source) {
                    it.ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it.ref.kind == Kamelet.RESOURCE_KIND
                    it.ref.name == 'aws-kinesis-source'
                    it.properties['id'] == DEFAULT_DEPLOYMENT_ID + '-source'
                }

                with(it.spec.sink) {
                    it.ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it.ref.kind == Kamelet.RESOURCE_KIND
                    it.ref.name == 'cos-kafka-sink'
                    it.properties['id'] == DEFAULT_DEPLOYMENT_ID + '-sink'
                }

                with(it.spec.steps) {
                    it[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it[0].ref.kind ==  Kamelet.RESOURCE_KIND
                    it[0].ref.name == 'cos-decoder-json-action'
                    it[0].properties['id'] == 'cos-decoder-json-action-0'

                    it[1].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it[1].ref.kind ==  Kamelet.RESOURCE_KIND
                    it[1].ref.name == 'extract-field-action'
                    it[1].properties['id'] == 'extract-field-action-1'

                    it[2].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it[2].ref.kind ==  Kamelet.RESOURCE_KIND
                    it[2].ref.name == 'insert-field-action'
                    it[2].properties['id'] == 'insert-field-action-2'

                    it[3].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it[3].ref.kind ==  Kamelet.RESOURCE_KIND
                    it[3].ref.name == 'insert-field-action'
                    it[3].properties['id'] == 'insert-field-action-3'

                    it[4].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it[4].ref.kind ==  Kamelet.RESOURCE_KIND
                    it[4].ref.name == 'cos-encoder-json-action'
                    it[4].properties['id'] == 'cos-encoder-json-action-4'
                }
            }

            with(secret(resources)) {
                def props = Secrets.extract(it, 'application.properties', Properties.class)

                props['camel.kamelet.cos-kafka-sink.topic'] == 'kafka-foo'
                props['camel.kamelet.cos-kafka-sink.bootstrapServers'] == 'kafka.acme.com:2181'
                props['camel.kamelet.cos-kafka-sink.user'] == 'kcid'
                props['camel.kamelet.cos-kafka-sink.password'] == 'kcs'
                props['camel.kamelet.aws-kinesis-source.bar'] == 'bar'
                props['camel.kamelet.aws-kinesis-source.foo'] == 'aws-foo'
                props['camel.kamelet.aws-kinesis-source.fooBar'] == 'aws-foo-bar'
                props['camel.kamelet.extract-field-action.extract-field-action-1.field'] == 'field'
                props['camel.kamelet.extract-field-action.extract-field-action-1.foo-field'] == 'foo'
                props['camel.kamelet.extract-field-action.extract-field-action-1.barField'] == 'bar'
                props['camel.kamelet.insert-field-action.insert-field-action-2.field'] == 'a-field'
                props['camel.kamelet.insert-field-action.insert-field-action-2.value'] == 'a-value'
                props['camel.kamelet.insert-field-action.insert-field-action-3.field'] == 'b-field'
                props['camel.kamelet.insert-field-action.insert-field-action-3.value'] == 'b-value'
            }
    }
}
