package org.bf2.cos.fleetshard.sync;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = {
    org.bf2.cos.fleet.manager.model.ConnectorDeployment.class,
    org.bf2.cos.fleet.manager.model.ConnectorClusterStatus.class
})
@QuarkusMain
public class Main implements QuarkusApplication {
    @Override
    public int run(String... args) throws Exception {
        Quarkus.waitForExit();
        return 0;
    }

    public static void main(String... args) {
        Quarkus.run(Main.class, args);
    }
}