package org.bf2.cos.fleetshard.operator.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.bf2.cos.fleetshard.operator.it.support.camel.CamelTestProfile;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(CamelConnectorProvisionerRealtimeTest.Profile.class)
public class CamelConnectorProvisionerRealtimeTest extends CamelConnectorProvisionerTestSupport {
    @Test
    public void managedCamelConnectorProvisionedByRealtimeTask() {
        managedCamelConnectorProvisioned();
    }

    public static class Profile extends CamelTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "cos.connectors.poll.interval", "1s",
                "cos.connectors.sync.interval", "1s",
                "cos.connectors.sync.all.interval", "disabled");
        }
    }
}
