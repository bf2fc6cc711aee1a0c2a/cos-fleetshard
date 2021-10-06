package org.bf2.cos.fleetshard.it.cucumber;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperatorBuilder;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperatorSpecBuilder;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

import static org.bf2.cos.fleetshard.support.resources.Resources.uid;

public class ManagedConnectorOperatorSteps {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedConnectorOperatorSteps.class);

    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    Awaiter awaiter;
    @Inject
    ManagedConnectorOperatorContext ctx;

    @Before
    @After
    public void cleanUp() {
        ctx.clear();

        if (ctx.managedConnectorOperator() != null) {
            LOGGER.info("Deleting ManagedConnectorOperator: {} in namespace {}",
                ctx.managedConnectorOperator().getMetadata().getName(),
                ctx.managedConnectorOperator().getMetadata().getNamespace());

            kubernetesClient.resources(ManagedConnectorOperator.class)
                .inNamespace(ctx.managedConnectorOperator().getMetadata().getNamespace())
                .withName(ctx.managedConnectorOperator().getMetadata().getName())
                .delete();
        }
    }

    @Given("^a ManagedConnectorOperator with:$")
    public void a_managedConnectorOperator(Map<String, String> entry) {
        final String operatorId = entry.getOrDefault("operator.id", uid());
        final String operatorType = entry.get("operator.type");
        final String operatorVersion = entry.get("operator.version");
        final String operatorRuntime = entry.get("operator.runtime");

        var connector = new ManagedConnectorOperatorBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(operatorId)
                .addToLabels(Resources.LABEL_OPERATOR_TYPE, operatorType)
                .addToLabels(Resources.LABEL_OPERATOR_VERSION, operatorVersion)
                .build())
            .withSpec(new ManagedConnectorOperatorSpecBuilder()
                .withVersion(operatorVersion)
                .withType(operatorType)
                .withRuntime(operatorRuntime)
                .build())
            .build();

        ctx.managedConnectorOperator(connector);
    }

    @When("deploy a ManagedConnectorOperator with:")
    public void deploy_a_managedConnectorOperator(Map<String, String> entry) {
        a_managedConnectorOperator(entry);
        ctx.managedConnectorOperator(
            kubernetesClient.resources(ManagedConnectorOperator.class)
                .inNamespace(ctx.namespace())
                .createOrReplace(ctx.managedConnectorOperator()));
    }

    @Then("the ManagedConnectorOperator with name {string} exists")
    public void managedConnectorOperator_is_created(String name) {
        until(() -> {
            var res = kubernetesClient.resources(ManagedConnectorOperator.class)
                .inNamespace(ctx.namespace())
                .withName(name)
                .get();

            return res != null;
        });
    }

    private void until(Callable<Boolean> conditionEvaluator) {
        awaiter.until(conditionEvaluator);
    }

    private void untilManagedConnectorOperator(Predicate<ManagedConnectorOperator> predicate) {
        awaiter.until(() -> {
            var res = kubernetesClient.resources(ManagedConnectorOperator.class)
                .inNamespace(ctx.managedConnectorOperator().getMetadata().getNamespace())
                .withName(ctx.managedConnectorOperator().getMetadata().getName())
                .get();

            if (res == null) {
                return false;
            }

            return predicate.test(res);
        });
    }
}
