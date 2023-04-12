package org.bf2.cos.fleetshard.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder", refs = @BuildableReference(CustomResource.class),
    editableEnabled = false)
@Version(ManagedConnector.VERSION)
@Group(ManagedConnector.GROUP)
@ShortNames("mpsr")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManagedProcessor extends CustomResource<ManagedProcessorSpec, ManagedProcessorStatus>
    implements Namespaced {

    @Override
    protected ManagedProcessorSpec initSpec() {
        return new ManagedProcessorSpec();
    }

    @Override
    protected ManagedProcessorStatus initStatus() {
        return new ManagedProcessorStatus();
    }
}
