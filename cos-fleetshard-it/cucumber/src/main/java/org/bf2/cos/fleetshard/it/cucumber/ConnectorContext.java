package org.bf2.cos.fleetshard.it.cucumber;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.text.StringSubstitutor;
import org.assertj.core.util.Strings;
import org.bf2.cos.fleetshard.api.ManagedConnector;

import io.cucumber.datatable.DataTable;
import io.fabric8.kubernetes.api.model.Secret;

@ApplicationScoped
public class ConnectorContext {
    public static final String COS_OPERATORS_NAMESPACE = "cos.operators.namespace";
    public static final String COS_CONNECTORS_NAMESPACE = "cos.connectors.namespace";
    public static final String COS_CLUSTER_ID = "cos.cluster.id";
    public static final String COS_OPERATOR_ID = "cos.operator.id";
    public static final String COS_OPERATOR_VERSION = "cos.operator.version";
    public static final String COS_DEPLOYMENT_ID = "cos.deployment.id";
    public static final String COS_DEPLOYMENT_RESOURCE_VERSION = "cos.deployment.resource-version";
    public static final String COS_CONNECTOR_ID = "cos.connector.id";
    public static final String COS_CONNECTOR_RESOURCE_VERSION = "cos.connector.resource-version";
    public static final String COS_MANAGED_CONNECTOR_NAME = "cos.managed.connector.name";
    public static final String COS_MANAGED_CONNECTOR_SECRET_NAME = "cos.managed.connector.secret.name";
    public static final String PLACEHOLDER_IGNORE = "${cos.ignore}";

    public static final String OPERATOR_TYPE = "operator.type";
    public static final String OPERATOR_ID = "operator.id";
    public static final String OPERATOR_VERSION = "operator.version";
    public static final String CONNECTOR_TYPE_ID = "connector.type.id";
    public static final String DESIRED_STATE = "desired.state";

    private final List<ManagedConnector> history = new CopyOnWriteArrayList<>();

    @Inject
    private CosFeatureContext cosCtx;

    private ManagedConnector connector;
    private Secret secret;

    public List<ManagedConnector> history() {
        return history;
    }

    public ManagedConnector connector() {
        return connector;
    }

    public synchronized void connector(ManagedConnector connector) {
        this.connector = connector;
    }

    public synchronized Secret secret() {
        return secret;
    }

    public synchronized void secret(Secret secret) {
        this.secret = secret;
    }

    public synchronized void clear() {
        this.connector = null;
        this.secret = null;
        this.history.clear();
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
        properties.put(COS_OPERATORS_NAMESPACE, cosCtx.getOperatorsNamespace());
        properties.put(COS_CONNECTORS_NAMESPACE, cosCtx.getConnectorsNamespace());
        properties.put(COS_CLUSTER_ID, clusterId());
        properties.put(COS_OPERATOR_ID, operatorId());
        properties.put(COS_OPERATOR_VERSION, operatorVersion());
        properties.put(COS_DEPLOYMENT_ID, connector().getSpec().getDeploymentId());
        properties.put(COS_DEPLOYMENT_RESOURCE_VERSION, connector().getSpec().getDeployment().getDeploymentResourceVersion());
        properties.put(COS_CONNECTOR_ID, connector().getSpec().getConnectorId());
        properties.put(COS_CONNECTOR_RESOURCE_VERSION, connector().getSpec().getDeployment().getConnectorResourceVersion());
        properties.put(COS_MANAGED_CONNECTOR_NAME, connector().getMetadata().getName());
        properties.put(COS_MANAGED_CONNECTOR_SECRET_NAME, secret().getMetadata().getName());

        return new StringSubstitutor(properties).replace(in);
    }

    public Map<String, String> resolvePlaceholders(DataTable in) {
        return resolvePlaceholders(in.asMap(String.class, String.class));
    }

    public Map<String, String> resolvePlaceholders(Map<String, String> in) {
        Map<String, String> answer = new HashMap<>();
        in.forEach((k, v) -> {
            if (!Strings.isNullOrEmpty(v) && PLACEHOLDER_IGNORE.equals(v)) {
                v = resolvePlaceholders(v);
            }

            answer.put(k, resolvePlaceholders(v));
        });

        return Collections.unmodifiableMap(answer);
    }
}
