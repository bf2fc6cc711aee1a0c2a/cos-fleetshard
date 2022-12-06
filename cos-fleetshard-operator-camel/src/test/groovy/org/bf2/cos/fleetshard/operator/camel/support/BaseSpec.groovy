package org.bf2.cos.fleetshard.operator.camel.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import groovy.transform.TypeChecked
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.kubernetes.api.model.Secret
import io.fabric8.kubernetes.client.KubernetesClient
import io.quarkus.runtime.LaunchMode
import io.quarkus.runtime.configuration.ConfigUtils
import io.quarkus.runtime.configuration.ProfileManager
import io.quarkus.runtime.logging.LoggingSetupRecorder
import org.bf2.cos.fleetshard.api.DeploymentSpec
import org.bf2.cos.fleetshard.api.KafkaSpec
import org.bf2.cos.fleetshard.api.ManagedConnector
import org.bf2.cos.fleetshard.api.SchemaRegistrySpec
import org.bf2.cos.fleetshard.api.ServiceAccountSpec
import org.bf2.cos.fleetshard.operator.FleetShardOperatorConfig
import org.bf2.cos.fleetshard.operator.camel.CamelOperandConfiguration
import org.bf2.cos.fleetshard.operator.camel.CamelOperandController
import org.bf2.cos.fleetshard.operator.camel.model.CamelShardMetadata
import org.bf2.cos.fleetshard.operator.camel.model.EndpointKamelet
import org.bf2.cos.fleetshard.operator.camel.model.KameletBinding
import org.bf2.cos.fleetshard.operator.connector.ConnectorConfiguration
import org.bf2.cos.fleetshard.support.metrics.MetricsRecorderConfig
import org.bf2.cos.fleetshard.support.resources.Connectors
import org.bf2.cos.fleetshard.support.resources.Secrets
import org.eclipse.microprofile.config.Config
import org.eclipse.microprofile.config.spi.ConfigProviderResolver
import org.mockito.Mockito
import spock.lang.Specification

import static org.mockito.Mockito.when

class BaseSpec extends Specification {
    public static final String DEFAULT_MANAGED_CONNECTOR_ID = "mid"
    public static final Long DEFAULT_CONNECTOR_REVISION = 1L
    public static final String DEFAULT_CONNECTOR_TYPE_ID = "ctid"
    public static final String DEFAULT_CONNECTOR_IMAGE = "quay.io/cos/s3:1"
    public static final String ALT_CONNECTOR_IMAGE = "quay.io/cos/s3:1.1"
    public static final String DEFAULT_DEPLOYMENT_ID = "1"
    public static final Long DEFAULT_DEPLOYMENT_REVISION = 1L
    public static final String DEFAULT_KAFKA_CLIENT_ID = "kcid"
    public static final String DEFAULT_KAFKA_TOPIC = "kafka-foo"
    public static final String DEFAULT_KAFKA_SERVER = "kafka.acme.com:2181"
    public static final String DEFAULT_KAFKA_REGISTRY= "http://foo.bar:443"
    public static final String DEFAULT_CLIENT_ID = "kcid"
    public static final String DEFAULT_CLIENT_SECRET = Secrets.toBase64("kcs")

    public static final YAMLMapper YAML = new YAMLMapper()

    public static final KubernetesClient CLIENT = Mockito.mock(KubernetesClient.class)
    public static final CamelOperandConfiguration CONF =  mockConfiguration()

    public static CamelOperandController CONTROLLER

    void setupSpec() {
        activateLogging()

        def config = Mockito.mock(FleetShardOperatorConfig.class)
        def metrics = Mockito.mock(FleetShardOperatorConfig.Metrics.class)
        def recorder = Mockito.mock(MetricsRecorderConfig.class)
        def tags = Mockito.mock(MetricsRecorderConfig.Tags.class)

        when(tags.common())
            .thenAnswer(invocation -> [ 'foo': 'bar'])
        when(tags.annotations())
            .thenAnswer(invocation -> Optional.of([ 'my.cos.bf2.org/connector-group' ]))
        when(tags.labels())
            .thenAnswer(invocation ->  Optional.of([ 'cos.bf2.org/organization-id', 'cos.bf2.org/pricing-tier']))

        when(recorder.tags())
            .thenAnswer(invocation -> tags)
        when(metrics.recorder())
            .thenAnswer(invocation -> recorder)
        when(config.metrics())
            .thenAnswer(invocation -> metrics)

        CONTROLLER = new CamelOperandController(config, CLIENT, CONF)
    }

    // ***********************************
    //
    // Helpers
    //
    // ***********************************


    KameletBinding klb(Collection<HasMetadata> resources) {
        return resources.find {
            it.apiVersion == KameletBinding.RESOURCE_API_VERSION && it.kind == KameletBinding.RESOURCE_KIND
        } as KameletBinding
    }


    Secret secret(Collection<HasMetadata> resources) {
        return resources.find {
            it.apiVersion == 'v1' && it.kind == 'Secret'
        } as Secret
    }


    Properties applicationProperties(Collection<HasMetadata> resources) {
        def secret = secret(resources)

        if (secret != null) {
            return Secrets.extract(secret, 'application.properties', Properties.class)
        }

        return new Properties()
    }


    @TypeChecked
    <T> T readValue(Class<T> type, GString content) {
        return YAML.readValue(content, type)
    }


    def reify(ManagedConnector connector, CamelShardMetadata meta, ServiceAccountSpec serviceAccount, Map<String, Object> content) {
        def conf = new ObjectMapper().convertValue(content, ObjectNode.class)

        CONTROLLER.doReify(
                connector, meta,
                new ConnectorConfiguration<ObjectNode, ObjectNode>(conf, ObjectNode.class, ObjectNode.class, null),
                serviceAccount)
    }

    def connector() {
        def connector = new ManagedConnector()
        connector.metadata = new ObjectMeta()
        connector.metadata.name = Connectors.generateConnectorId(DEFAULT_DEPLOYMENT_ID)
        connector.metadata.annotations = [
            'my.cos.bf2.org/connector-group': 'baz'
        ]
        connector.metadata.labels = [
            'cos.bf2.org/organization-id': '20000000',
            'cos.bf2.org/pricing-tier': 'essential'
        ]

        connector.spec.connectorId = DEFAULT_MANAGED_CONNECTOR_ID
        connector.spec.deploymentId = DEFAULT_DEPLOYMENT_ID
        connector.spec.deployment = new DeploymentSpec()
        connector.spec.deployment.connectorTypeId = DEFAULT_CONNECTOR_TYPE_ID
        connector.spec.deployment.connectorResourceVersion = DEFAULT_CONNECTOR_REVISION
        connector.spec.deployment.deploymentResourceVersion = DEFAULT_DEPLOYMENT_REVISION
        connector.spec.deployment.secret = 'secret'
        connector.spec.deployment.desiredState = 'ready'
        connector.spec.deployment.kafka = new KafkaSpec(null, DEFAULT_KAFKA_SERVER)
        connector.spec.deployment.schemaRegistry = new SchemaRegistrySpec(null, DEFAULT_KAFKA_REGISTRY)

        return connector
    }

    def sharedMeta() {
        def sm = new CamelShardMetadata()
        sm.connectorImage = DEFAULT_CONNECTOR_IMAGE
        sm.connectorType = 'source'
        sm.kamelets.adapter = new EndpointKamelet('test-adapter', 'aws')
        sm.kamelets.kafka = new EndpointKamelet('test-kafka', 'kafka')

        return sm
    }

    def serviceAccount() {
        return new ServiceAccountSpec(DEFAULT_CLIENT_ID, DEFAULT_CLIENT_SECRET)
    }

    static def activateLogging() {
        ProfileManager.setLaunchMode(LaunchMode.TEST)

        var testConfig = ConfigUtils.configBuilder(true, true, LaunchMode.NORMAL).build()

        var configProviderResolver = ConfigProviderResolver.instance()
        var tccl = Thread.currentThread().getContextClassLoader()
        Config configToRestore
        try {
            configProviderResolver.registerConfig(testConfig, tccl)
            configToRestore = null
        } catch (IllegalStateException e) {
            // a config is already registered, which can happen in rare cases,
            // so remember it for later restore, release it and register the test config instead
            configToRestore = configProviderResolver.getConfig()
            configProviderResolver.releaseConfig(configToRestore)
            configProviderResolver.registerConfig(testConfig, tccl)
        }

        // calling this method of the Recorder essentially sets up logging and configures most things
        // based on the provided configuration

        try {
            Class<?> lrs = tccl.loadClass(LoggingSetupRecorder.class.getName())
            lrs.getDeclaredMethod("handleFailedStart").invoke(null)
        } catch (Exception e) {
            throw new RuntimeException(e)
        }
    }

    static CamelOperandConfiguration mockConfiguration() {
        def answer = Mockito.mock(CamelOperandConfiguration.class)

        when(answer.labelSelection()).thenAnswer(invocation -> {
            var labelSelection = Mockito.mock(CamelOperandConfiguration.LabelSelection.class);
            when(labelSelection.enabled()).thenAnswer(i -> true);

            return labelSelection;
        });

        when(answer.connectors()).thenAnswer(invocation -> {
            var processors = Mockito.mock((CamelOperandConfiguration.Processors.class));
            when(processors.enabled()).thenReturn(true);
            var connectors = Mockito.mock(CamelOperandConfiguration.Connectors.class);
            when(connectors.processors()).thenReturn(processors);
            return connectors;
        });

        return answer
    }
}
