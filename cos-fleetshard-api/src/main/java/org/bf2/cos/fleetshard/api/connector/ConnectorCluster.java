package org.bf2.cos.fleetshard.api.connector;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder", refs = @BuildableReference(CustomResource.class), editableEnabled = false)
@Version("v1alpha1")
@Group("cos.bf2.org")
public class ConnectorCluster
        extends CustomResource<ConnectorClusterSpec, ConnectorClusterStatus>
        implements Namespaced {

    @JsonIgnore
    public boolean isReady() {
        return getStatus().isInPhase(ConnectorClusterStatus.PhaseType.Ready);
    }
}
