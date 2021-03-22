package org.bf2.cos.fleetshard.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.Condition;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder", refs = @BuildableReference(Condition.class))
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceCondition extends Condition {
    private ResourceRef ref;

    public ResourceCondition() {
    }

    public ResourceCondition(ResourceRef ref) {
        this.ref = ref;
    }

    public ResourceCondition(Condition condition, ResourceRef ref) {
        super(condition.getLastTransitionTime(), condition.getMessage(), null, condition.getReason(), condition.getStatus(),
                condition.getType());
        this.ref = ref;
    }

    public ResourceRef getRef() {
        return ref;
    }

    public void setRef(ResourceRef ref) {
        this.ref = ref;
    }
}
