package org.bf2.cos.fleetshard.api.connector.support;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder", refs = @BuildableReference(io.fabric8.kubernetes.api.model.Condition.class))
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Condition extends io.fabric8.kubernetes.api.model.Condition {

    @JsonIgnore
    public void setType(Enum<?> type) {
        setType(type.name());
    }

    public boolean is(String type) {
        return Objects.equals(getType(), type);
    }

    public boolean is(Enum<?> type) {
        return Objects.equals(getType(), type.name());
    }
}
