package org.bf2.cos.fleetshard.support.resources;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.bf2.cos.fleetshard.api.ResourceRef;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.Pluralize;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;

public final class Resources {
    private static final Logger LOGGER = LoggerFactory.getLogger(Resources.class);

    public static final String LABEL_CLUSTER_ID = "cos.bf2.org/cluster.id";
    public static final String LABEL_NAMESPACE_ID = "cos.bf2.org/namespace.id";
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
    public static final String ANNOTATION_NAMESPACE_NAME = "cos.bf2.org/namespace.name";
    public static final String ANNOTATION_NAMESPACE_EXPIRATION = "cos.bf2.org/namespace.expiration";
    public static final String ANNOTATION_NAMESPACE_QUOTA = "cos.bf2.org/namespace.quota";
    public static final String ANNOTATION_NAMESPACE_RESOURCE_VERSION = "cos.bf2.org/namespace.resource.version";
    public static final String LABEL_NAMESPACE_STATE = "cos.bf2.org/namespace.state";
    public static final String LABEL_NAMESPACE_STATE_FORCED = "cos.bf2.org/namespace.state.forced";

    public static final String LABEL_NAMESPACE_TENANT_KIND = "cos.bf2.org/namespace.tenant.kind";
    public static final String LABEL_NAMESPACE_TENANT_ID = "cos.bf2.org/namespace.tenant.id";

    public static final String CONNECTOR_PREFIX = "mctr-";
    public static final String CONNECTOR_SECRET_SUFFIX = "-config";
    public static final String CONNECTOR_SECRET_DEPLOYMENT_SUFFIX = "-deploy";

    public static final String LABEL_KCP_TARGET_CLUSTER_ID = "kcp.dev/cluster";

    public static final String LABEL_KUBERNETES_NAME = "app.kubernetes.io/name";
    public static final String LABEL_KUBERNETES_INSTANCE = "app.kubernetes.io/instance";
    public static final String LABEL_KUBERNETES_VERSION = "app.kubernetes.io/version";
    public static final String LABEL_KUBERNETES_COMPONENT = "app.kubernetes.io/component";
    public static final String LABEL_KUBERNETES_PART_OF = "app.kubernetes.io/part-of";
    public static final String LABEL_KUBERNETES_MANAGED_BY = "app.kubernetes.io/managed-by";
    public static final String LABEL_KUBERNETES_CREATED_BY = "app.kubernetes.io/created-by";

    public static final String COMPONENT_CLUSTER = "cos-cluster";
    public static final String COMPONENT_OPERATOR = "cos-operator";
    public static final String COMPONENT_CONNECTOR = "cos-connector";
    public static final String COMPONENT_NAMESPACE = "cos-namespace";

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

    public static String uid(String prefix) {
        return prefix + uid();
    }

    public static boolean hasLabel(HasMetadata metadata, String name, String value) {
        Map<String, String> elements = metadata.getMetadata().getLabels();
        return elements != null && Objects.equals(value, elements.get(name));
    }

    public static void setLabel(HasMetadata metadata, String name, String value) {
        if (value != null) {
            KubernetesResourceUtil.getOrCreateLabels(metadata).put(name, value);
        }
    }

    public static void setLabels(HasMetadata metadata, String name, String value, String... keyVals) {
        Map<String, String> labels = KubernetesResourceUtil.getOrCreateLabels(metadata);
        labels.put(name, value);

        for (int i = 0; i < keyVals.length; i += 2) {
            labels.put(keyVals[i], keyVals[i + 1]);
        }
    }

    public static String getLabel(HasMetadata metadata, String name) {
        Map<String, String> labels = metadata.getMetadata().getLabels();
        if (labels != null) {
            return labels.get(name);
        }

        return null;
    }

    public static String getLabel(HasMetadata metadata, String name, Supplier<String> defaultValueSupplier) {
        String answer = getLabel(metadata, name);
        return answer != null ? answer : defaultValueSupplier.get();
    }

    public static void copyLabel(String name, HasMetadata source, HasMetadata target) {
        setLabel(target, name, getLabel(source, name));
    }

    public static boolean hasAnnotation(HasMetadata metadata, String name, String value) {
        Map<String, String> elements = metadata.getMetadata().getAnnotations();
        return elements != null && Objects.equals(value, elements.get(name));
    }

    public static void setAnnotation(HasMetadata metadata, String name, String value) {
        if (value != null) {
            KubernetesResourceUtil.getOrCreateAnnotations(metadata).put(name, value);
        }
    }

    public static void setAnnotations(HasMetadata metadata, String name, String value, String... keyVals) {
        Map<String, String> annotations = KubernetesResourceUtil.getOrCreateAnnotations(metadata);
        if (value != null) {
            annotations.put(name, value);
        }

        for (int i = 0; i < keyVals.length; i += 2) {
            if (keyVals[i + 1] == null) {
                continue;
            }

            annotations.put(keyVals[i], keyVals[i + 1]);
        }
    }

    public static String getAnnotation(HasMetadata metadata, String name) {
        Map<String, String> annotations = metadata.getMetadata().getAnnotations();
        if (annotations != null) {
            return annotations.get(name);
        }

        return null;
    }

    public static void copyAnnotation(String name, HasMetadata source, HasMetadata target) {
        setAnnotation(target, name, getAnnotation(source, name));
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

    public static <T extends HasMetadata> boolean delete(
        KubernetesClient client,
        Class<T> type,
        String namespace,
        String name) {

        Boolean result = client.resources(type)
            .inNamespace(namespace)
            .withName(name)
            .delete();

        if (result == null || result) {
            return true;
        }

        return client.resources(type)
            .inNamespace(namespace)
            .withName(name)
            .get() == null;
    }

    public static void setOwnerReferences(HasMetadata target, HasMetadata owner) {
        if (owner == null) {
            return;
        }

        OwnerReference ref = new OwnerReference();
        ref.setApiVersion(owner.getApiVersion());
        ref.setKind(owner.getKind());
        ref.setName(owner.getMetadata().getName());
        ref.setUid(owner.getMetadata().getUid());
        ref.setBlockOwnerDeletion(true);

        KubernetesResourceUtil.getOrCreateMetadata(target).setOwnerReferences(List.of(ref));
    }

    public static void closeQuietly(final AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                LOGGER.debug("Exception closing: {}", closeable, e);
            }
        }
    }

    public static String computeTraitsChecksum(HasMetadata resource) {
        Checksum crc32 = new CRC32();

        var annotations = resource.getMetadata().getAnnotations();
        if (annotations != null) {
            annotations.entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith("trait.camel.apache.org/"))
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    byte[] k = e.getKey().getBytes(StandardCharsets.UTF_8);
                    byte[] v = e.getValue().getBytes(StandardCharsets.UTF_8);

                    crc32.update(k, 0, k.length);
                    crc32.update(v, 0, v.length);
                });
        }

        return Long.toHexString(crc32.getValue());
    }
}
