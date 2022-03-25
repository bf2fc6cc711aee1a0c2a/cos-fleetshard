package org.bf2.cos.fleetshard.sync.connector;

import java.util.List;
import java.util.UUID;

import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleet.manager.model.ServiceAccount;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorBuilder;
import org.bf2.cos.fleetshard.support.resources.Connectors;
import org.bf2.cos.fleetshard.support.resources.Secrets;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.client.FleetManagerClient;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CLUSTER_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CONNECTOR_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_DEPLOYMENT_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_DEPLOYMENT_RESOURCE_VERSION;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_UOW;
import static org.bf2.cos.fleetshard.sync.connector.ConnectorTestSupport.createDeployment;
import static org.mockito.Mockito.verify;

public class ConnectorProvisionerTest {
    private static final String CLUSTER_ID = UUID.randomUUID().toString();

    @Test
    void createResources() {
        //
        // Given that no resources associated to the provided deployment exist
        //
        final ConnectorDeployment deployment = createDeployment(0);

        final List<ManagedConnector> connectors = List.of();
        final List<Secret> secrets = List.of();

        final FleetShardClient fleetShard = ConnectorTestSupport.fleetShard(CLUSTER_ID, connectors, secrets);
        final FleetManagerClient fleetManager = ConnectorTestSupport.fleetManagerClient();
        final FleetShardSyncConfig config = ConnectorTestSupport.config();
        final ConnectorDeploymentProvisioner provisioner = new ConnectorDeploymentProvisioner(config, fleetShard, fleetManager);
        final ArgumentCaptor<Secret> sc = ArgumentCaptor.forClass(Secret.class);
        final ArgumentCaptor<ManagedConnector> mcc = ArgumentCaptor.forClass(ManagedConnector.class);

        //
        // When deployment is applied
        //
        provisioner.provision(deployment);

        verify(fleetShard).createSecret(sc.capture());
        verify(fleetShard).createConnector(mcc.capture());

        //
        // Then resources must be created according to the deployment
        //
        assertThat(sc.getValue()).satisfies(val -> {
            assertThat(val.getMetadata().getName())
                .isEqualTo(Secrets.generateConnectorSecretId(deployment.getId()));

            assertThat(val.getMetadata().getLabels())
                .containsEntry(LABEL_CLUSTER_ID, CLUSTER_ID)
                .containsEntry(LABEL_CONNECTOR_ID, deployment.getSpec().getConnectorId())
                .containsEntry(LABEL_DEPLOYMENT_ID, deployment.getId())
                .containsEntry(LABEL_DEPLOYMENT_RESOURCE_VERSION, "" + deployment.getMetadata().getResourceVersion())
                .containsKey(LABEL_UOW);

            assertThat(val.getData())
                .containsKey(Secrets.SECRET_ENTRY_SERVICE_ACCOUNT)
                .containsKey(Secrets.SECRET_ENTRY_CONNECTOR);

            var serviceAccountNode = Secrets.extract(val, Secrets.SECRET_ENTRY_SERVICE_ACCOUNT, ServiceAccount.class);
            assertThat(serviceAccountNode.getClientSecret())
                .isEqualTo(deployment.getSpec().getServiceAccount().getClientSecret());
            assertThat(serviceAccountNode.getClientId())
                .isEqualTo(deployment.getSpec().getServiceAccount().getClientId());

            var connectorNode = Secrets.extract(val, Secrets.SECRET_ENTRY_CONNECTOR);
            assertThatJson(Secrets.extract(val, Secrets.SECRET_ENTRY_CONNECTOR))
                .inPath("connector.foo").isEqualTo("connector-foo");
            assertThatJson(connectorNode)
                .inPath("kafka.topic").isEqualTo("kafka-foo");

            var metaNode = Secrets.extract(val, Secrets.SECRET_ENTRY_META);
            assertThatJson(metaNode)
                .isObject()
                .containsKey("connector_type")
                .containsKey("connector_image")
                .containsKey("kamelets")
                .containsKey("operators");

        });

        assertThat(mcc.getValue()).satisfies(val -> {
            assertThat(val.getMetadata().getName())
                .isEqualTo(Connectors.generateConnectorId(deployment.getId()));

            assertThat(val.getMetadata().getLabels())
                .containsEntry(LABEL_CLUSTER_ID, CLUSTER_ID)
                .containsEntry(LABEL_CONNECTOR_ID, deployment.getSpec().getConnectorId())
                .containsEntry(LABEL_DEPLOYMENT_ID, deployment.getId())
                .containsKey(LABEL_UOW);

            assertThat(val.getSpec().getDeployment()).satisfies(d -> {
                assertThat(d.getSecret()).isEqualTo(sc.getValue().getMetadata().getName());
                assertThat(d.getUnitOfWork())
                    .isNotEmpty()
                    .isEqualTo(sc.getValue().getMetadata().getLabels().get(LABEL_UOW));
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
        final ConnectorDeployment oldDeployment = createDeployment(0);

        final List<ManagedConnector> connectors = List.of(
            new ManagedConnectorBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(Connectors.generateConnectorId(oldDeployment.getId()))
                    .addToLabels(LABEL_CLUSTER_ID, CLUSTER_ID)
                    .addToLabels(LABEL_CONNECTOR_ID, oldDeployment.getSpec().getConnectorId())
                    .addToLabels(LABEL_DEPLOYMENT_ID, oldDeployment.getId())
                    .build())
                .build());
        final List<Secret> secrets = List.of(
            new SecretBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(Secrets.generateConnectorSecretId(oldDeployment.getId()))
                    .addToLabels(LABEL_CLUSTER_ID, CLUSTER_ID)
                    .addToLabels(LABEL_CONNECTOR_ID, oldDeployment.getSpec().getConnectorId())
                    .addToLabels(LABEL_DEPLOYMENT_ID, oldDeployment.getId())
                    .addToLabels(LABEL_DEPLOYMENT_RESOURCE_VERSION, "" + oldDeployment.getMetadata().getResourceVersion())
                    .build())
                .build());

        final FleetShardClient fleetShard = ConnectorTestSupport.fleetShard(CLUSTER_ID, connectors, secrets);
        final FleetManagerClient fleetManager = ConnectorTestSupport.fleetManagerClient();
        final FleetShardSyncConfig config = ConnectorTestSupport.config();
        final ConnectorDeploymentProvisioner provisioner = new ConnectorDeploymentProvisioner(config, fleetShard, fleetManager);
        final ArgumentCaptor<Secret> sc = ArgumentCaptor.forClass(Secret.class);
        final ArgumentCaptor<ManagedConnector> mcc = ArgumentCaptor.forClass(ManagedConnector.class);

        //
        // When deployment is updated
        //
        final ConnectorDeployment newDeployment = createDeployment(0, d -> {
            d.getSpec().getKafka().setUrl("my-kafka.acme.com:218");
            ((ObjectNode) d.getSpec().getConnectorSpec()).with("connector").put("foo", "connector-baz");
            ((ObjectNode) d.getSpec().getShardMetadata()).put("connector_image", "quay.io/mcs_dev/aws-s3-sink:0.1.0");
        });

        provisioner.provision(newDeployment);

        verify(fleetShard).createSecret(sc.capture());
        verify(fleetShard).createConnector(mcc.capture());

        //
        // Then the existing resources must be updated to reflect the changes made to the
        // deployment. This scenario could happen when a resource on the connector cluster
        // is amended outside the control of fleet manager (i.e. with kubectl) and in such
        // case, the expected behavior is that the resource is re-set to the configuration
        // from the fleet manager.
        //
        assertThat(sc.getValue()).satisfies(val -> {
            assertThat(val.getMetadata().getName())
                .isEqualTo(Secrets.generateConnectorSecretId(oldDeployment.getId()));

            assertThat(val.getMetadata().getLabels())
                .containsEntry(LABEL_CLUSTER_ID, CLUSTER_ID)
                .containsEntry(LABEL_CONNECTOR_ID, newDeployment.getSpec().getConnectorId())
                .containsEntry(LABEL_DEPLOYMENT_ID, newDeployment.getId())
                .containsEntry(LABEL_DEPLOYMENT_RESOURCE_VERSION, "" + newDeployment.getMetadata().getResourceVersion())
                .containsKey(LABEL_UOW);

            assertThat(val.getData())
                .containsKey(Secrets.SECRET_ENTRY_SERVICE_ACCOUNT)
                .containsKey(Secrets.SECRET_ENTRY_CONNECTOR);

            var serviceAccountNode = Secrets.extract(val, Secrets.SECRET_ENTRY_SERVICE_ACCOUNT, ServiceAccount.class);
            assertThat(serviceAccountNode.getClientSecret())
                .isEqualTo(newDeployment.getSpec().getServiceAccount().getClientSecret());
            assertThat(serviceAccountNode.getClientId())
                .isEqualTo(newDeployment.getSpec().getServiceAccount().getClientId());

            var connectorNode = Secrets.extract(val, Secrets.SECRET_ENTRY_CONNECTOR);
            assertThatJson(Secrets.extract(val, Secrets.SECRET_ENTRY_CONNECTOR))
                .inPath("connector.foo").isEqualTo("connector-baz");
            assertThatJson(connectorNode)
                .inPath("kafka.topic").isEqualTo("kafka-foo");

            var metaNode = Secrets.extract(val, Secrets.SECRET_ENTRY_META);
            assertThatJson(metaNode)
                .isObject()
                .containsKey("connector_type")
                .containsKey("connector_image")
                .containsKey("kamelets")
                .containsKey("operators");
        });

        assertThat(mcc.getValue()).satisfies(val -> {
            assertThat(val.getMetadata().getName())
                .isEqualTo(Connectors.generateConnectorId(oldDeployment.getId()));

            assertThat(val.getMetadata().getLabels())
                .containsEntry(LABEL_CLUSTER_ID, CLUSTER_ID)
                .containsEntry(LABEL_CONNECTOR_ID, oldDeployment.getSpec().getConnectorId())
                .containsEntry(LABEL_DEPLOYMENT_ID, oldDeployment.getId())
                .containsKey(LABEL_UOW);

            assertThat(val.getSpec().getDeployment()).satisfies(d -> {
                assertThat(d.getDeploymentResourceVersion()).isEqualTo(oldDeployment.getMetadata().getResourceVersion());
                assertThat(d.getDeploymentResourceVersion()).isEqualTo(newDeployment.getMetadata().getResourceVersion());
                assertThat(d.getSecret()).isEqualTo(sc.getValue().getMetadata().getName());
                assertThat(d.getUnitOfWork())
                    .isNotEmpty()
                    .isEqualTo(sc.getValue().getMetadata().getLabels().get(LABEL_UOW));
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
        final ConnectorDeployment oldDeployment = createDeployment(0);

        final List<ManagedConnector> connectors = List.of(
            new ManagedConnectorBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(Connectors.generateConnectorId(oldDeployment.getId()))
                    .addToLabels(LABEL_CLUSTER_ID, CLUSTER_ID)
                    .addToLabels(LABEL_CONNECTOR_ID, oldDeployment.getSpec().getConnectorId())
                    .addToLabels(LABEL_DEPLOYMENT_ID, oldDeployment.getId())
                    .build())
                .build());
        final List<Secret> secrets = List.of(
            new SecretBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(Secrets.generateConnectorSecretId(oldDeployment.getId()))
                    .addToLabels(LABEL_CLUSTER_ID, CLUSTER_ID)
                    .addToLabels(LABEL_CONNECTOR_ID, oldDeployment.getSpec().getConnectorId())
                    .addToLabels(LABEL_DEPLOYMENT_ID, oldDeployment.getId())
                    .addToLabels(LABEL_DEPLOYMENT_RESOURCE_VERSION, "" + oldDeployment.getMetadata().getResourceVersion())
                    .build())
                .build());

        final FleetShardClient fleetShard = ConnectorTestSupport.fleetShard(CLUSTER_ID, connectors, secrets);
        final FleetManagerClient fleetManager = ConnectorTestSupport.fleetManagerClient();
        final FleetShardSyncConfig config = ConnectorTestSupport.config();
        final ConnectorDeploymentProvisioner provisioner = new ConnectorDeploymentProvisioner(config, fleetShard, fleetManager);
        final ArgumentCaptor<Secret> sc = ArgumentCaptor.forClass(Secret.class);
        final ArgumentCaptor<ManagedConnector> mcc = ArgumentCaptor.forClass(ManagedConnector.class);

        //
        // When a change to the deployment happen that ends up with a new resource version
        //
        final ConnectorDeployment newDeployment = createDeployment(1, d -> {
            d.getMetadata().setResourceVersion(1L);
            d.getSpec().getKafka().setUrl("my-kafka.acme.com:218");
            ((ObjectNode) d.getSpec().getConnectorSpec()).with("connector").put("foo", "connector-baz");
            ((ObjectNode) d.getSpec().getShardMetadata()).put("connector_image", "quay.io/mcs_dev/aws-s3-sink:0.1.0");
        });

        provisioner.provision(newDeployment);

        verify(fleetShard).createSecret(sc.capture());
        verify(fleetShard).createConnector(mcc.capture());

        //
        // Then the managed connector resource is expected to be updated to reflect the
        // changes made to the deployment
        //
        assertThat(sc.getValue()).satisfies(val -> {
            assertThat(val.getMetadata().getName())
                .isEqualTo(Secrets.generateConnectorSecretId(oldDeployment.getId()));

            assertThat(val.getMetadata().getLabels())
                .containsEntry(LABEL_CLUSTER_ID, CLUSTER_ID)
                .containsEntry(LABEL_CONNECTOR_ID, newDeployment.getSpec().getConnectorId())
                .containsEntry(LABEL_DEPLOYMENT_ID, newDeployment.getId())
                .containsEntry(LABEL_DEPLOYMENT_RESOURCE_VERSION, "" + newDeployment.getMetadata().getResourceVersion())
                .containsKey(LABEL_UOW);

            assertThat(val.getData())
                .containsKey(Secrets.SECRET_ENTRY_SERVICE_ACCOUNT)
                .containsKey(Secrets.SECRET_ENTRY_CONNECTOR);

            var serviceAccountNode = Secrets.extract(val, Secrets.SECRET_ENTRY_SERVICE_ACCOUNT, ServiceAccount.class);
            assertThat(serviceAccountNode.getClientSecret())
                .isEqualTo(newDeployment.getSpec().getServiceAccount().getClientSecret());
            assertThat(serviceAccountNode.getClientId())
                .isEqualTo(newDeployment.getSpec().getServiceAccount().getClientId());

            var connectorNode = Secrets.extract(val, Secrets.SECRET_ENTRY_CONNECTOR);
            assertThatJson(Secrets.extract(val, Secrets.SECRET_ENTRY_CONNECTOR))
                .inPath("connector.foo").isEqualTo("connector-baz");
            assertThatJson(connectorNode)
                .inPath("kafka.topic").isEqualTo("kafka-foo");

            var metaNode = Secrets.extract(val, Secrets.SECRET_ENTRY_META);
            assertThatJson(metaNode)
                .isObject()
                .containsKey("connector_type")
                .containsKey("connector_image")
                .containsKey("kamelets")
                .containsKey("operators");
        });

        assertThat(mcc.getValue()).satisfies(val -> {
            assertThat(val.getMetadata().getName())
                .isEqualTo(Connectors.generateConnectorId(oldDeployment.getId()));

            assertThat(val.getMetadata().getLabels())
                .containsEntry(LABEL_CLUSTER_ID, CLUSTER_ID)
                .containsEntry(LABEL_CONNECTOR_ID, oldDeployment.getSpec().getConnectorId())
                .containsEntry(LABEL_DEPLOYMENT_ID, oldDeployment.getId())
                .containsKey(LABEL_UOW);

            assertThat(val.getSpec().getDeployment()).satisfies(d -> {
                assertThat(d.getDeploymentResourceVersion()).isEqualTo(newDeployment.getMetadata().getResourceVersion());
                assertThat(d.getDeploymentResourceVersion()).isNotEqualTo(oldDeployment.getMetadata().getResourceVersion());
                assertThat(d.getSecret()).isEqualTo(sc.getValue().getMetadata().getName());
                assertThat(d.getUnitOfWork())
                    .isNotEmpty()
                    .isEqualTo(sc.getValue().getMetadata().getLabels().get(LABEL_UOW));
                assertThat(d.getKafka().getUrl())
                    .isNotEmpty()
                    .isEqualTo(newDeployment.getSpec().getKafka().getUrl());
            });
        });
    }
}
