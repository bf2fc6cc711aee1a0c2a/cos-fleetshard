package org.bf2.cos.fleetshard.operator.it.debezium.glues;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.assertj.core.util.Strings;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.it.cucumber.Awaiter;
import org.bf2.cos.fleetshard.it.cucumber.ConnectorContext;
import org.bf2.cos.fleetshard.support.json.JacksonUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.strimzi.api.kafka.model.KafkaConnector;
import io.strimzi.api.kafka.model.status.Condition;
import io.strimzi.api.kafka.model.status.ConditionBuilder;
import io.strimzi.api.kafka.model.status.KafkaConnectorStatus;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

public class KafkaConnectorSteps {
    private static final ParseContext PARSER = JsonPath.using(
        Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .mappingProvider(new JacksonMappingProvider())
            .build());

    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    Awaiter awaiter;
    @Inject
    ConnectorContext ctx;

    @Then("the kctr exists")
    public void exists() {
        awaiter.until(() -> kctr() != null);
    }

    @Then("the kctr does not exists")
    public void does_not_exists() {
        awaiter.until(() -> kctr() == null);
    }

    @When("the kctr path {string} is set to json:")
    public void kctr_pointer(String path, String payload) {
        kubernetesClient.resources(KafkaConnector.class)
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .edit(res -> {
                JsonNode replacement = Serialization.unmarshal(payload, JsonNode.class);
                JsonNode replaced = PARSER.parse(Serialization.asJson(res)).set(path, replacement).json();

                return JacksonUtil.treeToValue(replaced, KafkaConnector.class);
            });
    }

    @And("the kctr has an entry at path {string} with value {string}")
    public void kctr_has_a_path_matching_value(String path, String value) {
        KafkaConnector res = kctr();

        assertThat(res).isNotNull();
        assertThatJson(JacksonUtil.asJsonNode(res))
            .inPath(path)
            .isString()
            .isEqualTo(ctx.resolvePlaceholders(value));
    }

    @And("the kctr has an entry at path {string} with value {int}")
    public void kctr_has_a_path_matching_value(String path, int value) {
        KafkaConnector res = kctr();
        assertThat(res).isNotNull();
        assertThatJson(JacksonUtil.asJsonNode(res))
            .inPath(path)
            .isNumber()
            .satisfies(bd -> assertThat(bd.intValue()).isEqualTo(value));
    }

    @And("the kctr has an entry at path {string} with value {bool}")
    public void kctr_has_a_path_matching_value(String path, Boolean value) {
        KafkaConnector res = kctr();
        assertThat(res).isNotNull();
        assertThatJson(JacksonUtil.asJsonNode(res))
            .inPath(path)
            .isBoolean()
            .isEqualTo(value);
    }

    @And("the kctr has an object at path {string} containing:")
    public void kctr_has_a_path_matching_object(String path, String content) {
        KafkaConnector res = kctr();
        assertThat(res).isNotNull();
        assertThatJson(JacksonUtil.asJsonNode(res))
            .inPath(path)
            .isObject()
            .containsValue(Serialization.unmarshal(content, JsonNode.class));
    }

    @And("the kctr has an array at path {string} containing:")
    public void kctr_has_a_path_containing_object(String path, DataTable elements) {
        KafkaConnector res = kctr();
        assertThat(res).isNotNull();

        assertThatJson(JacksonUtil.asJsonNode(res))
            .inPath(path)
            .isArray()
            .containsAll(
                elements.asList().stream()
                    .map(e -> Serialization.unmarshal(e, JsonNode.class))
                    .collect(Collectors.toList()));
    }

    @And("the kctr has annotations containing:")
    public void kctr_annotation_contains(DataTable table) {
        KafkaConnector res = kctr();
        assertThat(res).isNotNull();

        Map<String, String> entries = ctx.resolvePlaceholders(table);
        entries.forEach((k, v) -> {
            if (Strings.isNullOrEmpty(v) || "${cos.ignore}".equals(v)) {
                assertThat(res.getMetadata().getAnnotations()).containsKey(k);
            } else {
                assertThat(res.getMetadata().getAnnotations()).containsEntry(k, v);
            }
        });
    }

    @And("the kctr has labels containing:")
    public void kctr_label_contains(DataTable table) {
        var res = kctr();
        assertThat(res).isNotNull();

        Map<String, String> entries = ctx.resolvePlaceholders(table);
        entries.forEach((k, v) -> {
            if (Strings.isNullOrEmpty(v) || "${cos.ignore}".equals(v)) {
                assertThat(res.getMetadata().getLabels()).containsKey(k);
            } else {
                assertThat(res.getMetadata().getLabels()).containsEntry(k, v);
            }
        });
    }

    @And("the kctr has config containing:")
    public void kc_config_contains(DataTable table) {
        var res = kctr();
        assertThat(res).isNotNull();

        Map<String, String> entries = ctx.resolvePlaceholders(table);
        entries.forEach((k, v) -> {
            if (Strings.isNullOrEmpty(v) || "${cos.ignore}".equals(v)) {
                assertThat(res.getSpec().getConfig()).containsKey(k);
            } else {
                assertThat(res.getSpec().getConfig()).containsKey(k);
                assertThat(res.getSpec().getConfig().get(k)).hasToString(v);
            }
        });
    }

    @Then("the kctr path {string} matches json:")
    public void kctr_path_matches(String path, String payload) {
        untilKctr(res -> {
            JsonNode actual = PARSER.parse(JacksonUtil.asJsonNode(res)).read(path);
            JsonNode expected = PARSER.parse(payload).json();

            assertThatJson(actual).isEqualTo(expected);
        });
    }

    @When("the kctr status is {string}")
    public void kctr_status_is(String phase) {
        kubernetesClient.resources(KafkaConnector.class)
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .editStatus(resource -> {
                if (resource.getStatus() == null) {
                    resource.setStatus(new KafkaConnectorStatus());
                }
                if (resource.getStatus().getConnectorStatus() == null) {
                    resource.getStatus().setConnectorStatus(new HashMap<>());
                }

                resource.getStatus().getConnectorStatus().put(
                    "connector",
                    Map.of("state", phase));

                //System.err.println(JacksonUtil.asPrettyPrintedYaml(resource));

                return resource;
            });
    }

    @When("the kctr has conditions:")
    public void kctr_and_conditions(DataTable table) {
        kubernetesClient.resources(KafkaConnector.class)
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .editStatus(resource -> {
                List<Map<String, String>> rows = table.asMaps(String.class, String.class);
                List<Condition> conditions = new ArrayList<>(rows.size());

                for (Map<String, String> columns : rows) {
                    conditions.add(new ConditionBuilder()
                        .withMessage(columns.get("message"))
                        .withReason(columns.get("reason"))
                        .withStatus(columns.get("status"))
                        .withType(columns.get("type"))
                        .withLastTransitionTime(columns.get("lastTransitionTime"))
                        .build());
                }

                if (resource.getStatus() == null) {
                    resource.setStatus(new KafkaConnectorStatus());
                }

                resource.getStatus().setConditions(conditions);

                return resource;
            });
    }

    private KafkaConnector kctr() {
        return kubernetesClient.resources(KafkaConnector.class)
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .get();
    }

    private void untilKctr(Consumer<KafkaConnector> predicate) {
        awaiter.untilAsserted(() -> {
            KafkaConnector res = kctr();

            assertThat(res).isNotNull();
            assertThat(res).satisfies(predicate);
        });
    }

    private ManagedConnector connector() {
        return kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .get();
    }
}
