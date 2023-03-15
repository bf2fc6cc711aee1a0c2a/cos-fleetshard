package org.bf2.cos.fleetshard.operator.it.debezium.glues;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.bf2.cos.fleetshard.it.cucumber.support.StepsSupport;
import org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport;
import org.bf2.cos.fleetshard.operator.debezium.client.KafkaConnectorDetail;
import org.bf2.cos.fleetshard.operator.it.debezium.support.DebeziumConnectContext;
import org.bf2.cos.fleetshard.support.json.JacksonUtil;
import org.bf2.cos.fleetshard.support.resources.ConfigMaps;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.resources.Secrets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.DocumentContext;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.PodConditionBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentCondition;
import io.fabric8.kubernetes.api.model.apps.DeploymentConditionBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.client.utils.Serialization;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.it.cucumber.assertions.CucumberAssertions.assertThatDataTable;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.CONNECTOR_CONFIG_FILENAME;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.CONNECT_CONFIG_FILENAME;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.LOGGING_CONFIG;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.LOGGING_CONFIG_FILENAME;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.METRICS_CONFIG;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.METRICS_CONFIG_FILENAME;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.connectorSpecToMap;

public class KafkaConnectSteps extends StepsSupport {
    @Inject
    DebeziumConnectContext debeziumConnectContext;
    @Inject
    Scope scope;

    @Then("the kc deployment exists")
    public void deployment_exists() {
        awaiter.until(() -> deployment() != null);
    }

    @Then("the kc secret exists")
    public void secret_exists() {
        awaiter.until(() -> DebeziumOperandSupport.secret(kubernetesClient, ctx.connector()) != null);
    }

    @Then("the kc configmap exists")
    public void configmap_exists() {
        awaiter.until(() -> DebeziumOperandSupport.configmap(kubernetesClient, ctx.connector()) != null);
    }

    @Then("the kc pvc exists")
    public void pvc_exists() {
        awaiter.until(() -> DebeziumOperandSupport.pvc(kubernetesClient, ctx.connector()) != null);
    }

    @Then("the kc svc exists")
    public void svc_exists() {
        awaiter.until(() -> DebeziumOperandSupport.svc(kubernetesClient, ctx.connector()) != null);
    }

    @Then("the kc deployment does not exists")
    public void deployment_does_not_exists() {
        awaiter.until(() -> deployment() == null);
    }

    @And("the kc deployment has an entry at path {string} with value {string}")
    public void kc_has_a_path_matching_value(String path, String value) {
        awaiter.untilAsserted(() -> {
            Deployment res = deployment();

            assertThat(res)
                .isNotNull();
            assertThatJson(JacksonUtil.asJsonNode(res))
                .inPath(path)
                .isString()
                .isEqualTo(ctx.resolvePlaceholders(value));
        });
    }

    @And("the kc deployment has an entry at path {string} with value {int}")
    public void kc_has_a_path_matching_value(String path, int value) {
        awaiter.untilAsserted(() -> {
            Deployment res = deployment();

            assertThat(res)
                .isNotNull();
            assertThatJson(JacksonUtil.asJsonNode(res))
                .inPath(path)
                .isNumber()
                .satisfies(bd -> assertThat(bd.intValue()).isEqualTo(value));
        });
    }

    @And("the kc deployment has an entry at path {string} with value {bool}")
    public void kc_has_a_path_matching_value(String path, Boolean value) {
        awaiter.untilAsserted(() -> {
            Deployment res = deployment();

            assertThat(res)
                .isNotNull();
            assertThatJson(JacksonUtil.asJsonNode(res))
                .inPath(path)
                .isBoolean()
                .isEqualTo(value);
        });
    }

    @And("the kc deployment satisfy:")
    public void kc_deployment_satisfies(String expression) throws Exception {
        kc_deployment_satisfies_expression(expression);
    }

    @And("the kc deployment satisfy expression {string}")
    public void kc_deployment_satisfies_expression(String expression) throws Exception {
        final String expre = ctx.resolvePlaceholders(expression);
        final JsonQuery query = JsonQuery.compile(expre, Versions.JQ_1_6);

        Deployment res = deployment();

        assertThat(res)
            .isNotNull();

        ObjectNode doc = Serialization.jsonMapper().valueToTree(res);

        List<JsonNode> out = new ArrayList<>();
        query.apply(this.scope, doc, out::add);

        assertThat(out)
            .hasSize(1)
            .first()
            .matches(JsonNode::booleanValue, expression);
    }

    @And("the kc deployment has an object at path {string} containing:")
    public void kc_has_a_path_matching_object(String path, String content) {
        Deployment res = deployment();
        content = ctx.resolvePlaceholders(content);

        assertThat(res)
            .isNotNull();
        assertThatJson(JacksonUtil.asJsonNode(res))
            .inPath(path)
            .isObject()
            .containsValue(Serialization.unmarshal(content, JsonNode.class));
    }

    @And("the kc deployment has an array at path {string} containing:")
    public void kc_has_a_path_containing_object(String path, DataTable elements) {
        Deployment res = deployment();

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

    @And("the kc has config containing:")
    public void kc_config_contains(DataTable table) {
        Secret res = DebeziumOperandSupport.secret(kubernetesClient, ctx.connector());

        assertThat(res)
            .isNotNull();

        Properties props = Secrets.extract(res, CONNECT_CONFIG_FILENAME, Properties.class);

        assertThatDataTable(table, ctx::resolvePlaceholders)
            .matches(props);
    }

    @And("the kctr has config containing:")
    public void kctr_config_contains(DataTable table) {
        Secret res = DebeziumOperandSupport.secret(kubernetesClient, ctx.connector());

        assertThat(res)
            .isNotNull();

        Properties props = Secrets.extract(res, CONNECTOR_CONFIG_FILENAME, Properties.class);

        assertThatDataTable(table, ctx::resolvePlaceholders)
            .matches(props);
    }

    @And("the kctr has config from connector")
    public void kctr_config_from_connector() {
        Secret kctrSecret = DebeziumOperandSupport.secret(kubernetesClient, ctx.connector());
        Properties kctrProps = Secrets.extract(kctrSecret, CONNECTOR_CONFIG_FILENAME, Properties.class);
        ObjectNode mctrProps = Secrets.extract(ctx.secret(), Secrets.SECRET_ENTRY_CONNECTOR, ObjectNode.class);

        assertThat(kctrSecret)
            .isNotNull();
        assertThat(kctrProps)
            .isNotNull();
        assertThat(mctrProps)
            .isNotNull();

        Map<String, String> config = new TreeMap<>();
        connectorSpecToMap(mctrProps, config);

        assertThat(kctrProps)
            .containsAllEntriesOf(config);
    }

    @Then("the kc deployment has path {string} matching json:")
    public void kc_path_matches(String path, String payload) {
        untilDeployment(res -> {
            JsonNode actual = PARSER.parse(JacksonUtil.asJsonNode(res)).read(path);
            JsonNode expected = PARSER.parse(payload).json();

            assertThatJson(actual).isEqualTo(expected);
        });
    }

    private Deployment deployment() {
        return Resources.lookupDeployment(kubernetesClient, ctx.connector());
    }

    private Pod pod() {
        return Resources.lookupPod(kubernetesClient, ctx.connector());
    }

    private void untilDeployment(Consumer<Deployment> predicate) {
        awaiter.untilAsserted(() -> {
            Deployment res = deployment();

            assertThat(res).isNotNull();
            assertThat(res).satisfies(predicate);
        });
    }

    private void untilPod(Consumer<Pod> predicate) {
        awaiter.untilAsserted(() -> {
            Pod res = pod();

            assertThat(res).isNotNull();
            assertThat(res).satisfies(predicate);
        });
    }

    @Given("^create kc pod$")
    public void create_a_kc_pod() {
        var pod = new PodBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .addToLabels(Resources.LABEL_CLUSTER_ID, ctx.connector().getSpec().getClusterId())
                .addToLabels(Resources.LABEL_CONNECTOR_ID, ctx.connector().getSpec().getConnectorId())
                .addToLabels(Resources.LABEL_DEPLOYMENT_ID, ctx.connector().getSpec().getDeploymentId())
                .withNamespace(ctx.connector().getMetadata().getNamespace())
                .withName(ctx.connector().getMetadata().getName())
                .build())
            .withSpec(new PodSpecBuilder()
                .build())
            .build();

        kubernetesClient.resource(pod).createOrReplace();
    }

    @When("set the kc deployment to have conditions:")
    public void kc_deployment_conditions(DataTable table) {
        kubernetesClient.apps().deployments()
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .editStatus(res -> {

                List<Map<String, String>> rows = table.asMaps(String.class, String.class);
                List<DeploymentCondition> conditions = new ArrayList<>(rows.size());

                for (Map<String, String> columns : rows) {
                    conditions.add(new DeploymentConditionBuilder()
                        .withMessage(columns.get("message"))
                        .withReason(columns.get("reason"))
                        .withStatus(columns.get("status"))
                        .withType(columns.get("type"))
                        .withLastTransitionTime(columns.get("lastTransitionTime"))
                        .build());
                }

                if (res.getStatus() == null) {
                    res.setStatus(new DeploymentStatus());
                }

                res.getStatus().setConditions(conditions);

                return res;
            });
    }

    @When("set the kc deployment property {string} at path {string} to {int}")
    public void kc_deployment_set_path_value(String property, String path, int value) {
        setDeploymentPathValue(property, path, value);
    }

    @When("set the kc deployment property {string} at path {string} to {string}")
    public void kc_deployment_set_path_value(String property, String path, String value) {
        setDeploymentPathValue(property, path, value);
    }

    private void setDeploymentPathValue(String property, String path, Object value) {
        kubernetesClient.apps().deployments()
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .edit(res -> {
                DocumentContext document = PARSER.parse(Serialization.asJson(res));
                JsonNode result = document.put(path, property, value).json();

                return JacksonUtil.treeToValue(result, Deployment.class);
            });
    }

    @When("set the kc pod property {string} at path {string} to {int}")
    public void kc_pod_set_path_value(String property, String path, int value) {
        setPodPathValue(property, path, value);
    }

    @When("set the kc pod property {string} at path {string} to {string}")
    public void kc_pod_set_path_value(String property, String path, String value) {
        setPodPathValue(property, path, value);
    }

    private void setPodPathValue(String property, String path, Object value) {
        kubernetesClient.pods()
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .edit(res -> {
                DocumentContext document = PARSER.parse(Serialization.asJson(res));
                JsonNode result = document.put(path, property, value).json();

                return JacksonUtil.treeToValue(result, Pod.class);
            });
    }

    @When("set the kc pod to have conditions:")
    public void kc_pod_conditions(DataTable table) {
        kubernetesClient.pods()
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .editStatus(res -> {

                List<Map<String, String>> rows = table.asMaps(String.class, String.class);
                List<PodCondition> conditions = new ArrayList<>(rows.size());

                for (Map<String, String> columns : rows) {
                    conditions.add(new PodConditionBuilder()
                        .withMessage(columns.get("message"))
                        .withReason(columns.get("reason"))
                        .withStatus(columns.get("status"))
                        .withType(columns.get("type"))
                        .withLastTransitionTime(columns.get("lastTransitionTime"))
                        .build());
                }

                if (res.getStatus() == null) {
                    res.setStatus(new PodStatus());
                }

                res.getStatus().setConditions(conditions);

                return res;
            });
    }

    @Given("^set kc detail to json:$")
    public void connector_detail_json(String detail) {
        try {
            debeziumConnectContext.setDetail(
                Serialization.jsonMapper().readValue(detail, KafkaConnectorDetail.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Given("^set kc detail to yaml:$")
    public void connector_detail_yaml(String detail) {
        try {
            debeziumConnectContext.setDetail(
                Serialization.yamlMapper().readValue(detail, KafkaConnectorDetail.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @And("the kc has configmap containing logging config")
    public void kc_configmap_contains_logging_config() {
        ConfigMap res = DebeziumOperandSupport.configmap(kubernetesClient, ctx.connector());

        assertThat(res)
            .isNotNull();

        String val = LOGGING_CONFIG.keySet().stream()
            .sorted()
            .map(k -> k + "=" + LOGGING_CONFIG.getProperty((String) k))
            .collect(Collectors.joining("\n"));

        assertThat(res.getData()).containsEntry(LOGGING_CONFIG_FILENAME, val);
    }

    @And("the kc has configmap containing logger {string} with level {string}")
    public void kc_configmap_contains_logging_level(String logger, String level) {
        ConfigMap res = DebeziumOperandSupport.configmap(kubernetesClient, ctx.connector());

        assertThat(res)
            .isNotNull();

        Properties props = ConfigMaps.extract(res, LOGGING_CONFIG_FILENAME, Properties.class);

        assertThat(props)
            .isNotNull();

        assertThat(props).containsEntry("log4j.logger." + logger, level);
    }

    @And("wait till the kc has configmap containing logger {string} with level {string}")
    public void wait_till_kc_configmap_contains_logging_level(String logger, String level) {
        awaiter.untilAsserted(() -> {
            ConfigMap res = DebeziumOperandSupport.configmap(kubernetesClient, ctx.connector());

            assertThat(res)
                .isNotNull();

            Properties props = ConfigMaps.extract(res, LOGGING_CONFIG_FILENAME, Properties.class);

            assertThat(props)
                .isNotNull();

            assertThat(props).containsEntry("log4j.logger." + logger, level);
        });
    }

    @And("the kc has configmap containing metrics config")
    public void kc_configmap_contains_metrics_config() {
        ConfigMap res = DebeziumOperandSupport.configmap(kubernetesClient, ctx.connector());

        assertThat(res)
            .isNotNull();

        assertThat(res.getData()).containsEntry(METRICS_CONFIG_FILENAME, METRICS_CONFIG);
    }
}
