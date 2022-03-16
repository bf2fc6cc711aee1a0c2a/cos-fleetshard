package org.bf2.cos.fleetshard.operator.camel

import io.fabric8.kubernetes.api.model.ConditionBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import org.bf2.cos.fleetshard.api.ConnectorStatusSpec
import org.bf2.cos.fleetshard.operator.camel.model.KameletBinding
import org.bf2.cos.fleetshard.operator.camel.model.KameletBindingStatus
import org.bf2.cos.fleetshard.operator.camel.model.KameletBindingStatusBuilder
import org.bf2.cos.fleetshard.operator.camel.support.BaseSpec
import org.mockito.Mockito

import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_FAILED
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_PROVISIONING
import static org.bf2.cos.fleetshard.api.ManagedConnector.STATE_READY

class OperandTest extends BaseSpec {

    def 'declares expected resource types'() {
        given:
            def kubernetesClient = Mockito.mock(KubernetesClient.class)
            def configuration = Mockito.mock(CamelOperandConfiguration.class)
            def controller = new CamelOperandController(kubernetesClient, configuration)

        when:
            def resources = controller.getResourceTypes()

        then:
            resources.size() == 1
            resources[0].kind == KameletBinding.RESOURCE_KIND
            resources[0].group == KameletBinding.RESOURCE_GROUP
            resources[0].version == KameletBinding.RESOURCE_VERSION
    }

    def 'status (condition false)'() {
        given:
            def status = new ConnectorStatusSpec()

        when:
            CamelOperandSupport.computeStatus(
                    status,
                    new KameletBindingStatusBuilder()
                            .withPhase(KameletBindingStatus.PHASE_READY)
                            .addToConditions(new ConditionBuilder()
                                    .withType("Ready")
                                    .withStatus("False")
                                    .withReason("reason")
                                    .withMessage("message")
                                    .build())
                            .build());

        then:
            status.phase == STATE_PROVISIONING
            status.conditions.any {
                it.type == 'Ready' && it.reason == 'reason'
            }
    }

    def 'status (ready)'() {
        given:
        def status = new ConnectorStatusSpec()

        when:
        CamelOperandSupport.computeStatus(
                status,
                new KameletBindingStatusBuilder()
                        .withPhase(KameletBindingStatus.PHASE_READY)
                        .addToConditions(new ConditionBuilder()
                                .withType("Ready")
                                .withStatus("True")
                                .withReason("reason")
                                .withMessage("message")
                                .build())
                        .build());

        then:
        status.phase == STATE_READY
        status.conditions.any {
            it.type == 'Ready' && it.reason == 'reason'
        }
    }



    def 'status (error)'() {
        given:
            def status = new ConnectorStatusSpec()

        when:
            CamelOperandSupport.computeStatus(
                    status,
                    new KameletBindingStatusBuilder()
                            .withPhase(KameletBindingStatus.PHASE_ERROR)
                            .addToConditions(new ConditionBuilder()
                                    .withType("Ready")
                                    .withStatus("False")
                                    .withReason("reason")
                                    .withMessage("message")
                                    .build())
                            .build());

        then:
            status.phase == STATE_FAILED
            status.conditions.any {
                it.type == 'Ready' && it.reason == 'reason'
            }
    }
}
