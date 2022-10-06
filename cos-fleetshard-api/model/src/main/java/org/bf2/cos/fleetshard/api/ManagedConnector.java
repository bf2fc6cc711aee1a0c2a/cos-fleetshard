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
@JsonInclude(JsonInclude.Include.NON_NULL)
@ShortNames("mctr")
public class ManagedConnector
    extends CustomResource<ManagedConnectorSpec, ManagedConnectorStatus>
    implements Namespaced {

    public static final String VERSION = "v1alpha1";
    public static final String GROUP = "cos.bf2.org";
    public static final String API_VERSION = GROUP + "/" + VERSION;

    public static final String DESIRED_STATE_READY = "ready";
    public static final String DESIRED_STATE_DELETED = "deleted";
    public static final String DESIRED_STATE_UNASSIGNED = "unassigned";
    public static final String DESIRED_STATE_STOPPED = "stopped";

    public static final String STATE_PROVISIONING = "provisioning";
    public static final String STATE_DE_PROVISIONING = "deprovisioning";
    public static final String STATE_DELETED = "deleted";
    public static final String STATE_STOPPED = "stopped";
    public static final String STATE_FAILED = "failed";
    public static final String STATE_READY = "ready";

    @Override
    protected ManagedConnectorSpec initSpec() {
        return new ManagedConnectorSpec();
    }

    @Override
    protected ManagedConnectorStatus initStatus() {
        return new ManagedConnectorStatus();
    }
}
