package org.bf2.cos.fleetshard.support.resources;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.bf2.cos.fleetshard.api.ResourceRef;
import org.bson.types.ObjectId;

import io.fabric8.kubernetes.api.Pluralize;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;

public final class Resources {
    public static final String LABEL_CLUSTER_ID = "cos.bf2.org/cluster.id";
    public static final String LABEL_DEPLOYMENT_ID = "cos.bf2.org/deployment.id";
    public static final String LABEL_CONNECTOR_ID = "cos.bf2.org/connector.id";
    public static final String LABEL_CONNECTOR_TYPE_ID = "cos.bf2.org/connector.type.id";
    public static final String LABEL_CONNECTOR_OPERATOR = "cos.bf2.org/connector.operator";
    public static final String LABEL_DEPLOYMENT_RESOURCE_VERSION = "cos.bf2.org/deployment.resource.version";
    public static final String LABEL_OPERATOR_OWNER = "cos.bf2.org/operator.owner";
    public static final String LABEL_OPERATOR_ASSIGNED = "cos.bf2.org/operator.assigned";
    public static final String LABEL_OPERATOR_TYPE = "cos.bf2.org/operator.type";
    public static final String LABEL_OPERATOR_VERSION = "cos.bf2.org/operator.version";
    public static final String LABEL_UOW = "cos.bf2.org/uow";

    public static final String ANNOTATION_UPDATED_TIMESTAMP = "cos.bf2.org/update.timestamp";

    public static final String CONNECTOR_PREFIX = "mctr-";
    public static final String CONNECTOR_SECRET_SUFFIX = "-config";
    public static final String CONNECTOR_SECRET_DEPLOYMENT_SUFFIX = "-deploy";

    private Resources() {
    }

    public static ResourceRef asRef(HasMetadata from) {
        ResourceRef answer = new ResourceRef();
        answer.setApiVersion(from.getApiVersion());
        answer.setKind(from.getKind());
        answer.setName(from.getMetadata().getName());

        return answer;
    }

    public static String uid() {
        return ObjectId.get().toString();
    }

    public static boolean hasLabel(HasMetadata metadata, String name, String value) {
        Map<String, String> elements = metadata.getMetadata().getLabels();
        return elements != null && Objects.equals(value, elements.get(name));
    }

    public static void setLabel(HasMetadata metadata, String name, String value) {
        KubernetesResourceUtil.getOrCreateLabels(metadata).put(name, value);
    }

    public static boolean hasAnnotation(HasMetadata metadata, String name, String value) {
        Map<String, String> elements = metadata.getMetadata().getAnnotations();
        return elements != null && Objects.equals(value, elements.get(name));
    }

    public static void setAnnotation(HasMetadata metadata, String name, String value) {
        KubernetesResourceUtil.getOrCreateAnnotations(metadata).put(name, value);
    }

    public static ResourceDefinitionContext asResourceDefinitionContext(String apiVersion, String kind) {
        ResourceDefinitionContext.Builder builder = new ResourceDefinitionContext.Builder();
        builder.withNamespaced(true);

        if (apiVersion != null) {
            String[] items = apiVersion.split("/");
            if (items.length == 1) {
                builder.withVersion(items[0]);
            }
            if (items.length == 2) {
                builder.withGroup(items[0]);
                builder.withVersion(items[1]);
            }
        }
        if (kind != null) {
            builder.withKind(kind);
            builder.withPlural(Pluralize.toPlural(kind.toLowerCase(Locale.US)));
        }

        return builder.build();
    }

    public static <T extends HasMetadata> boolean delete(KubernetesClient client, Class<T> type, String namespace,
        String name) {
        Boolean result = client.resources(type)
            .inNamespace(namespace)
            .withName(name)
            .delete();

        return result == null || result;
    }
}
