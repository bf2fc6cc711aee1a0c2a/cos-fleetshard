package org.bf2.cos.fleetshard.operator.e2e.glues;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import javax.inject.Inject;

import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;
import org.assertj.core.util.Strings;
import org.bson.types.ObjectId;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

import static org.assertj.core.api.Assertions.assertThat;

public class Steps {
    public static final String PLACEHOLDER_IGNORE = "${cos.ignore}";
    public static final String PLACEHOLDER_UID = "${cos.uid}";

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    Awaiter awaiter;

    public volatile String namespace;
    public volatile Map<String, String> properties = new HashMap<>();

    @Given("^E2E configuration$")
    public void configure(Map<String, String> configuration) {
        if (configuration.containsKey("atMost")) {
            awaiter.atMost = Long.parseLong(configuration.get("atMost"));
        }
        if (configuration.containsKey("pollDelay")) {
            awaiter.pollDelay = Long.parseLong(configuration.get("pollDelay"));
        }
        if (configuration.containsKey("pollInterval")) {
            awaiter.pollInterval = Long.parseLong(configuration.get("pollInterval"));
        }
        if (configuration.containsKey("namespace")) {
            namespace = configuration.get("namespace");
        }
    }

    @When("scale component {string} to {int} replicas")
    public void component_scale(String name, int replicas) {
        kubernetesClient.apps()
            .deployments()
            .inNamespace(namespace)
            .withName(name)
            .edit(deployment -> {
                deployment.getSpec().setReplicas(replicas);
                return deployment;
            });
    }

    @Then("the component {string} has {int} replicas")
    public void component_has_replicas(String name, int replicas) {
        awaiter.until(() -> {
            var deployment = kubernetesClient.apps()
                .deployments()
                .inNamespace(namespace)
                .withName(name)
                .get();

            if (deployment == null) {
                return false;
            }
            if (deployment.getStatus() == null) {
                return replicas == 0;
            }
            if (deployment.getStatus().getReplicas() == null) {
                return replicas == 0;
            }

            return deployment.getStatus().getReplicas() == replicas;
        });
    }

    @Then("the component {string} own a lease and store the leader identity as {string}")
    public void component_lease(String name, String property) {
        awaiter.until(() -> {
            var lease = kubernetesClient.leases()
                .inNamespace(namespace)
                .withName(name)
                .get();

            if (lease == null) {
                return false;
            }
            if (lease.getSpec() == null) {
                return false;
            }
            if (lease.getSpec().getHolderIdentity() == null) {
                return false;
            }

            // This is not ideal, but we must be sure we don't have stale information
            // hence we must ensure the leas owner is a valid pod
            var pods = kubernetesClient.pods()
                .inNamespace(namespace)
                .withLabel("app.kubernetes.io/name", name)
                .list();

            if (pods == null) {
                return false;
            }
            if (pods.getItems() == null) {
                return false;
            }

            for (var pod : pods.getItems()) {
                // Only running PODs must be taken into account. This is likely not needed
                // when running the test from a clean se-up but in case the test is repeated,
                // some pods may have been left over.
                if (!Objects.equals(pod.getStatus().getPhase(), "Running")) {
                    continue;
                }

                if (Objects.equals(pod.getMetadata().getName(), lease.getSpec().getHolderIdentity())) {
                    properties.put(property, lease.getSpec().getHolderIdentity());
                    return true;
                }
            }

            return false;
        });
    }

    @Then("the component pod {string} has condition {string} with status {string}")
    public void component_pods_condition(String podName, String conditionType, String conditionStatus) {
        Objects.requireNonNull(podName);
        Objects.requireNonNull(conditionType);
        Objects.requireNonNull(conditionStatus);

        awaiter.until(() -> {
            String name = resolver().resolve(podName);

            var pod = kubernetesClient.pods()
                .inNamespace(namespace)
                .withName(name)
                .get();

            if (pod == null) {
                return false;
            }
            if (pod.getStatus() == null) {
                return false;
            }
            if (pod.getStatus().getConditions() == null) {
                return false;
            }

            for (var condition : pod.getStatus().getConditions()) {
                if (Objects.equals(condition.getType(), conditionType)
                    && Objects.equals(condition.getStatus(), conditionStatus)) {

                    return true;
                }
            }

            return false;
        });
    }

    @Then("the component pod {string} has condition {string} with status {string} and reason {string}")
    public void component_pods_condition(String podName, String conditionType, String conditionStatus, String conditionReason) {
        Objects.requireNonNull(podName);
        Objects.requireNonNull(conditionType);
        Objects.requireNonNull(conditionStatus);
        Objects.requireNonNull(conditionReason);

        awaiter.until(() -> {
            String name = resolver().resolve(podName);

            var pod = kubernetesClient.pods()
                .inNamespace(namespace)
                .withName(name)
                .get();

            if (pod == null) {
                return false;
            }
            if (pod.getStatus() == null) {
                return false;
            }
            if (pod.getStatus().getConditions() == null) {
                return false;
            }

            for (var condition : pod.getStatus().getConditions()) {

                if (Objects.equals(condition.getType(), conditionType)
                    && Objects.equals(condition.getStatus(), conditionStatus)
                    && Objects.equals(condition.getReason(), conditionReason)) {

                    return true;
                }
            }

            return false;
        });
    }

    @Then("delete the pod {string}")
    public void component_pods_delete(String podName) {
        Objects.requireNonNull(podName);

        awaiter.until(() -> {
            String name = resolver().resolve(podName);

            kubernetesClient.pods()
                .inNamespace(namespace)
                .withName(name)
                .delete();

            return kubernetesClient.pods().inNamespace(namespace).withName(name).get() == null;
        });
    }

    @Then("property {string} == {string}")
    public void compare_property_equals(String p1, String p2) {
        Objects.requireNonNull(p1);
        Objects.requireNonNull(p2);

        assertThat(p1).isEqualTo(p2);
    }

    @Then("property {string} != {string}")
    public void compare_property_not_equals(String p1, String p2) {
        Objects.requireNonNull(p1);
        Objects.requireNonNull(p2);

        assertThat(p1).isNotEqualTo(p2);
    }

    @Then("a secret {string} with data:")
    public void create_secret(String name, Map<String, String> data) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(data);

        var resolver = resolver();

        Secret secret = new Secret();
        secret.setMetadata(new ObjectMeta());
        secret.getMetadata().setNamespace(namespace);
        secret.getMetadata().setName(name);
        secret.setData(new HashMap<>());

        data.forEach((k, v) -> {
            k = resolver.resolve(k);
            v = resolver.resolve(v);

            secret.getData().put(
                k,
                Base64.getEncoder().encodeToString(v.getBytes(StandardCharsets.UTF_8)));
        });

        kubernetesClient.secrets().createOrReplace(secret);
    }

    @Then("a secret {string} with key {string} and data:")
    public void create_secret(String name, String key, Map<String, String> data) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(key);
        Objects.requireNonNull(data);

        var resolver = resolver();

        Secret secret = new Secret();
        secret.setMetadata(new ObjectMeta());
        secret.getMetadata().setNamespace(namespace);
        secret.getMetadata().setName(name);
        secret.setData(new HashMap<>());

        Properties pp = new Properties();

        data.forEach((k, v) -> {
            k = resolver.resolve(k);
            v = resolver.resolve(v);

            pp.setProperty(k, v);
        });

        try (var w = new StringWriter()) {
            pp.store(w, "cm");

            secret.getData().put(
                key,
                Base64.getEncoder().encodeToString(w.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        data.forEach((k, v) -> {
            k = resolver.resolve(k);
            v = resolver.resolve(v);

            secret.getData().put(
                k,
                Base64.getEncoder().encodeToString(v.getBytes(StandardCharsets.UTF_8)));
        });

        kubernetesClient.secrets().createOrReplace(secret);
    }

    @Then("delete secret {string}")
    public void delete_secret(String name) {
        Objects.requireNonNull(name);

        final String secretName = resolver().resolve(name);

        awaiter.until(() -> {
            kubernetesClient.secrets()
                .inNamespace(namespace)
                .withName(secretName)
                .delete();

            return kubernetesClient.secrets().inNamespace(namespace).withName(secretName).get() == null;
        });
    }

    @Given("a configmap {string} with data:")
    public void create_configmap(String name, Map<String, String> data) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(data);

        var resolver = resolver();

        ConfigMap configmap = new ConfigMap();
        configmap.setMetadata(new ObjectMeta());
        configmap.getMetadata().setNamespace(namespace);
        configmap.getMetadata().setName(name);
        configmap.setData(new HashMap<>());

        data.forEach((k, v) -> {
            k = resolver.resolve(k);
            v = resolver.resolve(v);

            configmap.getData().put(k, v);
        });

        kubernetesClient.configMaps().createOrReplace(configmap);
    }

    @Given("a configmap {string} with key {string} and data:")
    public void create_configmap(String name, String key, Map<String, String> data) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(key);
        Objects.requireNonNull(data);

        var resolver = resolver();

        ConfigMap configmap = new ConfigMap();
        configmap.setMetadata(new ObjectMeta());
        configmap.getMetadata().setNamespace(namespace);
        configmap.getMetadata().setName(name);
        configmap.setData(new HashMap<>());

        Properties pp = new Properties();

        data.forEach((k, v) -> {
            k = resolver.resolve(k);
            v = resolver.resolve(v);

            pp.setProperty(k, v);
        });

        try (var w = new StringWriter()) {
            pp.store(w, "cm");

            configmap.getData().put(
                key,
                w.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        kubernetesClient.configMaps().createOrReplace(configmap);
    }

    @Then("delete configmap {string}")
    public void delete_configmap(String name) {
        Objects.requireNonNull(name);

        final String cmName = resolver().resolve(name);

        awaiter.until(() -> {
            kubernetesClient.configMaps()
                .inNamespace(namespace)
                .withName(cmName)
                .delete();

            return kubernetesClient.configMaps().inNamespace(namespace).withName(cmName).get() == null;
        });
    }

    public PlaceholderResolver resolver() {
        return new PlaceholderResolver(this.properties);
    }

    public static String uid() {
        return ObjectId.get().toString();
    }

    public static final class PlaceholderResolver {
        private final StringSubstitutor substitutor;

        PlaceholderResolver(Map<String, String> placeholders) {
            StringLookup delegate = StringLookupFactory.INSTANCE.mapStringLookup(placeholders);

            this.substitutor = new StringSubstitutor(new HashMap<>(placeholders));
            this.substitutor.setVariableResolver(key -> {
                if ("cos.uid".equals(key)) {
                    return ObjectId.get().toString();
                }

                return delegate.lookup(key);
            });
        }

        public String resolve(String in) {
            return this.substitutor.replace(in);
        }

        public Map<String, String> resolve(Map<String, String> in) {
            Map<String, String> answer = new HashMap<>();
            in.forEach((k, v) -> {
                if (!Strings.isNullOrEmpty(v) && PLACEHOLDER_IGNORE.equals(v)) {
                    v = resolve(v);
                }

                answer.put(k, resolve(v));
            });

            return Collections.unmodifiableMap(answer);
        }
    }
}
