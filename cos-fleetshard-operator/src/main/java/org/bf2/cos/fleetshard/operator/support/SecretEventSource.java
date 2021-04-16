package org.bf2.cos.fleetshard.operator.support;

import static org.bf2.cos.fleetshard.common.ResourceUtil.objectRef;
import static org.bf2.cos.fleetshard.common.ResourceUtil.ownerUid;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SecretEventSource extends WatcherEventSource<Secret> {
    private final Set<String> labels;

    public SecretEventSource(KubernetesClient client, Collection<String> labels) {
        super(client);

        this.labels = new HashSet<>(labels);
    }

    @Override
    protected Watch watch() {
        var mixed = getClient().secrets();
        labels.forEach(mixed::withLabel);

        return mixed.inNamespace(getClient().getNamespace()).watch(this);
    }

    @Override
    public void eventReceived(Action action, Secret resource) {
        getLogger().info("Event received for action: {}", action.name());
        if (action == Action.ERROR) {
            getLogger().warn("Skipping");
            return;
        }

        if (objectRef(resource) != null && ownerUid(resource) != null) {
            eventHandler.handleEvent(new ObjectReferenceResourceEvent(action, resource, this));
        } else {
            getLogger().info("Event discarded as no object ref has been set");
        }
    }
}
