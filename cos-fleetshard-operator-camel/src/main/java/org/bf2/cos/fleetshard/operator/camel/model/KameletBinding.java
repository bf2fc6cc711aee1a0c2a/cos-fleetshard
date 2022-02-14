package org.bf2.cos.fleetshard.operator.camel.model;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.fabric8.kubernetes.api.Pluralize;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder", refs = @BuildableReference(CustomResource.class),
    editableEnabled = false)
@Version(KameletBinding.RESOURCE_VERSION)
@Group(KameletBinding.RESOURCE_GROUP)
public class KameletBinding
    extends CustomResource<KameletBindingSpec, KameletBindingStatus>
    implements Namespaced {

    public static final String RESOURCE_GROUP = "camel.apache.org";
    public static final String RESOURCE_VERSION = "v1alpha1";
    public static final String RESOURCE_API_VERSION = RESOURCE_GROUP + "/" + RESOURCE_VERSION;
    public static final String RESOURCE_KIND = "KameletBinding";

    public static final ResourceDefinitionContext RESOURCE_DEFINITION = new ResourceDefinitionContext.Builder()
        .withNamespaced(true)
        .withGroup(RESOURCE_GROUP)
        .withVersion(RESOURCE_VERSION)
        .withKind(RESOURCE_KIND)
        .withPlural(Pluralize.toPlural(RESOURCE_KIND.toLowerCase(Locale.US)))
        .build();

    public KameletBinding() {
        super();
    }

    @Override
    protected KameletBindingSpec initSpec() {
        return new KameletBindingSpec();
    }

    @Override
    protected KameletBindingStatus initStatus() {
        return new KameletBindingStatus();
    }
}
