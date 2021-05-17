package org.bf2.cos.fleetshard.operator.connector;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.common.ResourceUtil;

public final class ConnectorSupport {
    private ConnectorSupport() {
    }

    public static Deployment createMetaDeployment(ManagedConnectorCluster owner, String ns, String name, String image) {
        var metaImageName = KubernetesResourceUtil.sanitizeName(image);

        return new DeploymentBuilder()
            .withMetadata(
                new ObjectMetaBuilder()
                    .withName(name)
                    .withNamespace(ns)
                    .withOwnerReferences(ResourceUtil.asOwnerReference(owner))
                    .addToLabels(ManagedConnector.LABEL_CONNECTOR_META, "true")
                    .addToLabels(ManagedConnector.LABEL_CONNECTOR_META_IMAGE, metaImageName)
                    .build())
            .withSpec(new DeploymentSpecBuilder()
                .withSelector(new LabelSelectorBuilder()
                    .addToMatchLabels(ManagedConnector.LABEL_CONNECTOR_META_IMAGE, metaImageName)
                    .build())
                .withTemplate(
                    new PodTemplateSpecBuilder()
                        .withMetadata(new ObjectMetaBuilder()
                            .addToLabels(ManagedConnector.LABEL_CONNECTOR_META_IMAGE, metaImageName)
                            .build())
                        .withSpec(createMetaPodSpec(image))
                        .build())
                .build())
            .build();
    }

    public static Service createMetaDeploymentService(ManagedConnectorCluster owner, String ns, String name, String image) {
        var metaImageName = KubernetesResourceUtil.sanitizeName(image);

        return new ServiceBuilder()
            .withMetadata(
                new ObjectMetaBuilder()
                    .withName(name)
                    .withNamespace(ns)
                    .withOwnerReferences(ResourceUtil.asOwnerReference(owner))
                    .addToLabels(ManagedConnector.LABEL_CONNECTOR_META, "true")
                    .addToLabels(ManagedConnector.LABEL_CONNECTOR_META_IMAGE, metaImageName)
                    .build())
            .withSpec(new ServiceSpecBuilder()
                .addToSelector(ManagedConnector.LABEL_CONNECTOR_META, "true")
                .addToSelector(ManagedConnector.LABEL_CONNECTOR_META_IMAGE, metaImageName)
                .addToPorts(new ServicePortBuilder()
                    .withPort(80)
                    .withName("http")
                    .withProtocol("TCP")
                    .withNewTargetPort(8080)
                    .build())
                .build())
            .build();
    }

    public static Pod createMetaPod(ManagedConnector connector) {
        final String name = connector.getMetadata().getName()
            + "-"
            + connector.getSpec().getDeployment().getDeploymentResourceVersion();

        return new PodBuilder()
            .withMetadata(
                new ObjectMetaBuilder()
                    .withName(name)
                    .withNamespace(connector.getMetadata().getNamespace())
                    .withOwnerReferences(ResourceUtil.asOwnerReference(connector))
                    .addToLabels(ManagedConnector.LABEL_DEPLOYMENT_ID, connector.getSpec().getDeploymentId())
                    .addToLabels(ManagedConnector.LABEL_CONNECTOR_ID, connector.getSpec().getConnectorId())
                    .addToLabels(ManagedConnector.LABEL_CONNECTOR_META, "true")
                    .build())
            .withSpec(createMetaPodSpec(connector.getSpec().getDeployment().getMetaImage()))
            .build();
    }

    public static PodSpec createMetaPodSpec(String image) {
        return new PodSpecBuilder()
            .withContainers(
                new ContainerBuilder()
                    .withName("meta")
                    .withImage(image)
                    .withPorts(new ContainerPortBuilder()
                        .withName("http")
                        .withProtocol("TCP")
                        .withContainerPort(8080)
                        .build())
                    .withLivenessProbe(new ProbeBuilder()
                        .withHttpGet(new HTTPGetActionBuilder()
                            .withNewPort(8080)
                            .withPath("/q/health/live")
                            .build())
                        .build())
                    .withReadinessProbe(new ProbeBuilder()
                        .withHttpGet(new HTTPGetActionBuilder()
                            .withNewPort(8080)
                            .withPath("/q/health/ready")
                            .build())
                        .build())
                    .build())
            .build();
    }

    public static Service createMetaPodService(ManagedConnector connector) {
        final String name = connector.getMetadata().getName()
            + "-"
            + connector.getSpec().getDeployment().getDeploymentResourceVersion();

        return new ServiceBuilder()
            .withMetadata(
                new ObjectMetaBuilder()
                    .withName(name)
                    .withNamespace(connector.getMetadata().getNamespace())
                    .withOwnerReferences(ResourceUtil.asOwnerReference(connector))
                    .addToLabels(ManagedConnector.LABEL_DEPLOYMENT_ID, connector.getSpec().getDeploymentId())
                    .addToLabels(ManagedConnector.LABEL_CONNECTOR_ID, connector.getSpec().getConnectorId())
                    .addToLabels(ManagedConnector.LABEL_CONNECTOR_META, "true")
                    .build())
            .withSpec(new ServiceSpecBuilder()
                .addToSelector(ManagedConnector.LABEL_DEPLOYMENT_ID, connector.getSpec().getDeploymentId())
                .addToSelector(ManagedConnector.LABEL_CONNECTOR_ID, connector.getSpec().getConnectorId())
                .addToSelector(ManagedConnector.LABEL_CONNECTOR_META, "true")
                .addToPorts(new ServicePortBuilder()
                    .withPort(80)
                    .withName("http")
                    .withProtocol("TCP")
                    .withNewTargetPort("http")
                    .build())
                .build())
            .build();
    }
}
