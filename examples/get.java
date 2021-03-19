///usr/bin/env jbang "$0" "$@" ; exit $?
//
//DEPS org.slf4j:slf4j-simple:1.7.30
//DEPS io.fabric8:kubernetes-client:5.2.1
//

import java.util.Locale;
import java.util.Map;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.utils.Pluralize;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.kubernetes.model.Scope;

public class get {
    public static void main(String... args) throws Exception {
        try (KubernetesClient kc = new DefaultKubernetesClient()) {
            var mapper =  Serialization.jsonMapper();
            var context = new CustomResourceDefinitionContext.Builder()
                .withScope(Scope.NAMESPACED.value())
                .withGroup(args[0])
                .withVersion(args[1])
                .withKind(args[2])
                .withPlural(Pluralize.toPlural(args[2].toLowerCase(Locale.US)))
                .build();

            System.out.println(
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(context)
            );

            Map<String, Object> untyped;            
            if (args.length == 4) {
                untyped = kc.customResource(context).inNamespace(kc.getNamespace()).get(kc.getNamespace(), args[3]);
            } else {
                untyped = kc.customResource(context).inNamespace(kc.getNamespace()).list();
            }
            
            System.out.println(
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(untyped)
            );
        }
    }
}
