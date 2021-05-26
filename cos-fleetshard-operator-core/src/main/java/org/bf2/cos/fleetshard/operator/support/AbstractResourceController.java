package org.bf2.cos.fleetshard.operator.support;

import io.fabric8.kubernetes.client.CustomResource;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;

public abstract class AbstractResourceController<R extends CustomResource> implements ResourceController<R> {
    @Override
    public DeleteControl deleteResource(R resource, Context<R> context) {
        return DeleteControl.DEFAULT_DELETE;
    }
}
