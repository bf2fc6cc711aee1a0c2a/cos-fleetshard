package org.bf2.cos.fleetshard.sync.it;

import java.util.List;
import java.util.Map;

import org.bf2.cos.fleet.manager.model.ConnectorNamespace;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceQuota;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceState;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceStatus1;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenant;
import org.bf2.cos.fleet.manager.model.ConnectorNamespaceTenantKind;
import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.sync.it.support.OidcTestResource;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
import org.bf2.cos.fleetshard.sync.it.support.WireMockServer;
import org.bf2.cos.fleetshard.sync.it.support.WireMockTestResource;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.RequestMethod;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@QuarkusTest
@TestProfile(NamespaceProvisionerWithQuotaDisabledTest.Profile.class)
public class NamespaceProvisionerWithQuotaDisabledTest extends NamespaceProvisionerWithNoQuotaTestBase {

    public static class Profile extends SyncTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "test.ns.id.1", uid(),
                "test.ns.id.1.limits.cpu", "0.1",
                "cos.cluster.id", getId(),
                "test.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.namespace", Namespaces.generateNamespaceId(getId()),
                "cos.quota.enabled", "false",
                "cos.resources.update-interval", "disabled",
                "cos.resources.poll-interval", "disabled",
                "cos.resources.resync-interval", "disabled",
                "cos.resources.housekeeper-interval", "disabled");
        }

        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(
                new TestResourceEntry(OidcTestResource.class),
                new TestResourceEntry(FleetManagerTestResource.class));
        }
    }

    public static class FleetManagerTestResource extends WireMockTestResource {
        @Override
        protected void configure(WireMockServer server) {
            final Config cfg = ConfigProvider.getConfig();
            final String nsId1 = cfg.getValue("test.ns.id.1", String.class);

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces",
                resp -> {
                    ConnectorNamespace ns = namespace(nsId1, nsId1, n -> {
                        n.status(new ConnectorNamespaceStatus1().state(ConnectorNamespaceState.READY).connectorsDeployed(0));
                        n.tenant(new ConnectorNamespaceTenant().id(uid()).kind(ConnectorNamespaceTenantKind.ORGANISATION));
                        n.quota(new ConnectorNamespaceQuota().cpuLimits(cfg.getValue("test.ns.id.1.limits.cpu", String.class)));
                    });

                    resp.withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                        .withJsonBody(namespaceList(ns));
                });

            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/deployments/.*/status",
                () -> WireMock.ok());
        }
    }
}
