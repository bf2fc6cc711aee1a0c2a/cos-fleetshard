package org.bf2.cos.fleetshard.it.cucumber;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.api.model.Secret;

@ApplicationScoped
public class ConnectorContext {
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

    private volatile ManagedConnector connector;
    private volatile Secret secret;

    public ManagedConnector connector() {
        return connector;
    }

    public void connector(ManagedConnector connector) {
        this.connector = connector;
    }

    public Secret secret() {
        return secret;
    }

    public void secret(Secret secret) {
        this.secret = secret;
    }

    public void clear() {
        this.connector = null;
        this.secret = null;
    }

    public String clusterId() {
        return clusterId;
    }

    public String operatorId() {
        return operatorId;
    }

    public String operatorVersion() {
        return operatorVersion;
    }

    public String namespace() {
        return namespace;
    }
}
