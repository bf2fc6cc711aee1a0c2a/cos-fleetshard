package org.bf2.cos.fleetshard.operator.it.camel.glues;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import org.assertj.core.util.Strings;
import org.bf2.cos.fleetshard.it.cucumber.Awaiter;
import org.bf2.cos.fleetshard.it.cucumber.ConnectorContext;
import org.bf2.cos.fleetshard.operator.camel.model.KameletBinding;
import org.bf2.cos.fleetshard.support.json.JacksonUtil;
import org.bf2.cos.fleetshard.support.resources.Connectors;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

public class KameletBindingSecretSteps {
    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    Awaiter awaiter;
    @Inject
    ConnectorContext ctx;

    @Then("the klb secret exists")
    public void exists() {
        awaiter.until(() -> secret() != null);
    }

    @Then("the klb secret does not exists")
    public void does_not_exists() {
        awaiter.until(() -> secret() == null);
    }

    @And("the klb secret contains:")
    public void klb_secret_contains(DataTable table) throws IOException {
        Secret secret = secret();
        assertThat(secret).isNotNull();

        String enc = secret.getData().get("application.properties");
        String dec = new String(Base64.getDecoder().decode(enc));
        Properties props = new Properties();

        try (InputStream is = new ByteArrayInputStream(dec.getBytes(StandardCharsets.UTF_8))) {
            props.load(is);
        }

        Map<String, String> entries = table.asMap(String.class, String.class);
        entries.forEach((k, v) -> {
            if (Strings.isNullOrEmpty(v)) {
                assertThat(props).containsKey(k);
            } else {
                assertThat(props).containsEntry(k, v);
            }
        });
    }

    @And("the klb secret has annotations containing:")
    public void klb_secret_annotation_contains(DataTable table) {
        Secret secret = secret();
        assertThat(secret).isNotNull();

        Map<String, String> entries = table.asMap(String.class, String.class);
        entries.forEach((k, v) -> {
            if (Strings.isNullOrEmpty(v)) {
                assertThat(secret.getMetadata().getAnnotations()).containsKey(k);
            } else {
                assertThat(secret.getMetadata().getAnnotations()).containsEntry(k, v);
            }
        });
    }

    @And("the klb secret has labels containing:")
    public void klb_secret_label_contains(DataTable table) {
        Secret secret = secret();
        assertThat(secret).isNotNull();

        Map<String, String> entries = table.asMap(String.class, String.class);
        entries.forEach((k, v) -> {
            if (Strings.isNullOrEmpty(v)) {
                assertThat(secret.getMetadata().getLabels()).containsKey(k);
            } else {
                assertThat(secret.getMetadata().getLabels()).containsEntry(k, v);
            }
        });
    }

    @And("the klb secret has an entry at path {string} with value {string}")
    public void klb_has_a_path_matching_value(String path, String value) {
        Secret secret = secret();
        assertThat(secret).isNotNull();

        assertThatJson(JacksonUtil.asJsonNode(secret))
            .inPath(path)
            .isString()
            .isEqualTo(value);
    }

    @And("the klb secret has an entry at path {string} with value {int}")
    public void klb_has_a_path_matching_value(String path, int value) {
        Secret secret = secret();
        assertThat(secret).isNotNull();

        assertThatJson(JacksonUtil.asJsonNode(secret))
            .inPath(path)
            .isNumber()
            .isEqualTo(value);
    }

    @And("the klb secret has an entry at path {string} with value {bool}")
    public void klb_has_a_path_matching_value(String path, Boolean value) {
        Secret secret = secret();
        assertThat(secret).isNotNull();

        assertThatJson(JacksonUtil.asJsonNode(secret))
            .inPath(path)
            .isBoolean()
            .isEqualTo(value);
    }

    private GenericKubernetesResource klb() {
        return kubernetesClient.genericKubernetesResources(KameletBinding.RESOURCE_DEFINITION)
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .get();
    }

    private Secret secret() {
        return kubernetesClient.secrets()
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName() + Connectors.CONNECTOR_SECRET_SUFFIX)
            .get();
    }
}
