package org.bf2.cos.fleetshard.operator.it;

import java.util.Map;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@CucumberOptions(
    features = {
        "classpath:ConnectorCustomLabels.feature"
    },
    glue = {
        "org.bf2.cos.fleetshard.it.cucumber"
    })
@TestProfile(ConnectorCustomLabelsTest.Profile.class)
public class ConnectorCustomLabelsTest extends CucumberQuarkusTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            final String ns = "cos-" + uid();

            return Map.of(
                "cos.metrics.recorder.tags.common.foo", "bar",
                "cos.metrics.recorder.tags.labels[0]", "my.cos.bf2.org/connector-group",
                "cos.metrics.recorder.tags.annotations[0]", "cos.bf2.org/organisation-id",
                "cos.metrics.recorder.tags.annotations[1]", "cos.bf2.org/pricing-tier",
                "test.namespace", ns,
                "cos.namespace", ns);
        }
    }
}
