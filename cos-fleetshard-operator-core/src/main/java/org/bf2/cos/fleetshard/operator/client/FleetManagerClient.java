package org.bf2.cos.fleetshard.operator.client;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.oidc.client.filter.OidcClientRequestFilter;
import io.quarkus.restclient.NoopHostnameVerifier;
import org.bf2.cos.fleet.manager.api.ConnectorClustersAgentApi;
import org.bf2.cos.fleet.manager.model.ConnectorClusterStatus;
import org.bf2.cos.fleet.manager.model.ConnectorClusterStatusOperators;
import org.bf2.cos.fleet.manager.model.ConnectorDeployment;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentList;
import org.bf2.cos.fleet.manager.model.ConnectorDeploymentStatus;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.api.ManagedConnectorClusterStatus;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.operator.support.OperatorSupport;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.PassthroughTrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.bf2.cos.fleetshard.operator.client.FleetManagerClientHelper.call;
import static org.bf2.cos.fleetshard.operator.client.FleetManagerClientHelper.run;

@ApplicationScoped
public class FleetManagerClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(FleetManagerClient.class);

    ConnectorClustersAgentApi controlPlane;

    @Inject
    KubernetesClient kubernetesClient;

    @ConfigProperty(
        name = "quarkus.tls.trust-all")
    boolean trustAll;

    @ConfigProperty(
        name = "control-plane-base-url")
    URI controlPlaneUri;

    @ConfigProperty(
        name = "cos.manager.connect.timeout",
        defaultValue = "5s")
    Duration connectTimeout;

    @ConfigProperty(
        name = "cos.manager.read.timeout",
        defaultValue = "10s")
    Duration readTimeout;

    @PostConstruct
    void setUpClientClient() throws Exception {
        RestClientBuilder builder = RestClientBuilder.newBuilder();
        builder.baseUri(controlPlaneUri);
        builder.register(OidcClientRequestFilter.class);
        builder.connectTimeout(connectTimeout.toMillis(), TimeUnit.MILLISECONDS);
        builder.readTimeout(readTimeout.toMillis(), TimeUnit.MILLISECONDS);

        if (trustAll) {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(
                null,
                new TrustManager[] { new PassthroughTrustManager() },
                new SecureRandom());

            builder.sslContext(sslContext);
            builder.register(NoopHostnameVerifier.class);
        }

        this.controlPlane = builder.build(ConnectorClustersAgentApi.class);
    }

    public void updateClusterStatus(ManagedConnectorCluster cluster, List<ManagedConnectorOperator> operators) {
        run(() -> {
            var phase = cluster.getStatus().getPhase();
            if (phase == null) {
                phase = ManagedConnectorClusterStatus.PhaseType.Unconnected;
            }

            var ops = operators.stream()
                .map(o -> new ConnectorClusterStatusOperators()
                    .operator(OperatorSupport.toConnectorOperator(o))
                    .namespace(o.getMetadata().getName()))
                .collect(Collectors.toList());

            controlPlane.updateKafkaConnectorClusterStatus(
                cluster.getSpec().getId(),
                new ConnectorClusterStatus()
                    .phase(phase.name().toLowerCase(Locale.US))
                    .operators(ops));
        });
    }

    public ConnectorDeployment getDeployment(String clusterId, String deploymentId) {
        return call(() -> {
            return controlPlane.getClusterAssignedConnectorDeploymentById(clusterId, deploymentId);
        });
    }

    public List<ConnectorDeployment> getDeployments(String clusterId, String namespace) {
        // TODO: check namespaces
        // TODO: check labels
        final List<ManagedConnector> managedConnectors = kubernetesClient.customResources(ManagedConnector.class)
            .inNamespace(namespace)
            .list().getItems();

        final long gv = managedConnectors.stream()
            .mapToLong(c -> c.getSpec().getDeployment().getDeploymentResourceVersion())
            .max()
            .orElse(0);

        return call(() -> {
            LOGGER.debug("polling with gv: {}", gv);

            List<ConnectorDeployment> answer = new ArrayList<>();

            for (int i = 1; i < Integer.MAX_VALUE; i++) {
                ConnectorDeploymentList list = controlPlane.getClusterAssignedConnectorDeployments(
                    clusterId,
                    Integer.toString(i),
                    null,
                    gv,
                    "false");

                if (list == null || list.getItems() == null) {
                    break;
                }

                answer.addAll(list.getItems());

                if (list.getItems().isEmpty() || answer.size() == list.getTotal()) {
                    break;
                }
            }

            if (answer.isEmpty()) {
                LOGGER.info("No connectors for agent {}", clusterId);
            }

            answer.sort(Comparator.comparingLong(d -> d.getMetadata().getResourceVersion()));

            return answer;
        });
    }

    public void updateConnectorStatus(ManagedConnector connector, ConnectorDeploymentStatus status) {
        run(() -> {
            LOGGER.info("Update connector status: cluster_id={}, deployment_id={}, status={}",
                connector.getSpec().getClusterId(),
                connector.getSpec().getDeploymentId(),
                Serialization.asJson(status));

            controlPlane.updateConnectorDeploymentStatus(
                connector.getSpec().getClusterId(),
                connector.getSpec().getDeploymentId(),
                status);
        });
    }
}
