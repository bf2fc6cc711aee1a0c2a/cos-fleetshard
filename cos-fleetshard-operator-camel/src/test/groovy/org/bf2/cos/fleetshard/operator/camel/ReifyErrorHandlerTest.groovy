package org.bf2.cos.fleetshard.operator.camel

import groovy.util.logging.Slf4j
import org.bf2.cos.fleetshard.operator.camel.model.Kamelet
import org.bf2.cos.fleetshard.operator.camel.support.BaseSpec

import static org.bf2.cos.fleetshard.operator.camel.CamelConstants.ERROR_HANDLER_DEAD_LETTER_CHANNEL_KAMELET

@Slf4j
class ReifyErrorHandlerTest extends BaseSpec {

    def 'reify error_handler (log)'() {
        given:
            def connector = connector()
            def sm = sharedMeta()
            def sa = serviceAccount()

        when:
            def resources = reify(connector, sm, sa, [
                error_handler: [ 'log': [:]]
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
                it.spec.errorHandler.requiredAt("/sink/endpoint/ref/kind").asText() == Kamelet.RESOURCE_KIND
                it.spec.errorHandler.requiredAt("/sink/endpoint/ref/apiVersion").asText() == Kamelet.RESOURCE_API_VERSION
                it.spec.errorHandler.requiredAt("/sink/endpoint/ref/name").asText() == ERROR_HANDLER_DEAD_LETTER_CHANNEL_KAMELET
                it.spec.errorHandler.requiredAt("/sink/endpoint/properties/topic").asText() == 'dlq'
                it.spec.errorHandler.requiredAt("/sink/endpoint/properties/bootstrapServers").asText() == DEFAULT_KAFKA_SERVER
                it.spec.errorHandler.requiredAt("/sink/endpoint/properties/user").asText() == '{{sa_client_id}}'
                it.spec.errorHandler.requiredAt("/sink/endpoint/properties/password").asText() == '{{sa_client_secret}}'
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
                it.spec.errorHandler.at("/sink/endpoint/uri").asText() == 'rc:fail?routeId=current'
            }
    }
}
