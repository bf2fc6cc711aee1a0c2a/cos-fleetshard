package org.bf2.cos.fleetshard.operator.it.camel.glues;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.bf2.cos.fleetshard.it.cucumber.support.StepsSupport;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBinding;
import org.bf2.cos.fleetshard.support.json.JacksonUtil;

import com.fasterxml.jackson.databind.JsonNode;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.it.cucumber.assertions.CucumberAssertions.assertThatDataTable;

public class KameletBindingSteps extends StepsSupport {
    @Then("the klb exists")
    public void exists() {
        awaiter.until(() -> klb() != null);
    }

    @Then("the klb does not exists")
    public void does_not_exists() {
        awaiter.until(() -> klb() == null);
    }

    @SuppressWarnings("unchecked")
    @When("the klb phase is {string} with conditions:")
    public void klb_phase_and_conditions(String phase, DataTable table) {

        // TODO: investigate using KubernetesClient.resources(KameletBinding.class) result in a bad patch
        kubernetesClient.genericKubernetesResources(KameletBinding.RESOURCE_DEFINITION)
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .editStatus(binding -> {
                Map<String, Object> status = (Map<String, Object>) binding.getAdditionalProperties().get("status");
                if (status == null) {
                    status = new HashMap<>();
                }

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

                status.put("phase", phase);
                status.put("conditions", conditions);
                binding.getAdditionalProperties().put("status", status);

                return binding;
            });

        //        kubernetesClient.resources(KameletBinding.class)
        //            .inNamespace(ctx.connector().getMetadata().getNamespace())
        //            .withName(ctx.connector().getMetadata().getName())
        //            .editStatus(binding -> {
        //                if (binding.getStatus() == null) {
        //                    binding.setStatus(new KameletBindingStatus());
        //                }
        //
        //                List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        //                List<Condition> conditions = new ArrayList<>(rows.size());
        //
        //                for (Map<String, String> columns : rows) {
        //                    conditions.add(new ConditionBuilder()
        //                        .withMessage(columns.get("message"))
        //                        .withReason(columns.get("reason"))
        //                        .withStatus(columns.get("status"))
        //                        .withType(columns.get("type"))
        //                        .withLastTransitionTime(columns.get("lastTransitionTime"))
        //                        .build());
        //                }
        //
        //                binding.getStatus().setPhase(phase);
        //                binding.getStatus().setConditions(conditions);
        //
        //                return binding;
        //            });
    }

    @When("the klb path {string} is set to json:")
    public void klb_pointer(String path, String payload) {
        kubernetesClient.resources(KameletBinding.class)
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .edit(res -> {
                JsonNode replacement = Serialization.unmarshal(payload, JsonNode.class);
                JsonNode replaced = PARSER.parse(Serialization.asJson(res)).set(path, replacement).json();

                return JacksonUtil.treeToValue(replaced, KameletBinding.class);
            });
    }

    @And("the klb has an entry at path {string} with value {string}")
    public void klb_has_a_path_matching_value(String path, String value) {
        KameletBinding res = klb();

        assertThat(res)
            .isNotNull();
        assertThatJson(JacksonUtil.asJsonNode(res))
            .inPath(path)
            .isString()
            .isEqualTo(value);
    }

    @And("the klb has an entry at path {string} with value {int}")
    public void klb_has_a_path_matching_value(String path, int value) {
        KameletBinding res = klb();

        assertThat(res)
            .isNotNull();
        assertThatJson(JacksonUtil.asJsonNode(res))
            .inPath(path)
            .isNumber()
            .isEqualTo(value);
    }

    @And("the klb has an entry at path {string} with value {bool}")
    public void klb_has_a_path_matching_value(String path, Boolean value) {
        KameletBinding res = klb();

        assertThat(res)
            .isNotNull();
        assertThatJson(JacksonUtil.asJsonNode(res))
            .inPath(path)
            .isBoolean()
            .isEqualTo(value);
    }

    @And("the klb has an object at path {string} containing:")
    public void klb_has_a_path_matching_object(String path, String content) {
        KameletBinding res = klb();

        assertThat(res)
            .isNotNull();
        assertThatJson(JacksonUtil.asJsonNode(res))
            .inPath(path)
            .isObject()
            .containsValue(Serialization.unmarshal(content, JsonNode.class));
    }

    @And("the klb has an array at path {string} containing:")
    public void klb_has_a_path_containing_object(String path, DataTable elements) {
        KameletBinding res = klb();

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

    @And("the klb has annotations containing:")
    public void klb_annotation_contains(DataTable table) {
        KameletBinding res = klb();

        assertThat(res)
            .isNotNull();
        assertThatDataTable(table, ctx::resolvePlaceholders)
            .matches(res.getMetadata().getAnnotations());
    }

    @And("the klb has labels containing:")
    public void klb_label_contains(DataTable table) {
        KameletBinding res = klb();

        assertThat(res)
            .isNotNull();
        assertThatDataTable(table, ctx::resolvePlaceholders)
            .matches(res.getMetadata().getLabels());
    }

    @Then("the klb path {string} matches json:")
    public void klb_path_matches(String path, String payload) {
        untilKlb(res -> {
            JsonNode actual = PARSER.parse(JacksonUtil.asJsonNode(res)).read(path);
            JsonNode expected = PARSER.parse(payload).json();

            assertThatJson(actual).isEqualTo(expected);
        });
    }

    private KameletBinding klb() {
        return kubernetesClient.resources(KameletBinding.class)
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .get();
    }

    private void untilKlb(Consumer<KameletBinding> predicate) {
        awaiter.untilAsserted(() -> {
            var res = klb();

            assertThat(res).isNotNull();
            assertThat(res).satisfies(predicate);
        });
    }
}
