package org.bf2.cos.fleetshard.operator.it.support.camel;

import java.util.List;

import io.quarkus.test.junit.QuarkusTestProfile;
import org.bf2.cos.fleetshard.operator.it.support.KubernetesSetup;
import org.bf2.cos.fleetshard.operator.it.support.OidcSetup;
import org.bf2.cos.fleetshard.operator.it.support.OperatorSetup;

public class CamelTestProfile implements QuarkusTestProfile {
    @Override
    public boolean disableGlobalTestResources() {
        return true;
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(
            new TestResourceEntry(OidcSetup.class),
            new TestResourceEntry(OperatorSetup.class),
            new TestResourceEntry(KubernetesSetup.class),
            new TestResourceEntry(CamelMetaServiceSetup.class));
    }
}
