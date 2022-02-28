package org.bf2.cos.fleetshard.operator.camel

import org.bf2.cos.fleetshard.operator.camel.support.BaseSpec
import org.bf2.cos.fleetshard.support.resources.Secrets

class ReifyErrorHandlerTest extends BaseSpec {

    def 'reify error_handler (log)'() {
        given:
            def connector = connector()
            def sm = sharedMeta()
            def sa = serviceAccount()

        when:
            def resources = reify(connector, sm, sa, [
                error_handler: ['log': [:]]
            ])

        then:
            with(klb(resources)) {
                it.spec.errorHandler.get('log') != null
            }
    }


    def 'reify error_handler (dlq)'() {
        given:
            def connector = connector()
            def sm = sharedMeta()
            def sa = serviceAccount()

        when:
            def resources = reify(connector, sm, sa, [
                    error_handler: ['dead_letter_queue': [ topic: 'dlq'] ]
            ])

        then:
            with(klb(resources)) {
                it.spec.errorHandler.at("/sink/endpoint/uri").asText() == 'kamelet://cos-kafka-sink/error'
            }

            with(secret((resources))) {
                def props = Secrets.extract(it, 'application.properties', Properties.class)

                props['camel.kamelet.cos-kafka-sink.error.topic'] == 'dlq'
                props['camel.kamelet.cos-kafka-sink.error.bootstrapServers'] == 'kafka.acme.com:2181'
                props['camel.kamelet.cos-kafka-sink.error.user'] == 'kcid'
                props['camel.kamelet.cos-kafka-sink.error.password'] == 'kcs'
            }
    }

    def 'reify error_handler (stop)'() {
        given:
            def connector = connector()
            def sm = sharedMeta()
            def sa = serviceAccount()

        when:
            def resources = reify(connector, sm, sa, [
                    error_handler: ['stop': [:] ]
            ])

        then:
            with(klb(resources)) {
                it.spec.errorHandler.at("/sink/endpoint/uri").asText() == 'controlbus:route?routeId=current&action=stop'
            }
    }
}
