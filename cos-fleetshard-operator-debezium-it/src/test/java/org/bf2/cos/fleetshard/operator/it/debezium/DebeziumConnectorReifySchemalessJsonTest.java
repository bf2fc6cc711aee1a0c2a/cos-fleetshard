package org.bf2.cos.fleetshard.operator.it.debezium;

import java.util.Map;
import java.util.function.Consumer;

import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@CucumberOptions(
    features = {
        "classpath:DebeziumConnectorReifySchemalessJson.feature"
    },
    glue = {
        "org.bf2.cos.fleetshard.it.cucumber",
        "org.bf2.cos.fleetshard.operator.it.debezium.glues"
    })
@TestProfile(DebeziumConnectorReifySchemalessJsonTest.Profile.class)
@WithKubernetesTestServer(setup = DebeziumConnectorReifyJsonWithSchemaTest.Setup.class)
public class DebeziumConnectorReifySchemalessJsonTest extends CucumberQuarkusTest {

    @KubernetesTestServer
    KubernetesServer k8sServer;

    public static class Setup implements Consumer<KubernetesServer> {
        @Override
        public void accept(KubernetesServer server) {
        }
    }

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            final String ns = "cos-debezium-" + uid();

            return Map.of(
                "cos.namespace", ns);
        }
    }
}
