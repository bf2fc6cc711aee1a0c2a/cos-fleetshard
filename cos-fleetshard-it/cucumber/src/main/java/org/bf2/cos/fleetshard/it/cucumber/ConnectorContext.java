package org.bf2.cos.fleetshard.it.cucumber;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.text.StringSubstitutor;
import org.assertj.core.util.Strings;
import org.bf2.cos.fleetshard.api.ManagedConnector;

import io.cucumber.datatable.DataTable;
import io.fabric8.kubernetes.api.model.Secret;

@ApplicationScoped
public class ConnectorContext {
    @Inject
    private CosFeatureContext cosCtx;

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
        return cosCtx.getClusterId();
    }

    public String operatorId() {
        return cosCtx.getOperatorId();
    }

    public String operatorVersion() {
        return cosCtx.getOperatorVersion();
    }

    public String connectorsNamespace() {
        return cosCtx.getConnectorsNamespace();
    }

    public String operatorsNamespace() {
        return cosCtx.getOperatorsNamespace();
    }

    public String resolvePlaceholders(String in) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("cos.operators.namespace", cosCtx.getOperatorsNamespace());
        properties.put("cos.connectors.namespace", cosCtx.getConnectorsNamespace());
        properties.put("cos.cluster.id", clusterId());
        properties.put("cos.operator.id", operatorId());
        properties.put("cos.operator.version", operatorVersion());
        properties.put("cos.deployment.id", connector().getSpec().getDeploymentId());
        properties.put("cos.deployment.resource-version", connector().getSpec().getDeployment().getDeploymentResourceVersion());
        properties.put("cos.connector.id", connector().getSpec().getConnectorId());
        properties.put("cos.connector.resource-version", connector().getSpec().getDeployment().getConnectorResourceVersion());
        properties.put("cos.managed.connector.name", connector().getMetadata().getName());
        properties.put("cos.managed.connector.secret.name", secret().getMetadata().getName());

        return new StringSubstitutor(properties).replace(in);
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
