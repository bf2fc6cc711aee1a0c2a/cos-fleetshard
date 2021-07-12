package org.bf2.cos.fleetshard.operator.it;

import java.util.Map;

import org.bf2.cos.fleetshard.operator.it.support.camel.CamelTestProfile;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(CamelConnectorStatusBatchTest.Profile.class)
public class CamelConnectorStatusBatchTest extends CamelConnectorStatusTestSupport {

    @Test
    public void managedCamelConnectorStatusIsReportedByBatchTask() {
        managedCamelConnectorStatusIsReported();
    }

    public static class Profile extends CamelTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "cos.connectors.status.sync.interval", "1s",
                "cos.connectors.status.sync.all.interval", "1s");
        }
    }
}
