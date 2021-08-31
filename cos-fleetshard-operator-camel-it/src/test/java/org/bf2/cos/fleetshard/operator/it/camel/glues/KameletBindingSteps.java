package org.bf2.cos.fleetshard.operator.it.camel.glues;

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
import org.bf2.cos.fleetshard.operator.camel.model.KameletBinding;
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
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

public class KameletBindingSteps {
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

    @Then("the klb exists")
    public void exists() {
        awaiter.until(() -> klb() != null);
    }

    @Then("the klb does not exists")
    public void does_not_exists() {
        awaiter.until(() -> klb() == null);
    }

    @When("the klb phase is {string} with conditions:")
    public void klb_phase_and_conditions(String phase, DataTable table) {
        kubernetesClient.genericKubernetesResources(KameletBinding.RESOURCE_DEFINITION)
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .editStatus(binding -> {
                Map<String, Object> status = (Map<String, Object>) binding.getAdditionalProperties().get("status");
                if (status == null) {
                    status = new HashMap<>();
                }
                List<Map<String, String>> rows = table.asMaps(String.class, String.class);
                List<Map<String, String>> conditions = new ArrayList<>(rows.size());

                for (Map<String, String> columns : rows) {
                    conditions.add(Map.of(
                        "message", columns.get("message"),
                        "reason", columns.get("reason"),
                        "status", columns.get("status"),
                        "type", columns.get("type"),
                        "lastTransitionTime", columns.get("lastTransitionTime")));
                }

                status.put("phase", phase);
                status.put("conditions", conditions);
                binding.getAdditionalProperties().put("status", status);

                return binding;
            });
    }

    @When("the klb path {string} is set to json:")
    public void klb_pointer(String path, String payload) {
        kubernetesClient.genericKubernetesResources(KameletBinding.RESOURCE_DEFINITION)
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .edit(res -> {
                JsonNode replacement = Serialization.unmarshal(payload, JsonNode.class);
                JsonNode replaced = PARSER.parse(Serialization.asJson(res)).set(path, replacement).json();

                return JacksonUtil.treeToValue(replaced, GenericKubernetesResource.class);
            });
    }

    @And("the klb has an entry at path {string} with value {string}")
    public void klb_has_a_path_matching_value(String path, String value) {
        GenericKubernetesResource res = klb();
        assertThat(res).isNotNull();
        assertThatJson(JacksonUtil.asJsonNode(res))
            .inPath(path)
            .isString()
            .isEqualTo(value);
    }

    @And("the klb has an entry at path {string} with value {int}")
    public void klb_has_a_path_matching_value(String path, int value) {
        GenericKubernetesResource res = klb();
        assertThat(res).isNotNull();
        assertThatJson(JacksonUtil.asJsonNode(res))
            .inPath(path)
            .isNumber()
            .isEqualTo(value);
    }

    @And("the klb has an entry at path {string} with value {bool}")
    public void klb_has_a_path_matching_value(String path, Boolean value) {
        GenericKubernetesResource res = klb();
        assertThat(res).isNotNull();
        assertThatJson(JacksonUtil.asJsonNode(res))
            .inPath(path)
            .isBoolean()
            .isEqualTo(value);
    }

    @And("the klb has an object at path {string} containing:")
    public void klb_has_a_path_matching_object(String path, String content) {
        GenericKubernetesResource res = klb();
        assertThat(res).isNotNull();
        assertThatJson(JacksonUtil.asJsonNode(res))
            .inPath(path)
            .isObject()
            .containsValue(Serialization.unmarshal(content, JsonNode.class));
    }

    @And("the klb has an array at path {string} containing:")
    public void klb_has_a_path_containing_object(String path, DataTable elements) {
        GenericKubernetesResource res = klb();
        assertThat(res).isNotNull();

        assertThatJson(JacksonUtil.asJsonNode(res))
            .inPath(path)
            .isArray()
            .containsAll(
                elements.asList().stream()
                    .map(e -> Serialization.unmarshal(e, JsonNode.class))
                    .collect(Collectors.toList()));
    }

    @And("the klb has annotations containing:")
    public void klb_annotation_contains(DataTable table) {
        GenericKubernetesResource res = klb();
        assertThat(res).isNotNull();

        Map<String, String> entries = table.asMap(String.class, String.class);
        entries.forEach((k, v) -> {
            if (Strings.isNullOrEmpty(v)) {
                assertThat(res.getMetadata().getAnnotations()).containsKey(k);
            } else {
                assertThat(res.getMetadata().getAnnotations()).containsEntry(k, v);
            }
        });
    }

    @And("the klb has labels containing:")
    public void klb_label_contains(DataTable table) {
        GenericKubernetesResource res = klb();
        assertThat(res).isNotNull();

        Map<String, String> entries = table.asMap(String.class, String.class);
        entries.forEach((k, v) -> {
            if (Strings.isNullOrEmpty(v)) {
                assertThat(res.getMetadata().getLabels()).containsKey(k);
            } else {
                assertThat(res.getMetadata().getLabels()).containsEntry(k, v);
            }
        });
    }

    @Then("the klb path {string} matches json:")
    public void klb_path_matches(String path, String payload) {
        untilKlb(res -> {
            JsonNode actual = PARSER.parse(JacksonUtil.asJsonNode(res)).read(path);
            JsonNode expected = PARSER.parse(payload).json();

            assertThatJson(actual).isEqualTo(expected);
        });
    }

    private GenericKubernetesResource klb() {
        return kubernetesClient.genericKubernetesResources(KameletBinding.RESOURCE_DEFINITION)
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .get();
    }

    private void untilKlb(Consumer<GenericKubernetesResource> predicate) {
        awaiter.untilAsserted(() -> {
            var res = klb();

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
