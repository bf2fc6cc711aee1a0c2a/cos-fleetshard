package org.bf2.cos.fleetshard.operator.it;

import java.util.Map;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@CucumberOptions(
    features = {
        "classpath:ConnectorMultipleOperators.feature"
    },
    glue = {
        "org.bf2.cos.fleetshard.it.cucumber",
    })
@TestProfile(ConnectorMultipleOperatorsTest.Profile.class)
public class ConnectorMultipleOperatorsTest extends CucumberQuarkusTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            final String ns = "cos-" + uid();

            return Map.of(
                "test.namespace", ns,
                "cos.connectors.namespace", ns,
                "cos.operators.namespace", ns,
                "cos.cluster.id", uid());
        }
    }
}
