package org.bf2.cos.fleetshard.operator.connector;

import java.util.Objects;

import org.bf2.cos.fleetshard.api.DeploymentSpec;
import org.bf2.cos.fleetshard.api.DeploymentSpecAware;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.support.resources.Resources;

import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.processing.event.DefaultEvent;
import io.javaoperatorsdk.operator.processing.event.EventSource;

public class ConnectorSecretEvent extends DefaultEvent {
    public ConnectorSecretEvent(EventSource eventSource, Secret secret) {
        super(resource -> trigger((ManagedConnector) resource, secret), eventSource);
    }

    private static boolean trigger(ManagedConnector connector, Secret resource) {
        String secretUow = resource.getMetadata().getLabels().get(Resources.LABEL_UOW);
        if (secretUow == null) {
            return false;
        }

        //
        // If the status reports that the UoW is the same as the spec, then the
        // augmentation phase based on the current secret has happened so there
        // is no need to trigger a new reconcile
        //
        return Objects.equals(secretUow, getUoW(connector.getSpec()))
            && !Objects.equals(secretUow, getUoW(connector.getStatus()));
    }

    private static String getUoW(DeploymentSpecAware instance) {
        if (instance == null) {
            return null;
        }

        DeploymentSpec spec = instance.getDeployment();
        if (spec == null) {
            return null;
        }

        return spec.getUnitOfWork();
    }
}
