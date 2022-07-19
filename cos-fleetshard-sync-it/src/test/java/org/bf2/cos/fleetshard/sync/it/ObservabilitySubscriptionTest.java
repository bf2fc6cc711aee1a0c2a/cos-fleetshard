package org.bf2.cos.fleetshard.sync.it;

import java.util.*;

import org.assertj.core.api.Assertions;
import org.bf2.cos.fleetshard.support.resources.Namespaces;
import org.bf2.cos.fleetshard.sync.it.support.FleetManagerMockServer;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestProfile;
import org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.redhat.observability.v1.Observability;

import io.fabric8.openshift.api.model.operatorhub.v1alpha1.Subscription;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@QuarkusTest
@TestProfile(ObservabilitySubscriptionTest.Profile.class)
public class ObservabilitySubscriptionTest extends SyncTestSupport {

    @Test
    void observabilityIsProvisioned() {
        Subscription subscriptionResource = until(
            () -> Optional.ofNullable(kubernetesClient.resources(Subscription.class)
                .inNamespace(config.observability().namespace())
                .withName(config.observability().subscription().name())
                .get()),
            Objects::nonNull);

        Assertions.assertThat(subscriptionResource)
            .matches(sub -> sub.getMetadata().getName().equals("observability-subscription"))
            .matches(sub -> sub.getSpec().getSource().equals("addon-connectors-operator-catalog-source"))
            .matches(sub -> sub.getSpec().getInstallPlanApproval().equals("Manual"))
            .matches(sub -> sub.getSpec().getChannel().equals("beta"))
            .matches(sub -> sub.getSpec().getSourceNamespace().equals("openshift-marketplace-namespace"))
            .matches(sub -> sub.getSpec().getStartingCSV().equals("observability-operator.v3.0.3"));

        until(
            () -> Optional.ofNullable(kubernetesClient.resources(Observability.class)
                .inNamespace(config.observability().namespace())
                .withName(config.observability().resourceName())
                .get()),
            Objects::nonNull);
    }

    public static class Profile extends SyncTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> configMap = new HashMap<>();
            configMap.put("cos.cluster.id", getId());
            configMap.put("test.namespace", Namespaces.generateNamespaceId(getId()));
            configMap.put("cos.namespace", Namespaces.generateNamespaceId(getId()));
            configMap.put("cos.observability.namespace", Namespaces.generateNamespaceId(getId()));
            configMap.put("cos.observability.enabled", "true");
            configMap.put("cos.observability.subscription.enabled", "true");
            configMap.put("cos.observability.subscription.name", "observability-subscription");
            configMap.put("cos.observability.subscription.source", "addon-connectors-operator-catalog-source");
            configMap.put("cos.observability.subscription.install-plan-approval", "Manual");
            configMap.put("cos.observability.subscription.channel", "beta");
            configMap.put("cos.observability.subscription.source-namespace", "openshift-marketplace-namespace");
            configMap.put("cos.observability.subscription.starting-csv", "observability-operator.v3.0.3");
            configMap.put("cos.observability.resource-name", "observability-resource");
            configMap.put("cos.observability.secrets-to-copy", "");
            configMap.put("cos.observability.config-maps-to-copy", "");
            configMap.put("cos.resources.update-interval", "disabled");
            configMap.put("cos.resources.poll-interval", "disabled");
            configMap.put("cos.resources.resync-interval", "disabled");
            configMap.put("cos.resources.housekeeper-interval", "disabled");
            configMap.put("cos.manager.sso-provider-refresh-timeout", "disabled");
            return configMap;
        }

        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(
                new TestResourceEntry(ObservabilitySubscriptionTest.FleetManagerTestResource.class));
        }
    }

    public static class FleetManagerTestResource extends org.bf2.cos.fleetshard.sync.it.support.ControlPlaneTestResource {
        @Override
        protected void configure(FleetManagerMockServer server) {
            server.stubMatching(
                RequestMethod.GET,
                "/api/connector_mgmt/v1/agent/kafka_connector_clusters/.*/namespaces",
                resp -> {
                    resp.withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                        .withJsonBody(namespaceList());
                });
        }
    }

}
