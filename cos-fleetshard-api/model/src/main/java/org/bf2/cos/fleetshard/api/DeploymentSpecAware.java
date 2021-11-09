package org.bf2.cos.fleetshard.api;

public interface DeploymentSpecAware {
    DeploymentSpec getDeployment();

    void setDeployment(DeploymentSpec deployment);
}
