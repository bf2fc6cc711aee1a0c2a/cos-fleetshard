package org.bf2.cos.fleetshard.api.connector;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.HasMetadata;

@JsonDeserialize
public interface Connector extends HasMetadata {
}
