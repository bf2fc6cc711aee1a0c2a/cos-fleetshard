package org.bf2.cos.fleetshard.operator.camel;

import org.bf2.cos.fleetshard.support.json.JacksonUtil;

import static org.bf2.cos.fleetshard.support.resources.Resources.ANNOTATION_DEPLOYMENT_RESOURCE_VERSION;
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

    public static final String LABELS_TO_TRANSFER = JacksonUtil.asArrayString(
        LABEL_DEPLOYMENT_ID,
        LABEL_CONNECTOR_ID,
        LABEL_CONNECTOR_TYPE_ID);

    public static final String ANNOTATIONS_TO_TRANSFER = JacksonUtil.asArrayString(
        ANNOTATION_DEPLOYMENT_RESOURCE_VERSION);

    private CamelConstants() {
    }
}
