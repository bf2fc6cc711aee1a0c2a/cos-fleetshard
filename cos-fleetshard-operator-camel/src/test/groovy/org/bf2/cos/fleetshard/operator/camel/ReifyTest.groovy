package org.bf2.cos.fleetshard.operator.camel

import com.fasterxml.jackson.databind.node.ArrayNode
import groovy.util.logging.Slf4j
import org.bf2.cos.fleetshard.operator.camel.model.EndpointKamelet
import org.bf2.cos.fleetshard.operator.camel.model.Kamelet
import org.bf2.cos.fleetshard.operator.camel.support.BaseSpec
import org.bf2.cos.fleetshard.support.json.JacksonUtil
import org.bf2.cos.fleetshard.support.resources.Secrets

import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.LABELS_TO_TRANSFER
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_CONTAINER_IMAGE
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_HEALTH_ENABLED
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_PROBE_ENABLED
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_PROBE_ENABLED
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_JVM_ENABLED
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_KAMELETS_ENABLED
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_LOGGING_JSON
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_OWNER_TARGET_ANNOTATIONS
import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.TRAIT_CAMEL_APACHE_ORG_OWNER_TARGET_LABELS

@Slf4j
class ReifyTest extends BaseSpec {

    def 'reify'(String altContainerImage, String expectedContainerImage) {
        given:
            def sa = serviceAccount()
            def connector = connector()
            connector.metadata.labels

            def sm= sharedMeta()
            sm.kamelets.adapter = new EndpointKamelet('aws-kinesis-source', 'aws')
            sm.kamelets.kafka = new EndpointKamelet('cos-kafka-sink', 'kafka')

            if (altContainerImage != null) {
                sm.annotations[TRAIT_CAMEL_APACHE_ORG_CONTAINER_IMAGE] = altContainerImage
            }

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
                        [transform: [ jq: '{"field": .field, "foo_field": .foo, "bar_field": .bar}']],
                        [transform: [ jq: '{.a-field = "a-value"}']],
                        [transform: [ jq: '{.b-field = "b-value"}']]
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
                    Set<String> annotationsToAdd = new TreeSet<>()
                    annotationsToAdd.add('my.cos.bf2.org/connector-group')

                    Set<String> labelsToAdd = new TreeSet<>(LABELS_TO_TRANSFER)
                    labelsToAdd.add('cos.bf2.org/organization-id')
                    labelsToAdd.add('cos.bf2.org/pricing-tier')

                    annotations[TRAIT_CAMEL_APACHE_ORG_CONTAINER_IMAGE] == expectedContainerImage
                    annotations[TRAIT_CAMEL_APACHE_ORG_KAMELETS_ENABLED] == 'false'
                    annotations[TRAIT_CAMEL_APACHE_ORG_JVM_ENABLED] == 'false'
                    annotations[TRAIT_CAMEL_APACHE_ORG_LOGGING_JSON] == 'false'
                    annotations[TRAIT_CAMEL_APACHE_ORG_HEALTH_ENABLED] == 'true'
                    annotations[TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_PROBE_ENABLED] == 'true'
                    annotations[TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_PROBE_ENABLED] == 'true'
                    annotations[TRAIT_CAMEL_APACHE_ORG_OWNER_TARGET_LABELS] == JacksonUtil.asArrayString(labelsToAdd)
                    annotations[TRAIT_CAMEL_APACHE_ORG_OWNER_TARGET_ANNOTATIONS] == JacksonUtil.asArrayString(annotationsToAdd)
                    annotations['my.cos.bf2.org/connector-group'] == 'baz'

                    labels['cos.bf2.org/organization-id'] == '20000000'
                    labels['cos.bf2.org/pricing-tier'] == 'essential'
                }

                with(it.spec.integration) {
                    it.get("profile").textValue() == CamelConstants.CAMEL_K_PROFILE_OPENSHIFT
                    def processors = it.get("flows").get(0).get("from").get("steps") as ArrayNode
                    processors.get(0).get("transform").get("jq").asText() == '{\"field\": .field, \"foo_field\": .foo, \"bar_field\": .bar}'
                    processors.get(1).get("transform").get("jq").asText() == '{.a-field = \"a-value\"}'
                    processors.get(2).get("transform").get("jq").asText() == '{.b-field = \"b-value\"}'
                }

                with(it.spec.source) {
                    it.ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it.ref.kind == Kamelet.RESOURCE_KIND
                    it.ref.name == 'aws-kinesis-source'
                    it.properties['id'] == DEFAULT_DEPLOYMENT_ID + '-source'
                    it.properties['bar'] == '{{aws_bar}}'
                    it.properties['fooBar'] == 'aws-foo-bar'
                }

                with(it.spec.sink) {
                    it.ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it.ref.kind == Kamelet.RESOURCE_KIND
                    it.ref.name == 'cos-kafka-sink'
                    it.properties['id'] == DEFAULT_DEPLOYMENT_ID + '-sink'
                    it.properties['user'] == '{{sa_client_id}}'
                    it.properties['password'] == '{{sa_client_secret}}'
                    it.properties['bootstrapServers'] == 'kafka.acme.com:2181'
                    it.properties['topic'] == 'kafka-foo'
                }

                with(it.spec.steps) {
                    it[0].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it[0].ref.kind ==  Kamelet.RESOURCE_KIND
                    it[0].ref.name == 'cos-decoder-json-action'

                    it[1].uri == 'direct:cos-transform'

                    it[2].ref.apiVersion == Kamelet.RESOURCE_API_VERSION
                    it[2].ref.kind ==  Kamelet.RESOURCE_KIND
                    it[2].ref.name == 'cos-encoder-json-action'
                }
            }

            with(secret(resources)) {
                def props = Secrets.extract(it, 'application.properties', Properties.class)

                props['sa_client_secret'] == 'kcs'
                props['sa_client_id'] == 'kcid'
                props['aws_bar'] == 'bar'
            }

        where:
            altContainerImage   | expectedContainerImage
            null                | DEFAULT_CONNECTOR_IMAGE
            ALT_CONNECTOR_IMAGE | ALT_CONNECTOR_IMAGE

    }
}
