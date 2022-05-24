package org.bf2.cos.fleetshard.sync.it;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.bf2.cos.fleet.manager.model.ConnectorNamespaceState;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceStatus1;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenant;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenantKind;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerMockServer;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.eclipse.microprofile.config.ConfigProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.RequestMethod;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

public class NamespaceReaperTestSupport extends SyncTestSupport {

    public static class FleetManagerTestResource extends org.bf2.cos.fleetshard.sync.it.support.ControlPlaneTestResource {
        @Override
        protected void configure(FleetManagerMockServer server) {
            final String deployment = ConfigProvider.getConfig().getValue("test.deployment.id", String.class);
            final String state = ConfigProvider.getConfig().getValue("test.namespace.delete.state", String.class);
            final Integer connectors = ConfigProvider.getConfig().getOptionalValue("test.deployment.connectors", Integer.class)
                .orElse(0);

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces",
                req -> req.withQueryParam("gt_version", equalTo("0")),
                resp -> {
                    JsonNode body = namespaceList(
                        namespace(deployment, deployment,
                            ns -> {
                                ConnectorNamespaceTenant tenant = new ConnectorNamespaceTenant()
                                    .id(uid())
                                    .kind(ConnectorNamespaceTenantKind.ORGANISATION);

                                ns.setStatus(new ConnectorNamespaceStatus1()
                                    .state(ConnectorNamespaceState.DISCONNECTED)
                                    .connectorsDeployed(connectors));

                                ns.setResourceVersion(0L);
                                ns.setTenant(tenant);
                                ns.setExpiration(Instant.now().plus(5, ChronoUnit.MINUTES).toString());
                            }));

                    resp.withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                        .withJsonBody(body);
                });

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces",
                req -> req.withQueryParam("gt_version", equalTo("1")),
                resp -> {
                    JsonNode body = namespaceList(
                        namespace(deployment, deployment,
                            ns -> {
                                ConnectorNamespaceTenant tenant = new ConnectorNamespaceTenant()
                                    .id(uid())
                                    .kind(ConnectorNamespaceTenantKind.ORGANISATION);

                                ns.setStatus(new ConnectorNamespaceStatus1()
                                    .state(ConnectorNamespaceState.fromValue(state))
                                    .connectorsDeployed(connectors));

                                ns.setResourceVersion(1L);
                                ns.setTenant(tenant);
                                ns.setExpiration(Instant.now().plus(5, ChronoUnit.MINUTES).toString());
                            }));

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

            server.stubMatching(
                RequestMethod.PUT,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/status",
                () -> WireMock.ok());
        }
    }
}
