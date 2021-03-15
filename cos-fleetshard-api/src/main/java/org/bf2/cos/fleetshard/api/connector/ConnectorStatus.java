package org.bf2.cos.fleetshard.api.connector;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.sundr.builder.annotations.Buildable;
import org.bf2.cos.fleetshard.api.connector.support.Status;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class ConnectorStatus extends Status {

    public enum PhaseType {
        Installing,
        Ready,
        Deleted,
        Error;
    }

    public enum ConditionType {
        Installing,
        Validating,
        Augmenting,
        Running,
        Paused,
        Deleted,
        Error;
    }
}
