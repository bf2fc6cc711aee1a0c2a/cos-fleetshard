package org.bf2.cos.fleetshard.sync.it;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.bf2.cos.fleet.manager.model.ConnectorNamespaceState;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceStatus;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenant;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenantKind;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerMockServer;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.eclipse.microprofile.config.ConfigProvider;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.RequestMethod;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

public abstract class NamespaceReaperSyncTestBase extends SyncTestSupport {

    public static class FleetManagerTestResource extends org.bf2.cos.fleetshard.sync.it.support.ControlPlaneTestResource {
        @Override
        protected void configure(FleetManagerMockServer server) {
            final String nsId1 = ConfigProvider.getConfig().getValue("test.ns.id.1", String.class);
            final String nsId2 = ConfigProvider.getConfig().getValue("test.ns.id.2", String.class);

            var ns1 = namespace(nsId1, nsId1, ns -> {
                ConnectorNamespaceTenant tenant = new ConnectorNamespaceTenant()
                    .id(uid())
                    .kind(ConnectorNamespaceTenantKind.ORGANISATION);

                ns.setStatus(new ConnectorNamespaceStatus()
                    .state(ConnectorNamespaceState.READY)
                    .connectorsDeployed(1));

                ns.setResourceVersion(0L);
                ns.setTenant(tenant);
                ns.setExpiration(Instant.now().plus(5, ChronoUnit.MINUTES).toString());
            });

            var ns2 = namespace(nsId2, nsId2, ns -> {
                ConnectorNamespaceTenant tenant = new ConnectorNamespaceTenant()
                    .id(uid())
                    .kind(ConnectorNamespaceTenantKind.ORGANISATION);

                ns.setStatus(new ConnectorNamespaceStatus()
                    .state(ConnectorNamespaceState.READY)
                    .connectorsDeployed(1));

                ns.setResourceVersion(0L);
                ns.setTenant(tenant);
                ns.setExpiration(Instant.now().plus(5, ChronoUnit.MINUTES).toString());
            });

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces",
                req -> req.withQueryParam("gt_version", equalTo("0")),
                resp -> {
                    resp.withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                        .withJsonBody(namespaceList(ns1));
                });

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces",
                req -> req.withQueryParam("gt_version", equalTo("1")),
                resp -> {
                    resp.withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                        .withJsonBody(namespaceList(ns1, ns2));
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
