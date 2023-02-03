package org.bf2.cos.fleetshard.operator.e2e.glues;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import javax.inject.Inject;

import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;
import org.assertj.core.util.Strings;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.utils.Serialization;

import static org.assertj.core.api.Assertions.assertThat;

public class Steps {
    public static final Logger LOGGER = LoggerFactory.getLogger(Steps.class);

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

    @Then("the component {string} has condition {string} with status {string}")
    public void component_condition(String name, String conditionType, String conditionStatus) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(conditionType);
        Objects.requireNonNull(conditionStatus);

        awaiter.untilAsserted(() -> {
            var deployment = kubernetesClient.apps().deployments()
                .inNamespace(namespace)
                .withName(name)
                .get();

            assertThat(deployment).isNotNull();
            assertThat(deployment.getStatus()).isNotNull();
            assertThat(deployment.getStatus().getConditions()).isNotNull();

            assertThat(deployment.getStatus().getConditions())
                .withFailMessage(() -> {
                    try {
                        var pods = kubernetesClient.pods()
                            .inNamespace(namespace)
                            .withLabel("app.kubernetes.io/name", name)
                            .list();

                        List<String> logs = new ArrayList<>();
                        List<String> resources = new ArrayList<>();

                        for (Pod pod : pods.getItems()) {
                            try {
                                var log = kubernetesClient.pods()
                                    .inNamespace(pod.getMetadata().getNamespace())
                                    .withName(pod.getMetadata().getName())
                                    .getLog();

                                logs.add(log);
                            } catch (KubernetesClientException e) {
                                LOGGER.warn("", e);
                            }

                            resources.add(
                                Serialization.yamlMapper()
                                    .writerWithDefaultPrettyPrinter()
                                    .writeValueAsString(pod));
                        }

                        return String.format(
                            "No condition %s with status %s for deployment %s: \n%s\n%s\n%s",
                            conditionType,
                            conditionStatus,
                            name,
                            Serialization.yamlMapper()
                                .writerWithDefaultPrettyPrinter()
                                .writeValueAsString(deployment.getStatus().getConditions()),
                            String.join("\n", resources),
                            String.join("\n", logs));
                    } catch (JsonProcessingException e) {
                        throw KubernetesClientException.launderThrowable(e);
                    }
                })
                .anyMatch(c -> {
                    return Objects.equals(c.getType(), conditionType) && Objects.equals(c.getStatus(), conditionStatus);
                });
        });
    }

    @Then("the component pod {string} has condition {string} with status {string}")
    public void component_pods_condition(String podName, String conditionType, String conditionStatus) {
        Objects.requireNonNull(podName);
        Objects.requireNonNull(conditionType);
        Objects.requireNonNull(conditionStatus);

        awaiter.untilAsserted(() -> {
            String name = resolver().resolve(podName);

            var pod = kubernetesClient.pods()
                .inNamespace(namespace)
                .withName(name)
                .get();

            assertThat(pod).isNotNull();
            assertThat(pod.getStatus()).isNotNull();
            assertThat(pod.getStatus().getConditions()).isNotNull();

            assertThat(pod.getStatus().getConditions())
                .withFailMessage(() -> {
                    try {
                        String log = "";

                        try {
                            log = kubernetesClient.pods()
                                .inNamespace(pod.getMetadata().getNamespace())
                                .withName(pod.getMetadata().getName())
                                .getLog();
                        } catch (KubernetesClientException e) {
                            LOGGER.warn("", e);
                        }

                        return String.format(
                            "No condition %s with status %s for pod %s: \n%s\n%s\n%s",
                            conditionType,
                            conditionStatus,
                            name,
                            Serialization.yamlMapper()
                                .writerWithDefaultPrettyPrinter()
                                .writeValueAsString(pod.getStatus().getConditions()),
                            Serialization.yamlMapper()
                                .writerWithDefaultPrettyPrinter()
                                .writeValueAsString(pod),
                            log);
                    } catch (JsonProcessingException e) {
                        throw KubernetesClientException.launderThrowable(e);
                    }
                })
                .anyMatch(c -> {
                    return Objects.equals(c.getType(), conditionType) && Objects.equals(c.getStatus(), conditionStatus);
                });
        });
    }

    @Then("the component pod {string} has condition {string} with status {string} and reason {string}")
    public void component_pods_condition(String podName, String conditionType, String conditionStatus, String conditionReason) {
        Objects.requireNonNull(podName);
        Objects.requireNonNull(conditionType);
        Objects.requireNonNull(conditionStatus);
        Objects.requireNonNull(conditionReason);

        awaiter.untilAsserted(() -> {
            String name = resolver().resolve(podName);

            var pod = kubernetesClient.pods()
                .inNamespace(namespace)
                .withName(name)
                .get();

            assertThat(pod).isNotNull();
            assertThat(pod.getStatus()).isNotNull();
            assertThat(pod.getStatus().getConditions()).isNotNull();

            assertThat(pod.getStatus().getConditions())
                .withFailMessage(() -> {
                    try {
                        return String.format(
                            "No condition %s with status %s for pod %s: \n%s",
                            conditionType,
                            conditionStatus,
                            name,
                            Serialization.yamlMapper()
                                .writerWithDefaultPrettyPrinter()
                                .writeValueAsString(pod.getStatus().getConditions()));
                    } catch (JsonProcessingException e) {
                        throw KubernetesClientException.launderThrowable(e);
                    }
                })
                .anyMatch(c -> {
                    return Objects.equals(c.getType(), conditionType)
                        && Objects.equals(c.getStatus(), conditionStatus)
                        && Objects.equals(c.getReason(), conditionReason);
                });
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

        kubernetesClient.resource(secret).createOrReplace();
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

        kubernetesClient.resource(secret).createOrReplace();
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

        kubernetesClient.resource(configmap).createOrReplace();
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

        kubernetesClient.resource(configmap).createOrReplace();
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
