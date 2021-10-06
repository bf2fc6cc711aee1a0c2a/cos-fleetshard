package org.bf2.cos.fleetshard.it.cucumber;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CosFeatureContext {
    @Inject
    @ConfigProperty(name = "cos.cluster.id")
    String clusterId;
    @Inject
    @ConfigProperty(name = "cos.operator.id")
    String operatorId;
    @Inject
    @ConfigProperty(name = "cos.operator.version")
    String operatorVersion;
    @Inject
    @ConfigProperty(name = "test.namespace")
    String namespace;

    public String getClusterId() {
        return clusterId;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public String getOperatorVersion() {
        return operatorVersion;
    }

    public String getNamespace() {
        return namespace;
    }
}
