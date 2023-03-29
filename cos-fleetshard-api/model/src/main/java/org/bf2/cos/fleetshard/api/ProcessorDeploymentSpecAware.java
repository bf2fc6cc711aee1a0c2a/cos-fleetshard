package org.bf2.cos.fleetshard.api;

public interface ProcessorDeploymentSpecAware {
    ProcessorDeploymentSpec getDeployment();

    void setDeployment(ProcessorDeploymentSpec deployment);
}
