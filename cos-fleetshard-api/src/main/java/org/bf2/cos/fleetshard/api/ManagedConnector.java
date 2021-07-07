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
@Version(ManagedConnector.VERSION)
@Group(ManagedConnector.GROUP)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ShortNames("mcs")
public class ManagedConnector
    extends CustomResource<ManagedConnectorSpec, ManagedConnectorStatus>
    implements Namespaced {

    public static final String VERSION = "v1alpha1";
    public static final String GROUP = "cos.bf2.org";
    public static final String LABEL_DEPLOYMENT_ID = "cos.bf2.org/deployment.id";
    public static final String LABEL_CONNECTOR_ID = "cos.bf2.org/connector.id";
    public static final String LABEL_CONNECTOR_TYPE_ID = "cos.bf2.org/connector.type.id";
    public static final String LABEL_CONNECTOR_NAME = "cos.bf2.org/connector.name";
    public static final String LABEL_CONNECTOR_META = "cos.bf2.org/connector.meta";
    public static final String LABEL_CONNECTOR_GENERATED = "cos.bf2.org/connector.generated";
    public static final String LABEL_CONNECTOR_OPERATOR = "cos.bf2.org/connector.operator";
    public static final String ANNOTATION_DELETION_MODE = "cos.bf2.org/resource.deletion.mode";
    public static final String DELETION_MODE_CONNECTOR = "connector";
    public static final String DELETION_MODE_DEPLOYMENT = "deployment";
    public static final String DESIRED_STATE_READY = "ready";
    public static final String DESIRED_STATE_DELETED = "deleted";
    public static final String DESIRED_STATE_STOPPED = "stopped";

    public ManagedConnector() {
        setSpec(new ManagedConnectorSpec());
        setStatus(new ManagedConnectorStatus());
    }
}
