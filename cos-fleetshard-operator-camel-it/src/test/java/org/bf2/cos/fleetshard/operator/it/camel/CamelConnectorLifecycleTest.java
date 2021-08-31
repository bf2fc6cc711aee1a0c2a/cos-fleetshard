package org.bf2.cos.fleetshard.operator.it.camel;

import java.util.Map;

import org.bf2.cos.fleetshard.it.resources.BaseTestProfile;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@CucumberOptions(
    features = {
        "classpath:CamelConnectorLifecycle.feature"
    },
    glue = {
        "org.bf2.cos.fleetshard.it.cucumber",
        "org.bf2.cos.fleetshard.operator.it.camel.glues"
    })
@TestProfile(CamelConnectorLifecycleTest.Profile.class)
public class CamelConnectorLifecycleTest extends CucumberQuarkusTest {
    public static class Profile extends BaseTestProfile {
        @Override
        protected Map<String, String> additionalConfigOverrides() {
            final String ns = "cos-" + uid();

            return Map.of(
                "test.namespace", ns,
                "cos.connectors.namespace", ns,
                "cos.operator.namespace", ns,
                "cos.cluster.id", uid());
        }
    }
}
