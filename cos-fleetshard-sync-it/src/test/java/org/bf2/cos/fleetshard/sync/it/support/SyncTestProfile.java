package org.bf2.cos.fleetshard.sync.it.support;

import io.quarkus.test.junit.QuarkusTestProfile;

import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

public class SyncTestProfile implements QuarkusTestProfile {
    public static final String PROPERTY_ID = "id";

    private final String id = uid();

    public String getId() {
        return id;
    }
}
