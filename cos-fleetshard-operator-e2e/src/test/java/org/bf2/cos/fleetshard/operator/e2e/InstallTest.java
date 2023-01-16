package org.bf2.cos.fleetshard.operator.e2e;

import java.util.Map;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@CucumberOptions(
    features = {
        "classpath:Install.feature"
    },
    glue = {
        "org.bf2.cos.fleetshard.operator.e2e.glues"
    })
@TestProfile(InstallTest.Profile.class)
public class InstallTest extends CucumberQuarkusTest {

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of();
        }
    }
}
