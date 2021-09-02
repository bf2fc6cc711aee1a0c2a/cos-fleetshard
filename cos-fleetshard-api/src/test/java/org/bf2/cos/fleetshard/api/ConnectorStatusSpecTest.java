package org.bf2.cos.fleetshard.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConnectorStatusSpecTest {

    @Test
    void resourceAdd() {
        var spec = new ConnectorStatusSpec();

        var res = new DeployedResource();
        res.setName("mctr-612cbb43e4b0d0ef42e2e5cb");
        res.setNamespace("default");
        res.setApiVersion("camel.apache.org/v1alpha1");
        res.setKind("KameletBinding");
        res.setDeploymentRevision(872L);

        spec.addOrUpdateResource(res);
        assertThat(spec.getResources()).hasSize(1);

        spec.addOrUpdateResource(res);
        assertThat(spec.getResources()).hasSize(1);

    }
}
