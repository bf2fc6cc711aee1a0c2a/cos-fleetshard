package org.bf2.cos.fleetshard.operator;

import javax.inject.Inject;

import io.javaoperatorsdk.operator.Operator;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Main implements QuarkusApplication {

    @Inject
    Operator operator;

    public static void main(String... args) {
        Quarkus.run(Main.class, args);
    }

    @Override
    public int run(String... args) throws Exception {
        try {
            operator.start();
            Quarkus.waitForExit();
        } finally {
            operator.close();
        }

        return 0;
    }
}
