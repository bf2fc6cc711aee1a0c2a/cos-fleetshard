package org.bf2.cos.fleetshard.it.cucumber;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

@ApplicationScoped
public class CosFeatureContext {
    final String clusterId = uid();

    @Inject
    @ConfigProperty(name = "cos.operator.id")
    String operatorId;
    @Inject
    @ConfigProperty(name = "cos.operator.version")
    String operatorVersion;
    @Inject
    @ConfigProperty(name = "cos.namespace")
    String operatorsNamespace;

    String connectorsNamespace = "connectors-ns-" + uid();

    public String getClusterId() {
        return clusterId;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public String getOperatorVersion() {
        return operatorVersion;
    }

    public String getConnectorsNamespace() {
        return connectorsNamespace;
    }

    public String getOperatorsNamespace() {
        return operatorsNamespace;
    }

}
