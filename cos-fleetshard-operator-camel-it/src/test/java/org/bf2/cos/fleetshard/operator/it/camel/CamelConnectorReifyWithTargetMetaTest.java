package org.bf2.cos.fleetshard.operator.it.camel;

import java.util.Map;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import static org.bf2.cos.fleetshard.support.CollectionUtils.mapOf;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@CucumberOptions(
    features = {
        "classpath:CamelConnectorReifyWithTargetMeta.feature"
    },
    glue = {
        "org.bf2.cos.fleetshard.it.cucumber",
        "org.bf2.cos.fleetshard.operator.it.camel.glues"
    })
@TestProfile(CamelConnectorReifyWithTargetMetaTest.Profile.class)
public class CamelConnectorReifyWithTargetMetaTest extends CucumberQuarkusTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            final String ns = "cos-camel-" + uid();

            return mapOf(
                "cos.operators.namespace", ns,
                "cos.connectors.target-labels[0]", "foo/barLabel",
                "cos.connectors.target-annotations[0]", "foo/barAnnotation");
        }
    }
}
