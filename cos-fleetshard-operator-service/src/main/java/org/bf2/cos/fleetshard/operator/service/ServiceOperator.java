package org.bf2.cos.fleetshard.operator.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.javaoperatorsdk.operator.Operator;

@ApplicationScoped
public class ServiceOperator {
    @Inject
    Operator operator;

    public void start() {
        operator.start();
    }

    public void stop() {
        operator.stop();
    }
}
