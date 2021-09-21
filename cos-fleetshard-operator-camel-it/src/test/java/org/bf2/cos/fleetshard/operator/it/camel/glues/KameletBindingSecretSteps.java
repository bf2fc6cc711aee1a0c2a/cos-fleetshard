package org.bf2.cos.fleetshard.operator.it.camel.glues;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;

import org.bf2.cos.fleetshard.it.cucumber.support.StepsSupport;
import org.bf2.cos.fleetshard.support.json.JacksonUtil;
import org.bf2.cos.fleetshard.support.resources.Resources;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.fabric8.kubernetes.api.model.Secret;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.bf2.cos.fleetshard.it.cucumber.assertions.CucumberAssertions.assertThatDataTable;

public class KameletBindingSecretSteps extends StepsSupport {
    @Then("the klb secret exists")
    public void exists() {
        awaiter.until(() -> secret() != null);
    }

    @Then("the klb secret does not exists")
    public void does_not_exists() {
        awaiter.until(() -> secret() == null);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
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

        assertThatDataTable(table, ctx::resolvePlaceholders)
            .matches((Map) props);
    }

    @And("the klb secret has annotations containing:")
    public void klb_secret_annotation_contains(DataTable table) {
        Secret secret = secret();

        assertThat(secret)
            .isNotNull();
        assertThatDataTable(table, ctx::resolvePlaceholders)
            .matches(secret.getMetadata().getAnnotations());
    }

    @And("the klb secret has labels containing:")
    public void klb_secret_label_contains(DataTable table) {
        Secret secret = secret();

        assertThat(secret)
            .isNotNull();
        assertThatDataTable(table, ctx::resolvePlaceholders)
            .matches(secret.getMetadata().getLabels());
    }

    @And("the klb secret has an entry at path {string} with value {string}")
    public void klb_has_a_path_matching_value(String path, String value) {
        Secret secret = secret();

        assertThat(secret)
            .isNotNull();
        assertThatJson(JacksonUtil.asJsonNode(secret))
            .inPath(path)
            .isString()
            .isEqualTo(value);
    }

    @And("the klb secret has an entry at path {string} with value {int}")
    public void klb_has_a_path_matching_value(String path, int value) {
        Secret secret = secret();

        assertThat(secret)
            .isNotNull();
        assertThatJson(JacksonUtil.asJsonNode(secret))
            .inPath(path)
            .isNumber()
            .isEqualTo(value);
    }

    @And("the klb secret has an entry at path {string} with value {bool}")
    public void klb_has_a_path_matching_value(String path, Boolean value) {
        Secret secret = secret();

        assertThat(secret)
            .isNotNull();
        assertThatJson(JacksonUtil.asJsonNode(secret))
            .inPath(path)
            .isBoolean()
            .isEqualTo(value);
    }

    private Secret secret() {
        return kubernetesClient.secrets()
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName() + Resources.CONNECTOR_SECRET_SUFFIX)
            .get();
    }
}
