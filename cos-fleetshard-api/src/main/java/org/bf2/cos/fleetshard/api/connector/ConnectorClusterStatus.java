package org.bf2.cos.fleetshard.api.connector;

import io.sundr.builder.annotations.Buildable;
import org.bf2.cos.fleetshard.api.connector.support.Status;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
public class ConnectorClusterStatus extends Status {
    private long resourceVersion;

    public long getResourceVersion() {
        return resourceVersion;
    }

    public void setResourceVersion(long resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    public enum PhaseType {
        Installing,
        Ready,
        Deleted,
        Error;
    }

    public enum ConditionType {
        Installing,
        Ready,
        Deleted,
        Error;
    }
}
