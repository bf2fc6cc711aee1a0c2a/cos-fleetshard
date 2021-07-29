package org.bf2.cos.fleetshard.it;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class BaseTestProfile implements QuarkusTestProfile {
    private static final List<TestResourceEntry> DEFAULT_TEST_RESOURCES = List.of(
        new TestResourceEntry(KubernetesTestResource.class),
        new TestResourceEntry(OidcTestResource.class));

    private static final Map<String, String> DEFAULT_TEST_CONFIG = Map.of();

    @Override
    public boolean disableGlobalTestResources() {
        return true;
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> answer = new HashMap<>(DEFAULT_TEST_CONFIG);
        answer.putAll(additionalConfigOverrides());

        return answer;
    }

    protected Map<String, String> additionalConfigOverrides() {
        return Map.of();
    }

    @Override
    public List<TestResourceEntry> testResources() {
        List<TestResourceEntry> answer = new ArrayList<>(DEFAULT_TEST_RESOURCES);
        answer.addAll(additionalTestResources());

        return answer;
    }

    protected List<TestResourceEntry> additionalTestResources() {
        return List.of();
    }
}
