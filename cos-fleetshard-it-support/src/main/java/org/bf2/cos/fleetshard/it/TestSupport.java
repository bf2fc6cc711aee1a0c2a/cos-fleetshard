package org.bf2.cos.fleetshard.it;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import org.awaitility.Awaitility;
import org.bf2.cos.fleetshard.support.resources.UnstructuredClient;
import org.bf2.cos.fleetshard.support.resources.UnstructuredSupport;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeAll;

public class TestSupport {
    @KubernetesTestServer
    protected KubernetesServer ksrv;

    @ConfigProperty(
        name = "kubernetes.namespace")
    protected String namespace;

    @BeforeAll
    static void setUpAwaitility() {
        Awaitility.setDefaultPollInterval(500, TimeUnit.MILLISECONDS);
        Awaitility.setDefaultPollDelay(100, TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> updateUnstructured(
        String apiVersion,
        String kind,
        String name,
        Consumer<ObjectNode> consumer) {

        UnstructuredClient uc = new UnstructuredClient(ksrv.getClient());
        ObjectNode unstructured = (ObjectNode) uc.getAsNode(
            namespace,
            apiVersion,
            kind,
            name);

        consumer.accept(unstructured);

        try {
            return ksrv.getClient()
                .customResource(UnstructuredSupport.asCustomResourceDefinitionContext(unstructured))
                .updateStatus(
                    namespace,
                    name,
                    Serialization.jsonMapper().treeToValue(unstructured, Map.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
