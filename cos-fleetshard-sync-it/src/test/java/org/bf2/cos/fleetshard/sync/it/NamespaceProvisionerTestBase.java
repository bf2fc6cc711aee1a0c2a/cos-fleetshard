package org.bf2.cos.fleetshard.sync.it;

import java.util.Objects;

import javax.inject.Inject;

import org.bf2.cos.fleetshard.support.resources.NamespacedName;
import org.bf2.cos.fleetshard.sync.FleetShardSyncConfig;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.bf2.cos.fleetshard.sync.it.support.TestFleetShardSync;
import org.bf2.cos.fleetshard.sync.it.support.WireMockServer;
import org.bf2.cos.fleetshard.sync.it.support.WireMockTestResource;
import org.eclipse.microprofile.config.ConfigProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.RequestMethod;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Secret;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

public class NamespaceProvisionerTestBase extends SyncTestSupport {
    @Inject
    FleetShardSyncConfig config;

    void checkAddonPullSecretCopiedSuccessfullyToNamespace(Namespace ns) {
        //
        // check copied addon-pullsecret exists
        //
        Secret copiedAddonPullSecret = until(
            () -> fleetShardClient
                .getSecret(new NamespacedName(ns.getMetadata().getName(), config.imagePullSecretsName())),
            Objects::nonNull);
        assertThat(copiedAddonPullSecret.getType()).isEqualTo(TestFleetShardSync.ADDON_SECRET_TYPE);
        assertThat(copiedAddonPullSecret.getData().get(TestFleetShardSync.ADDON_SECRET_FIELD))
            .isEqualTo(TestFleetShardSync.ADDON_SECRET_VALUE);
    }

    public static class FleetManagerTestResource extends WireMockTestResource {
        @Override
        protected void configure(WireMockServer server) {
            final String deployment1 = ConfigProvider.getConfig().getValue("test.deployment.id.1", String.class);
            final String deployment2 = ConfigProvider.getConfig().getValue("test.deployment.id.2", String.class);

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces",
                resp -> {
                    JsonNode body = namespaceList(
                        namespace(deployment1, deployment1),
                        namespace(deployment2, deployment2));

                    resp.withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                        .withJsonBody(body);
                });

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/deployments",
                resp -> resp.withHeader(ContentTypeHeader.KEY, APPLICATION_JSON).withJsonBody(deploymentList()));

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/deployments/.*/status",
                () -> WireMock.ok());
        }
    }
}
