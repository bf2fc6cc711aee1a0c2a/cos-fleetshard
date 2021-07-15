package org.bf2.cos.fleetshard.operator.it;

import java.util.Map;

import org.bf2.cos.fleetshard.operator.it.support.camel.CamelTestProfile;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(CamelConnectorProvisionerBatchTest.Profile.class)
public class CamelConnectorProvisionerBatchTest extends CamelConnectorProvisionerTestSupport {

    @Test
    public void managedCamelConnectorStatusIsReportedByBatchTask() {
        managedCamelConnectorProvisioned();
    }

    public static class Profile extends CamelTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "cos.connectors.poll.interval", "disabled",
                "cos.connectors.sync.interval", "1s",
                "cos.connectors.sync.all.interval", "1s");
        }
    }
}
