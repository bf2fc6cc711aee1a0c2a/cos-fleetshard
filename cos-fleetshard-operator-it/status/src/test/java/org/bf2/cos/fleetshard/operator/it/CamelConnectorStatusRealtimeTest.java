package org.bf2.cos.fleetshard.operator.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.bf2.cos.fleetshard.operator.it.support.camel.CamelTestProfile;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(CamelConnectorStatusRealtimeTest.Profile.class)
public class CamelConnectorStatusRealtimeTest extends CamelConnectorStatusTestSupport {
    @Test
    public void managedCamelConnectorStatusIsReportedByRealtimeTask() {
        managedCamelConnectorStatusIsReported();
    }

    public static class Profile extends CamelTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "cos.connectors.status.sync.interval", "1s",
                "cos.connectors.status.sync.all.interval", "disabled");
        }
    }
}
