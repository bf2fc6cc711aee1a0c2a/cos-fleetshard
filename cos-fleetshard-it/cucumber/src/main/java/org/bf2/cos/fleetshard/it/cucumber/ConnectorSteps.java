package org.bf2.cos.fleetshard.it.cucumber;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.bf2.cos.fleetshard.api.DeploymentSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorSpecBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;
import org.bf2.cos.fleetshard.api.OperatorSelectorBuilder;
import org.bf2.cos.fleetshard.support.json.JacksonUtil;
import org.bf2.cos.fleetshard.support.resources.Connectors;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.resources.Secrets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;

import static org.bf2.cos.fleetshard.it.cucumber.support.StepsSupport.PARSER;
import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

public class ConnectorSteps {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorSteps.class);

    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    Awaiter awaiter;
    @Inject
    ConnectorContext ctx;

    @Before
    public void setUp() {
        ctx.clear();
    }

    @After
    public void cleanUp() {
        ctx.clear();

        if (ctx.connector() != null) {
            LOGGER.info("Deleting connector: {} in namespace {}",
                ctx.connector().getMetadata().getName(),
                ctx.connector().getMetadata().getNamespace());

            kubernetesClient.resources(ManagedConnector.class)
                .inNamespace(ctx.connector().getMetadata().getNamespace())
                .withName(ctx.connector().getMetadata().getName())
                .delete();
        }

        if (ctx.secret() != null) {
            LOGGER.info("Deleting secret: {} in namespace {}",
                ctx.secret().getMetadata().getName(),
                ctx.secret().getMetadata().getNamespace());

            kubernetesClient.secrets()
                .inNamespace(ctx.secret().getMetadata().getNamespace())
                .withName(ctx.secret().getMetadata().getName())
                .delete();
        }
    }

    @Given("^a Connector with:$")
    public void a_connector(Map<String, String> entry) {
        final Long drv = Long.parseLong(entry.getOrDefault("deployment.resource.version", "1"));
        final Long crv = Long.parseLong(entry.getOrDefault("connector.resource.version", "1"));
        final String connectorId = entry.getOrDefault("connector.id", uid());
        final String deploymentId = entry.getOrDefault("deployment.id", uid());
        final String clusterId = ctx.clusterId();

        var connector = new ManagedConnectorBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .addToLabels(Resources.LABEL_CLUSTER_ID, clusterId)
                .addToLabels(Resources.LABEL_CONNECTOR_ID, connectorId)
                .addToLabels(Resources.LABEL_DEPLOYMENT_ID, deploymentId)
                .addToLabels(Resources.LABEL_OPERATOR_TYPE, entry.get("operator.type"))
                .withName(Connectors.generateConnectorId(deploymentId))
                .build())
            .withSpec(new ManagedConnectorSpecBuilder()
                .withClusterId(clusterId)
                .withConnectorId(connectorId)
                .withDeploymentId(deploymentId)
                .withDeployment(new DeploymentSpecBuilder()
                    .withConnectorResourceVersion(crv)
                    .withConnectorTypeId(entry.get("connector.type.id"))
                    .withDeploymentResourceVersion(drv)
                    .withDesiredState(entry.get("desired.state"))
                    .withSecret(Connectors.generateConnectorId(deploymentId) + "-" + drv)
                    .build())
                .withOperatorSelector(new OperatorSelectorBuilder()
                    .withId(entry.get("operator.id"))
                    .withType(entry.get("operator.type"))
                    .withVersion(entry.get("operator.version"))
                    .build())
                .build())
            .build();

        var secret = new SecretBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .addToLabels(Resources.LABEL_OPERATOR_TYPE, entry.get("operator.type"))
                .withName(connector.getMetadata().getName()
                    + "-"
                    + connector.getSpec().getDeployment().getDeploymentResourceVersion())
                .build())
            .withData(new HashMap<>())
            .addToData(
                "kafka",
                Secrets.toBase64(Serialization.asJson(
                    Serialization.jsonMapper().createObjectNode()
                        .put("bootstrap_server", entry.getOrDefault("kafka.bootstrap", "kafka.acme.com:443"))
                        .put("client_id", entry.getOrDefault("kafka.client.id", uid()))
                        .put("client_secret", entry.getOrDefault("kafka.client.secret", Secrets.toBase64(uid()))))))
            .build();

        ctx.connector(connector);
        ctx.secret(secret);
    }

    @And("with connector spec:")
    public void with_connector_spec(String payload) {
        Secrets.set(ctx.secret(), Secrets.SECRET_ENTRY_CONNECTOR, payload);
    }

    @And("with shard meta:")
    public void with_shard_meta(String payload) {
        Secrets.set(ctx.secret(), Secrets.SECRET_ENTRY_META, payload);
    }

    @When("^deploy$")
    public void deploy() {
        final String uow = uid();
        deploy_connector_with_uow(uow);
        deploy_secret_with_uow(uow);
    }

    @When("^deploy secret$")
    public void deploy_secret() {
        deploy_secret_with_uow(uid());
    }

    @When("^deploy connector$")
    public void deploy_connector() {
        deploy_connector_with_uow(uid());
    }

    @Then("the connector exists")
    public void connector_is_created() {
        until(() -> {
            var res = kubernetesClient.resources(ManagedConnector.class)
                .inNamespace(ctx.connector().getMetadata().getNamespace())
                .withName(ctx.connector().getMetadata().getName())
                .get();

            return res != null;
        });
    }

    @Then("the connector secret exists")
    public void secret_is_created() {
        until(() -> {
            var res = kubernetesClient.resources(Secret.class)
                .inNamespace(ctx.secret().getMetadata().getNamespace())
                .withName(ctx.secret().getMetadata().getName())
                .get();

            return res != null;
        });
    }

    @And("the connector secret does not exists")
    public void secret_does_not_exists() {
        until(() -> {
            var res = kubernetesClient.resources(Secret.class)
                .inNamespace(ctx.secret().getMetadata().getNamespace())
                .withName(ctx.secret().getMetadata().getName())
                .get();

            return res == null;
        });
    }

    @When("the connector desired status is set to {string}")
    public void connector_desired_state_set_to(String status) {
        kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .accept(c -> c.getSpec().getDeployment().setDesiredState(status));
    }

    @Then("the connector is in phase {string}")
    public void connector_is_in_phase(String phase) {
        untilConnector(c -> {
            return Objects.equals(
                ManagedConnectorStatus.PhaseType.valueOf(phase),
                c.getStatus().getPhase());
        });
    }

    @Then("the deployment is in phase {string}")
    public void deployment_is_in_phase(String phase) {
        untilConnector(c -> {
            return Objects.equals(
                phase,
                c.getStatus().getConnectorStatus().getPhase());
        });
    }

    @When("the connector path {string} is set to json:")
    public void connector_pointer(String path, String payload) {
        var result = kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .edit(res -> {
                JsonNode replacement = Serialization.unmarshal(payload, JsonNode.class);
                JsonNode replaced = PARSER.parse(Serialization.asJson(res)).set(path, replacement).json();

                return JacksonUtil.treeToValue(replaced, ManagedConnector.class);
            });
    }

    @When("the connector path {string} is set to {string}")
    public void connector_pointer_set_to_string(String path, String value) {
        var result = kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .edit(res -> {
                JsonNode replaced = PARSER.parse(Serialization.asJson(res)).set(path, value).json();

                return JacksonUtil.treeToValue(replaced, ManagedConnector.class);
            });
    }

    @When("the connector path {string} is set to {int}")
    public void connector_pointer_set_to_int(String path, int value) {
        var result = kubernetesClient.resources(ManagedConnector.class)
            .inNamespace(ctx.connector().getMetadata().getNamespace())
            .withName(ctx.connector().getMetadata().getName())
            .edit(res -> {
                JsonNode replaced = PARSER.parse(Serialization.asJson(res)).set(path, value).json();

                return JacksonUtil.treeToValue(replaced, ManagedConnector.class);
            });
    }

    @When("the connector secret has labels:")
    public void connector_secret_has_labels(Map<String, String> entry) {
        var result = kubernetesClient.resources(Secret.class)
            .inNamespace(ctx.secret().getMetadata().getNamespace())
            .withName(ctx.secret().getMetadata().getName())
            .edit(res -> {
                entry.forEach((k, v) -> Resources.setLabel(res, k, ctx.resolvePlaceholders(v)));
                return res;
            });
    }

    private void until(Callable<Boolean> conditionEvaluator) {
        awaiter.until(conditionEvaluator);
    }

    private void untilConnector(Predicate<ManagedConnector> predicate) {
        awaiter.until(() -> {
            var res = kubernetesClient.resources(ManagedConnector.class)
                .inNamespace(ctx.connector().getMetadata().getNamespace())
                .withName(ctx.connector().getMetadata().getName())
                .get();

            if (res == null) {
                return false;
            }

            return predicate.test(res);
        });
    }

    public void deploy_secret_with_uow(String uow) {
        Resources.setAnnotation(
            ctx.secret(),
            Resources.ANNOTATION_UPDATED_TIMESTAMP,
            ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        Resources.setLabel(
            ctx.secret(),
            Resources.LABEL_UOW,
            uow);

        ctx.secret(
            kubernetesClient.resources(Secret.class)
                .inNamespace(ctx.namespace())
                .createOrReplace(ctx.secret()));
    }

    public void deploy_connector_with_uow(String uow) {
        Resources.setAnnotation(
            ctx.connector(),
            Resources.ANNOTATION_UPDATED_TIMESTAMP,
            ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        Resources.setLabel(
            ctx.connector(),
            Resources.LABEL_UOW,
            uow);

        ctx.connector().getSpec().getDeployment().setUnitOfWork(uow);

        ctx.connector(
            kubernetesClient.resources(ManagedConnector.class)
                .inNamespace(ctx.namespace())
                .createOrReplace(ctx.connector()));
    }
}
