package org.bf2.cos.fleetshard.operator.support;

import io.fabric8.kubernetes.client.CustomResource;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;

public abstract class AbstractResourceController<R extends CustomResource<?, ?>> implements Reconciler<R> {

    protected AbstractResourceController() {
    }
}
