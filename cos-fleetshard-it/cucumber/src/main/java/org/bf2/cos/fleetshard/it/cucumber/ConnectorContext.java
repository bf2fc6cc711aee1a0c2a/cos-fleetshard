package org.bf2.cos.fleetshard.it.cucumber;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.text.StringSubstitutor;
import org.assertj.core.util.Strings;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.cucumber.datatable.DataTable;
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

    public String resolvePlaceholders(String in) {
        StringSubstitutor sub = new StringSubstitutor(Map.of(
            "namespace", namespace,
            "cos.cluster.id", clusterId,
            "cos.operator.id", operatorId,
            "cos.operator.version", operatorVersion,
            "cos.deployment.id", connector().getSpec().getDeploymentId(),
            "cos.deployment.resource-version", connector().getSpec().getDeployment().getDeploymentResourceVersion(),
            "cos.connector.id", connector().getSpec().getConnectorId(),
            "cos.connector.resource-version", connector().getSpec().getDeployment().getConnectorResourceVersion(),
            "cos.managed.connector.name", connector().getMetadata().getName(),
            "cos.managed.connector.secret.name", secret().getMetadata().getName()));

        return sub.replace(in);
    }

    public Map<String, String> resolvePlaceholders(DataTable in) {
        return resolvePlaceholders(in.asMap(String.class, String.class));
    }

    public Map<String, String> resolvePlaceholders(Map<String, String> in) {
        Map<String, String> answer = new HashMap<>();
        in.forEach((k, v) -> {
            if (!Strings.isNullOrEmpty(v) && "${cos.ignore}".equals(v)) {
                v = resolvePlaceholders(v);
            }

            answer.put(k, resolvePlaceholders(v));
        });

        return Collections.unmodifiableMap(answer);
    }
}
