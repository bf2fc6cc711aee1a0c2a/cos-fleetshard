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

@ToString
@EqualsAndHashCode(
    callSuper = true)
@Buildable(
    builderPackage = "io.fabric8.kubernetes.api.builder",
    refs = @BuildableReference(CustomResource.class),
    editableEnabled = false)
@Version(ManagedConnectorOperator.VERSION)
@Group(ManagedConnectorOperator.GROUP)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ShortNames("mco")
public class ManagedConnectorOperator
    extends CustomResource<ManagedConnectorOperatorSpec, ManagedConnectorOperatorStatus>
    implements Namespaced {

    public static final String VERSION = "v1alpha1";
    public static final String GROUP = "cos.bf2.org";

    public ManagedConnectorOperator() {
        setSpec(new ManagedConnectorOperatorSpec());
        setStatus(new ManagedConnectorOperatorStatus());
    }
}
