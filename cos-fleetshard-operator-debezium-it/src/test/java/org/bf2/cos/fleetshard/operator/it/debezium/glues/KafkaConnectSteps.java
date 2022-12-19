package org.bf2.cos.fleetshard.operator.it.debezium.glues;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.bf2.cos.fleetshard.it.cucumber.support.StepsSupport;
import org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandController;
import org.bf2.cos.fleetshard.support.json.JacksonUtil;

import com.fasterxml.jackson.databind.JsonNode;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.strimzi.api.kafka.model.JmxPrometheusExporterMetrics;
import io.strimzi.api.kafka.model.KafkaConnect;
import io.strimzi.api.kafka.model.status.Condition;
import io.strimzi.api.kafka.model.status.ConditionBuilder;
import io.strimzi.api.kafka.model.status.KafkaConnectStatus;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.it.cucumber.assertions.CucumberAssertions.assertThatDataTable;

public class KafkaConnectSteps extends StepsSupport {
    @Then("the kc exists")
    public void exists() {
        awaiter.until(() -> kafkaConnect() != null);
    }

    @When("the kc has conditions:")
    public void kc_add_conditions(DataTable table) {
        kubernetesClient.resources(KafkaConnect.class)
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
                    resource.setStatus(new KafkaConnectStatus());
                }

                resource.getStatus().setConditions(conditions);

                return resource;
            });
    }

    @Then("the kc does not exists")
    public void does_not_exists() {
        awaiter.until(() -> kafkaConnect() == null);
    }

    @When("the kc path {string} is set to json:")
    public void kc_pointer(String path, String payload) {
        kubernetesClient.resources(KafkaConnect.class)
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .edit(res -> {
                JsonNode replacement = Serialization.unmarshal(payload, JsonNode.class);
                JsonNode replaced = PARSER.parse(Serialization.asJson(res)).set(path, replacement).json();

                return JacksonUtil.treeToValue(replaced, KafkaConnect.class);
            });
    }

    @And("the kc has an entry at path {string} with value {string}")
    public void kc_has_a_path_matching_value(String path, String value) {
        awaiter.untilAsserted(() -> {
            KafkaConnect res = kafkaConnect();

            assertThat(res)
                .isNotNull();
            assertThatJson(JacksonUtil.asJsonNode(res))
                .inPath(path)
                .isString()
                .isEqualTo(ctx.resolvePlaceholders(value));
        });
    }

    @And("the kc has an entry at path {string} with value {int}")
    public void kc_has_a_path_matching_value(String path, int value) {
        awaiter.untilAsserted(() -> {
            KafkaConnect res = kafkaConnect();

            assertThat(res)
                .isNotNull();
            assertThatJson(JacksonUtil.asJsonNode(res))
                .inPath(path)
                .isNumber()
                .satisfies(bd -> assertThat(bd.intValue()).isEqualTo(value));
        });
    }

    @And("the kc has an entry at path {string} with value {bool}")
    public void kc_has_a_path_matching_value(String path, Boolean value) {
        awaiter.untilAsserted(() -> {
            KafkaConnect res = kafkaConnect();

            assertThat(res)
                .isNotNull();
            assertThatJson(JacksonUtil.asJsonNode(res))
                .inPath(path)
                .isBoolean()
                .isEqualTo(value);
        });
    }

    @And("the kc has an object at path {string} containing:")
    public void kc_has_a_path_matching_object(String path, String content) {
        KafkaConnect res = kafkaConnect();
        content = ctx.resolvePlaceholders(content);

        assertThat(res)
            .isNotNull();
        assertThatJson(JacksonUtil.asJsonNode(res))
            .inPath(path)
            .isObject()
            .containsValue(Serialization.unmarshal(content, JsonNode.class));
    }

    @And("the kc has an array at path {string} containing:")
    public void kc_has_a_path_containing_object(String path, DataTable elements) {
        KafkaConnect res = kafkaConnect();

        assertThat(res)
            .isNotNull();
        assertThatJson(JacksonUtil.asJsonNode(res))
            .inPath(path)
            .isArray()
            .containsAll(
                elements.asList().stream()
                    .map(e -> ctx.resolvePlaceholders(e))
                    .map(e -> Serialization.unmarshal(e, JsonNode.class))
                    .collect(Collectors.toList()));
    }

    @And("the kc has annotations containing:")
    public void kc_annotation_contains(DataTable table) {
        KafkaConnect res = kafkaConnect();

        assertThat(res)
            .isNotNull();
        assertThatDataTable(table, ctx::resolvePlaceholders)
            .matches(res.getMetadata().getAnnotations());
    }

    @And("the kc has labels containing:")
    public void kc_label_contains(DataTable table) {
        KafkaConnect res = kafkaConnect();

        assertThat(res)
            .isNotNull();
        assertThatDataTable(table, ctx::resolvePlaceholders)
            .matches(res.getMetadata().getLabels());
    }

    @And("the kc has config containing:")
    public void kc_config_contains(DataTable table) {
        KafkaConnect res = kafkaConnect();

        assertThat(res)
            .isNotNull();
        assertThatDataTable(table, ctx::resolvePlaceholders)
            .matches(res.getSpec().getConfig());
    }

    @Then("the kc path {string} matches json:")
    public void kc_path_matches(String path, String payload) {
        untilKc(res -> {
            JsonNode actual = PARSER.parse(JacksonUtil.asJsonNode(res)).read(path);
            JsonNode expected = PARSER.parse(payload).json();

            assertThatJson(actual).isEqualTo(expected);
        });
    }

    @And("the kc has the correct metrics config map")
    public void kc_metrics_config_map() {
        KafkaConnect kc = kafkaConnect();
        assertThat(kc).isNotNull();
        assertThat(kc.getSpec().getMetricsConfig().getType()).isEqualTo("jmxPrometheusExporter");
        final String kafkaConnectMetricsConfigMapName = ctx.connector().getMetadata().getName()
            + DebeziumOperandController.KAFKA_CONNECT_METRICS_CONFIGMAP_NAME_SUFFIX;
        assertThat(kc.getSpec().getMetricsConfig()).isInstanceOfSatisfying(JmxPrometheusExporterMetrics.class,
            jmxMetricsConfig -> {
                assertThat(jmxMetricsConfig.getValueFrom().getConfigMapKeyRef().getKey())
                    .isEqualTo(DebeziumOperandController.METRICS_CONFIG_FILENAME);
                assertThat(jmxMetricsConfig.getValueFrom().getConfigMapKeyRef().getName())
                    .isEqualTo(kafkaConnectMetricsConfigMapName);
            });
        ConfigMap kcMetricsConfigMap = configMap(kafkaConnectMetricsConfigMapName);
        assertThat(kcMetricsConfigMap).isNotNull();
        assertThat(kcMetricsConfigMap.getData()).containsKey(DebeziumOperandController.METRICS_CONFIG_FILENAME);
        assertThat(kcMetricsConfigMap.getData().get(DebeziumOperandController.METRICS_CONFIG_FILENAME))
            .isEqualTo(DebeziumOperandController.METRICS_CONFIG);
    }

    private ConfigMap configMap(String name) {
        return kubernetesClient.resources(ConfigMap.class)
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(name)
            .get();
    }

    private KafkaConnect kafkaConnect() {
        return kubernetesClient.resources(KafkaConnect.class)
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .get();
    }

    private void untilKc(Consumer<KafkaConnect> predicate) {
        awaiter.untilAsserted(() -> {
            KafkaConnect res = kafkaConnect();

            assertThat(res).isNotNull();
            assertThat(res).satisfies(predicate);
        });
    }
}
