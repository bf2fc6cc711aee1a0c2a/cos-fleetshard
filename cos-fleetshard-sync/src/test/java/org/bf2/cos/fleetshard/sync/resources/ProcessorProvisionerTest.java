package org.bf2.cos.fleetshard.sync.resources;

import java.util.List;
import java.util.UUID;

import org.bf2.cos.fleet.manager.model.ProcessorDeployment;
import org.bf2.cos.fleet.manager.model.ServiceAccount;
import org.bf2.cos.fleetshard.api.ManagedProcessor;
import org.bf2.cos.fleetshard.api.ManagedProcessorBuilder;
import org.bf2.cos.fleetshard.api.ManagedProcessorSpec;
import org.bf2.cos.fleetshard.support.client.EventClient;
import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.bf2.cos.fleetshard.support.resources.Processors;
import org.bf2.cos.fleetshard.support.resources.Secrets;
import org.bf2.cos.fleetshard.sync.processor.ProcessorTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CLUSTER_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_DEPLOYMENT_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_DEPLOYMENT_RESOURCE_VERSION;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_PROCESSOR_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_UOW;
import static org.bf2.cos.fleetshard.sync.processor.ProcessorTestSupport.createProcessorDeployment;
import static org.mockito.Mockito.verify;

public class ProcessorProvisionerTest {
    private static final String CLUSTER_ID = UUID.randomUUID().toString();

    @Test
    void createResources() {
        //
        // Given that no resources associated to the provided deployment exist
        //
        final ProcessorDeployment deployment = createProcessorDeployment(0);

        final List<ManagedProcessor> processors = List.of();
        final List<Secret> secrets = List.of();

        final ArgumentCaptor<Secret> sc = ArgumentCaptor.forClass(Secret.class);
        final ArgumentCaptor<ManagedProcessor> mcc = ArgumentCaptor.forClass(ManagedProcessor.class);

        final ProcessorDeploymentProvisioner provisioner = new ProcessorDeploymentProvisioner();
        provisioner.config = ProcessorTestSupport.config();

        provisioner.fleetShard = ProcessorTestSupport.fleetShard(CLUSTER_ID, processors, secrets);
        provisioner.fleetManager = ProcessorTestSupport.fleetManagerClient();
        provisioner.eventClient = Mockito.mock(EventClient.class);
        provisioner.recorder = Mockito.mock(MetricsRecorder.class);

        //
        // When deployment is applied
        //
        provisioner.provision(deployment);

        verify(provisioner.fleetShard).createSecret(sc.capture());
        verify(provisioner.fleetShard).createProcessor(mcc.capture());

        //
        // Then resources must be created according to the deployment
        //
        assertThat(sc.getValue()).satisfies(val -> {
            assertThat(val.getMetadata().getName())
                .isEqualTo(Secrets.generateProcessorSecretId(deployment.getId()));

            assertThat(val.getMetadata().getLabels())
                .containsEntry(LABEL_CLUSTER_ID, CLUSTER_ID)
                .containsEntry(LABEL_PROCESSOR_ID, deployment.getSpec().getProcessorId())
                .containsEntry(LABEL_DEPLOYMENT_ID, deployment.getId())
                .containsEntry(LABEL_DEPLOYMENT_RESOURCE_VERSION, "" + deployment.getMetadata().getResourceVersion())
                .containsKey(LABEL_UOW);

        });

        assertThat(mcc.getValue()).satisfies(val -> {
            assertThat(val.getMetadata().getName())
                .isEqualTo(Processors.generateProcessorId(deployment.getId()));

            assertThat(val.getMetadata().getLabels())
                .containsEntry(LABEL_CLUSTER_ID, CLUSTER_ID)
                .containsEntry(LABEL_PROCESSOR_ID, deployment.getSpec().getProcessorId())
                .containsEntry(LABEL_DEPLOYMENT_ID, deployment.getId())
                .containsKey(LABEL_UOW);

            assertThat(val.getSpec()).satisfies(d -> {
                assertThat(d.getSecret()).isEqualTo(sc.getValue().getMetadata().getName());
                assertThat(d.getKafka().getUrl())
                    .isNotEmpty()
                    .isEqualTo(deployment.getSpec().getKafka().getUrl());
            });
        });
    }

    @Test
    void updateResources() {
        //
        // Given that the resources associated to the provided deployment exist
        //
        final ProcessorDeployment oldDeployment = ProcessorTestSupport.createProcessorDeployment(0);

        final List<ManagedProcessor> processors = List.of(
            new ManagedProcessorBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(Processors.generateProcessorId(oldDeployment.getId()))
                    .addToLabels(LABEL_CLUSTER_ID, CLUSTER_ID)
                    .addToLabels(LABEL_PROCESSOR_ID, oldDeployment.getSpec().getProcessorId())
                    .addToLabels(LABEL_DEPLOYMENT_ID, oldDeployment.getId())
                    .build())
                .withSpec(new ManagedProcessorSpec())
                .build());
        final List<Secret> secrets = List.of(
            new SecretBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(Secrets.generateProcessorSecretId(oldDeployment.getId()))
                    .addToLabels(LABEL_CLUSTER_ID, CLUSTER_ID)
                    .addToLabels(LABEL_PROCESSOR_ID, oldDeployment.getSpec().getProcessorId())
                    .addToLabels(LABEL_DEPLOYMENT_ID, oldDeployment.getId())
                    .addToLabels(LABEL_DEPLOYMENT_RESOURCE_VERSION, "" + oldDeployment.getMetadata().getResourceVersion())
                    .build())
                .build());

        final ProcessorDeploymentProvisioner provisioner = new ProcessorDeploymentProvisioner();
        provisioner.config = ProcessorTestSupport.config();
        provisioner.fleetShard = ProcessorTestSupport.fleetShard(CLUSTER_ID, processors, secrets);
        provisioner.fleetManager = ProcessorTestSupport.fleetManagerClient();
        provisioner.eventClient = Mockito.mock(EventClient.class);
        provisioner.recorder = Mockito.mock(MetricsRecorder.class);

        final ArgumentCaptor<Secret> sc = ArgumentCaptor.forClass(Secret.class);
        final ArgumentCaptor<ManagedProcessor> mcc = ArgumentCaptor.forClass(ManagedProcessor.class);

        //
        // When deployment is updated
        //
        final ProcessorDeployment newDeployment = ProcessorTestSupport.createProcessorDeployment(0, d -> {
            d.getSpec().getKafka().setUrl("my-kafka.acme.com:218");
            // TODO what is this in processsors?
            //            ((ObjectNode) d.getSpec()).withObject("/connector").put("foo", "connector-baz");
        });

        provisioner.provision(newDeployment);

        verify(provisioner.fleetShard).createSecret(sc.capture());
        verify(provisioner.fleetShard).createProcessor(mcc.capture());

        //
        // Then the existing resources must be updated to reflect the changes made to the
        // deployment. This scenario could happen when a resource on the connector cluster
        // is amended outside the control of fleet manager (i.e. with kubectl) and in such
        // case, the expected behavior is that the resource is re-set to the configuration
        // from the fleet manager.
        //
        assertThat(sc.getValue()).satisfies(val -> {
            assertThat(val.getMetadata().getName())
                .isEqualTo(Secrets.generateProcessorSecretId(oldDeployment.getId()));

            assertThat(val.getMetadata().getLabels())
                .containsEntry(LABEL_CLUSTER_ID, CLUSTER_ID)
                .containsEntry(LABEL_PROCESSOR_ID, newDeployment.getSpec().getProcessorId())
                .containsEntry(LABEL_DEPLOYMENT_ID, newDeployment.getId())
                .containsEntry(LABEL_DEPLOYMENT_RESOURCE_VERSION, "" + newDeployment.getMetadata().getResourceVersion())
                .containsKey(LABEL_UOW);

            var serviceAccountNode = Secrets.extract(val, Secrets.SECRET_ENTRY_SERVICE_ACCOUNT, ServiceAccount.class);
            assertThat(serviceAccountNode.getClientSecret())
                .isEqualTo(newDeployment.getSpec().getServiceAccount().getClientSecret());
            assertThat(serviceAccountNode.getClientId())
                .isEqualTo(newDeployment.getSpec().getServiceAccount().getClientId());
        });

        assertThat(mcc.getValue()).satisfies(val -> {
            assertThat(val.getMetadata().getName())
                .isEqualTo(Processors.generateProcessorId(oldDeployment.getId()));

            assertThat(val.getMetadata().getLabels())
                .containsEntry(LABEL_CLUSTER_ID, CLUSTER_ID)
                .containsEntry(LABEL_PROCESSOR_ID, oldDeployment.getSpec().getProcessorId())
                .containsEntry(LABEL_DEPLOYMENT_ID, oldDeployment.getId())
                .containsKey(LABEL_UOW);

            assertThat(val.getSpec()).satisfies(d -> {
                assertThat(d.getDeploymentResourceVersion()).isEqualTo(oldDeployment.getMetadata().getResourceVersion());
                assertThat(d.getDeploymentResourceVersion()).isEqualTo(newDeployment.getMetadata().getResourceVersion());
                assertThat(d.getSecret()).isEqualTo(sc.getValue().getMetadata().getName());
                assertThat(d.getKafka().getUrl())
                    .isNotEmpty()
                    .isEqualTo(newDeployment.getSpec().getKafka().getUrl());
            });
        });
    }

    @Test
    void updateAndCreateResources() {
        //
        // Given that the resources associated to the provided deployment exist
        //
        final ProcessorDeployment oldDeployment = ProcessorTestSupport.createProcessorDeployment(0);

        final List<ManagedProcessor> processors = List.of(
            new ManagedProcessorBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(Processors.generateProcessorId(oldDeployment.getId()))
                    .addToLabels(LABEL_CLUSTER_ID, CLUSTER_ID)
                    .addToLabels(LABEL_PROCESSOR_ID, oldDeployment.getSpec().getProcessorId())
                    .addToLabels(LABEL_DEPLOYMENT_ID, oldDeployment.getId())
                    .build())
                .withSpec(new ManagedProcessorSpec())
                .build());
        final List<Secret> secrets = List.of(
            new SecretBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(Secrets.generateProcessorSecretId(oldDeployment.getId()))
                    .addToLabels(LABEL_CLUSTER_ID, CLUSTER_ID)
                    .addToLabels(LABEL_PROCESSOR_ID, oldDeployment.getSpec().getProcessorId())
                    .addToLabels(LABEL_DEPLOYMENT_ID, oldDeployment.getId())
                    .addToLabels(LABEL_DEPLOYMENT_RESOURCE_VERSION, "" + oldDeployment.getMetadata().getResourceVersion())
                    .build())
                .build());

        final ProcessorDeploymentProvisioner provisioner = new ProcessorDeploymentProvisioner();
        provisioner.config = ProcessorTestSupport.config();
        provisioner.fleetShard = ProcessorTestSupport.fleetShard(CLUSTER_ID, processors, secrets);
        provisioner.fleetManager = ProcessorTestSupport.fleetManagerClient();
        provisioner.eventClient = Mockito.mock(EventClient.class);
        provisioner.recorder = Mockito.mock(MetricsRecorder.class);

        final ArgumentCaptor<Secret> sc = ArgumentCaptor.forClass(Secret.class);
        final ArgumentCaptor<ManagedProcessor> processorArgumentCaptor = ArgumentCaptor.forClass(ManagedProcessor.class);

        //
        // When a change to the deployment happen that ends up with a new resource version
        //
        final ProcessorDeployment newDeployment = ProcessorTestSupport.createProcessorDeployment(0, d -> {
            d.getMetadata().setResourceVersion(1L);
            d.getSpec().getKafka().setUrl("my-kafka.acme.com:218");
            // TODO what is this in processors?
            //            ((ObjectNode) d.getSpec()).withObject("/connector").put("foo", "connector-baz");
        });

        provisioner.provision(newDeployment);

        verify(provisioner.fleetShard).createSecret(sc.capture());
        verify(provisioner.fleetShard).createProcessor(processorArgumentCaptor.capture());

        //
        // Then the managed connector resource is expected to be updated to reflect the
        // changes made to the deployment
        //
        assertThat(sc.getValue()).satisfies(val -> {
            assertThat(val.getMetadata().getName())
                .isEqualTo(Secrets.generateProcessorSecretId(oldDeployment.getId()));

            assertThat(val.getMetadata().getLabels())
                .containsEntry(LABEL_CLUSTER_ID, CLUSTER_ID)
                .containsEntry(LABEL_PROCESSOR_ID, newDeployment.getSpec().getProcessorId())
                .containsEntry(LABEL_DEPLOYMENT_ID, newDeployment.getId())
                .containsEntry(LABEL_DEPLOYMENT_RESOURCE_VERSION, "" + newDeployment.getMetadata().getResourceVersion())
                .containsKey(LABEL_UOW);

            var serviceAccountNode = Secrets.extract(val, Secrets.SECRET_ENTRY_SERVICE_ACCOUNT, ServiceAccount.class);
            assertThat(serviceAccountNode.getClientSecret())
                .isEqualTo(newDeployment.getSpec().getServiceAccount().getClientSecret());
            assertThat(serviceAccountNode.getClientId())
                .isEqualTo(newDeployment.getSpec().getServiceAccount().getClientId());
        });

        assertThat(processorArgumentCaptor.getValue()).satisfies(val -> {
            assertThat(val.getMetadata().getName())
                .isEqualTo(Processors.generateProcessorId(oldDeployment.getId()));

            assertThat(val.getMetadata().getLabels())
                .containsEntry(LABEL_CLUSTER_ID, CLUSTER_ID)
                .containsEntry(LABEL_PROCESSOR_ID, oldDeployment.getSpec().getProcessorId())
                .containsEntry(LABEL_DEPLOYMENT_ID, oldDeployment.getId())
                .containsKey(LABEL_UOW);

            assertThat(val.getSpec()).satisfies(d -> {
                assertThat(d.getDeploymentResourceVersion()).isEqualTo(newDeployment.getMetadata().getResourceVersion());
                assertThat(d.getDeploymentResourceVersion()).isNotEqualTo(oldDeployment.getMetadata().getResourceVersion());
                assertThat(d.getSecret()).isEqualTo(sc.getValue().getMetadata().getName());
                assertThat(d.getKafka().getUrl())
                    .isNotEmpty()
                    .isEqualTo(newDeployment.getSpec().getKafka().getUrl());
            });
        });
    }
}
