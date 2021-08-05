package org.bf2.cos.fleetshard.operator.camel.model;

import java.util.Locale;

import io.fabric8.kubernetes.api.Pluralize;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.model.Scope;

public class Kamelet {
    public static final String RESOURCE_GROUP = "camel.apache.org";
    public static final String RESOURCE_VERSION = "v1alpha1";
    public static final String RESOURCE_API_VERSION = RESOURCE_GROUP + "/" + RESOURCE_VERSION;
    public static final String RESOURCE_KIND = "Kamelet";
    public static final String TYPE_SOURCE = "source";
    public static final String TYPE_SINK = "sink";

    public static final CustomResourceDefinitionContext RESOURCE_DEFINITION = new CustomResourceDefinitionContext.Builder()
        .withScope(Scope.NAMESPACED.value())
        .withGroup(RESOURCE_GROUP)
        .withVersion(RESOURCE_VERSION)
        .withKind(RESOURCE_KIND)
        .withPlural(Pluralize.toPlural(RESOURCE_KIND.toLowerCase(Locale.US)))
        .build();
}
