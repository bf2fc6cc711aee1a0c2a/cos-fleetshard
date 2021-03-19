///usr/bin/env jbang "$0" "$@" ; exit $?
//
//DEPS org.slf4j:slf4j-simple:1.7.30
//DEPS org.bf2:cos-fleetshard-api:1.0.0-SNAPSHOT
//DEPS org.bf2:cos-fleetshard-common:1.0.0-SNAPSHOT
//DEPS org.bf2:cos-fleetshard-common:1.0.0-SNAPSHOT
//

import java.io.File;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.utils.Pluralize;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.kubernetes.model.Scope;

import org.bf2.cos.fleetshard.common.ResourceUtil;

public class client {
    public static void main(String... args) throws Exception {
        try (KubernetesClient kubernetesClient = new DefaultKubernetesClient()) {
            var mapper =  Serialization.jsonMapper();
            var node = Serialization.jsonMapper().readTree(new File(args[0]));

            var context = ResourceUtil.asCustomResourceDefinitionContext(node);
            var output = kubernetesClient.customResource(context)
                .inNamespace(kubernetesClient.getNamespace())
                .createOrReplace(
                    mapper.writeValueAsString(node)
                );

            System.out.println(
                "in  : " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)
            );
            System.out.println(
                "out : " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(output)
            );
        }
    }
}
