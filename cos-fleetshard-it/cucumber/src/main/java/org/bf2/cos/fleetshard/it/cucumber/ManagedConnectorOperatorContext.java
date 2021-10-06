package org.bf2.cos.fleetshard.it.cucumber;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;

@ApplicationScoped
public class ManagedConnectorOperatorContext {
    @Inject
    private CosFeatureContext cosCtx;

    private volatile ManagedConnectorOperator managedConnectorOperator;

    public ManagedConnectorOperator managedConnectorOperator() {
        return managedConnectorOperator;
    }

    public void managedConnectorOperator(ManagedConnectorOperator managedConnectorOperator) {
        this.managedConnectorOperator = managedConnectorOperator;
    }

    public void clear() {
        this.managedConnectorOperator = null;
    }

    public String namespace() {
        return cosCtx.getNamespace();
    }
}
