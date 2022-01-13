package org.bf2.cos.fleetshard.operator.camel;

import org.bf2.cos.fleetshard.support.json.JacksonUtil;
import org.bf2.cos.fleetshard.support.resources.Resources;

import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CONNECTOR_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_CONNECTOR_TYPE_ID;
import static org.bf2.cos.fleetshard.support.resources.Resources.LABEL_DEPLOYMENT_ID;

public final class CamelConstants {
    public static final String OPERATOR_TYPE = "camel-connector-operator";
    public static final String CAMEL_GROUP = "camel.apache.org";
    public static final String OPERATOR_RUNTIME = "camel-k";
    public static final String APPLICATION_PROPERTIES = "application.properties";

    public static final String CONNECTOR_TYPE_SOURCE = "source";
    public static final String CONNECTOR_TYPE_SINK = "sink";

    public static final String TRAIT_CAMEL_APACHE_ORG_ENV = "trait.camel.apache.org/environment.%s";
    public static final String TRAIT_CAMEL_APACHE_ORG_CONTAINER_IMAGE = "trait.camel.apache.org/container.image";
    public static final String TRAIT_CAMEL_APACHE_ORG_KAMELETS_ENABLED = "trait.camel.apache.org/kamelets.enabled";
    public static final String TRAIT_CAMEL_APACHE_ORG_JVM_ENABLED = "trait.camel.apache.org/jvm.enabled";
    public static final String TRAIT_CAMEL_APACHE_ORG_LOGGING_JSON = "trait.camel.apache.org/logging.json";
    public static final String TRAIT_CAMEL_APACHE_ORG_OWNER_TARGET_LABELS = "trait.camel.apache.org/owner.target-labels";
    public static final String TRAIT_CAMEL_APACHE_ORG_OWNER_TARGET_ANNOTATIONS = "trait.camel.apache.org/owner.target-annotations";
    public static final String TRAIT_CAMEL_APACHE_ORG_HEALTH_ENABLED = "trait.camel.apache.org/health.enabled";
    public static final String TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_PROBE_ENABLED = "trait.camel.apache.org/health.liveness-probe-enabled";
    public static final String TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_PERIOD = "trait.camel.apache.org/health.liveness-period";
    public static final String TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_TIMEOUT = "trait.camel.apache.org/health.liveness-timeout";
    public static final String TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_SUCCESS_THRESHOLD = "trait.camel.apache.org/health.liveness-success-threshold";
    public static final String TRAIT_CAMEL_APACHE_ORG_HEALTH_LIVENESS_FAILURE_THRESHOLD = "trait.camel.apache.org/health.liveness-failure-threshold";
    public static final String TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_PROBE_ENABLED = "trait.camel.apache.org/health.readiness-probe-enabled";
    public static final String TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_PERIOD = "trait.camel.apache.org/health.readiness-period";
    public static final String TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_TIMEOUT = "trait.camel.apache.org/health.readiness-timeout";
    public static final String TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_SUCCESS_THRESHOLD = "trait.camel.apache.org/health.readiness-success-threshold";
    public static final String TRAIT_CAMEL_APACHE_ORG_HEALTH_READINESS_FAILURE_THRESHOLD = "trait.camel.apache.org/health.readiness-failure-threshold";

    public static final String ERROR_HANDLER_LOG_TYPE = "log";
    public static final String ERROR_HANDLER_DEAD_LETTER_CHANNEL_TYPE = "dead-letter-channel";
    public static final String ERROR_HANDLER_DEAD_LETTER_CHANNEL_KAMELET = "managed-kafka-sink";
    public static final String ERROR_HANDLER_DEAD_LETTER_CHANNEL_KAMELET_ID = "error";
    public static final String ERROR_HANDLER_STOP_URI = "controlbus:route?routeId=current&action=stop";

    public static final String LABELS_TO_TRANSFER = JacksonUtil.asArrayString(
        LABEL_DEPLOYMENT_ID,
        LABEL_CONNECTOR_ID,
        LABEL_CONNECTOR_TYPE_ID,
        Resources.LABEL_KCP_TARGET_CLUSTER_ID);

    private CamelConstants() {
    }
}
