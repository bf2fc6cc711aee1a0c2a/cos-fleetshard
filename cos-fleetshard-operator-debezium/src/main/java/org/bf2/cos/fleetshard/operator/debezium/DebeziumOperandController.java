package org.bf2.cos.fleetshard.operator.debezium;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorStatus;
import org.bf2.cos.fleetshard.api.ServiceAccountSpec;
import org.bf2.cos.fleetshard.operator.FleetShardOperatorConfig;
import org.bf2.cos.fleetshard.operator.connector.ConnectorConfiguration;
import org.bf2.cos.fleetshard.operator.debezium.client.KafkaConnectorClient;
import org.bf2.cos.fleetshard.operator.debezium.converter.KeyAndValueConverters;
import org.bf2.cos.fleetshard.operator.debezium.model.DebeziumDataShape;
import org.bf2.cos.fleetshard.operator.debezium.model.DebeziumShardMetadata;
import org.bf2.cos.fleetshard.operator.operand.AbstractOperandController;
import org.bf2.cos.fleetshard.support.resources.Conditions;
import org.bf2.cos.fleetshard.support.resources.ConfigMaps;
import org.bf2.cos.fleetshard.support.resources.Resources;
import org.bf2.cos.fleetshard.support.resources.Secrets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpecBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategy;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.javaoperatorsdk.operator.OperatorException;
import io.javaoperatorsdk.operator.processing.event.Event;
import io.javaoperatorsdk.operator.processing.event.EventHandler;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.CONNECTOR_CONFIG_FILENAME;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.CONNECTOR_CONFIG_PATH;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.CONNECTOR_OFFSET_PATH;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.CONNECT_CONFIG_FILENAME;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.CONNECT_CONFIG_PATH;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.DEFAULT_CONFIG_OPTIONS;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.KAFKA_HOME;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.LOGGING_CONFIG_PATH;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.METRICS_CONFIG_PATH;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.RHOC_CONFIG_HOME;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.RHOC_DATA_HOME;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumConstants.RHOC_SECRETS_HOME;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.computeConnectorCondition;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.computeKafkaConnectCondition;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.configMapName;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.createConnectorConfig;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.env;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.mount;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.secretName;
import static org.bf2.cos.fleetshard.operator.debezium.DebeziumOperandSupport.volume;
import static org.bf2.cos.fleetshard.support.resources.Resources.selector;

public class DebeziumOperandController extends AbstractOperandController<DebeziumShardMetadata, ObjectNode, DebeziumDataShape> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebeziumOperandController.class);

    public final static Probe PROBE = DebeziumOperandSupport.probe();
    public final static ContainerPort PORT_REST = DebeziumOperandSupport.port(8083, "rest-api");
    public final static ContainerPort PORT_PROMETHEUS = DebeziumOperandSupport.port(9404, "tcp-prometheus");
    public final static ResourceRequirements DEFAULT_RESOURCES = DebeziumOperandSupport.resources();

    public static final List<ResourceDefinitionContext> RESOURCE_TYPES = List.of(
        new ResourceDefinitionContext.Builder()
            .withNamespaced(true)
            .withGroup(HasMetadata.getGroup(Deployment.class))
            .withVersion(HasMetadata.getVersion(Deployment.class))
            .withKind(HasMetadata.getKind(Deployment.class))
            .withPlural(HasMetadata.getPlural(Deployment.class))
            .build());

    private final DebeziumOperandConfiguration configuration;
    private final KafkaConnectorClient client;

    public DebeziumOperandController(
        FleetShardOperatorConfig config,
        KubernetesClient kubernetesClient,
        DebeziumOperandConfiguration configuration,
        KafkaConnectorClient client) {

        super(config, kubernetesClient, DebeziumShardMetadata.class, ObjectNode.class, DebeziumDataShape.class);

        this.configuration = configuration;
        this.client = client;
    }

    @Override
    public List<ResourceDefinitionContext> getResourceTypes() {
        return RESOURCE_TYPES;
    }

    @Override
    public Map<String, EventSource> getEventSources() {
        return Map.of("_scraper", new Trigger());
    }

    @Override
    protected List<HasMetadata> doReify(
        ManagedConnector connector,
        DebeziumShardMetadata shardMetadata,
        ConnectorConfiguration<ObjectNode, DebeziumDataShape> connectorConfiguration,
        ServiceAccountSpec serviceAccountSpec) {

        //
        // PVC
        //

        PersistentVolumeClaim connectPvc = pvc(connector);

        //
        // Service
        //

        Service connectService = new ServiceBuilder()
            .withMetadata(
                new ObjectMetaBuilder()
                    .withName(connector.getMetadata().getName())
                    .addToLabels(Resources.LABEL_CLUSTER_ID, connector.getSpec().getClusterId())
                    .addToLabels(Resources.LABEL_CONNECTOR_ID, connector.getSpec().getConnectorId())
                    .addToLabels(Resources.LABEL_DEPLOYMENT_ID, connector.getSpec().getDeploymentId())
                    .build())
            .withSpec(
                new ServiceSpecBuilder()
                    .withType("NodePort")
                    .withPorts(
                        new ServicePortBuilder()
                            .withPort(8083)
                            .withTargetPort(new IntOrString("rest-api"))
                            .withProtocol("TCP")
                            .build())
                    .withSelector(
                        Map.of(
                            Resources.LABEL_CLUSTER_ID, connector.getSpec().getClusterId(),
                            Resources.LABEL_CONNECTOR_ID, connector.getSpec().getConnectorId(),
                            Resources.LABEL_DEPLOYMENT_ID, connector.getSpec().getDeploymentId()))
                    .build())
            .build();

        //
        // Kafka Connect
        //

        Secret connectSecret = new Secret();
        connectSecret.setMetadata(new ObjectMeta());
        connectSecret.getMetadata().setName(secretName(connector));
        connectSecret.setData(new TreeMap<>());

        {
            //
            // Connect
            //

            Map<String, String> params = new TreeMap<>();
            params.put("topic.creation.enable", "true");
            params.put("bootstrap.servers", connector.getSpec().getDeployment().getKafka().getUrl());
            params.put("security.protocol", "SASL_SSL");
            params.put("sasl.mechanism", "PLAIN");
            params.put("sasl.jaas.config", DebeziumOperandSupport.jaasConfig(serviceAccountSpec));
            params.put("producer.compression.type", "lz4");
            params.put("producer.security.protocol", "SASL_SSL");
            params.put("producer.sasl.mechanism", "PLAIN");
            params.put("producer.sasl.jaas.config", DebeziumOperandSupport.jaasConfig(serviceAccountSpec));
            params.put("admin.security.protocol", "SASL_SSL");
            params.put("admin.sasl.mechanism", "PLAIN");
            params.put("admin.sasl.jaas.config", DebeziumOperandSupport.jaasConfig(serviceAccountSpec));

            params.put("topic.creation.default.replication.factor", "-1");
            params.put("topic.creation.default.partitions", "-1");
            params.put("topic.creation.default.cleanup.policy", "compact");
            params.put("topic.creation.default.delete.retention.ms", "2678400000");

            // TODO: It may not be required
            params.put("plugin.path", "/opt/kafka/plugins");

            // TODO: At this stage, connector offset is stored on disk, we should explore an option
            //       to use kafka as a storage (i.e. https://github.com/lburgazzoli/quarkus-kc)
            params.put("offset.storage.file.filename", CONNECTOR_OFFSET_PATH);
            params.put("offset.flush.interval.ms", "10000");

            params.putAll(
                DEFAULT_CONFIG_OPTIONS);
            params.putAll(
                configuration.kafkaConnect().config());

            KeyAndValueConverters.getConfig(
                connectorConfiguration.getDataShapeSpec(),
                connector,
                serviceAccountSpec,
                configuration).forEach((k, v) -> params.put(k, (String) v));

            Secrets.set(connectSecret, CONNECT_CONFIG_FILENAME, DebeziumOperandSupport.mapAsString(params));
        }

        {

            //
            // Connector
            //

            Map<String, String> params = new TreeMap<>(createConnectorConfig(
                connector,
                configuration.kafkaConnector().config(),
                shardMetadata,
                connectorConfiguration,
                serviceAccountSpec));

            params.put("name", connector.getSpec().getConnectorId());
            params.put("tasks.max", "1");
            params.put("tasks", "1");
            params.put("connector.class", shardMetadata.getConnectorClass());

            Secrets.set(connectSecret, CONNECTOR_CONFIG_FILENAME, DebeziumOperandSupport.mapAsString(params));
        }

        //
        // Misc
        //

        ConfigMap connectConfig = new ConfigMap();
        connectConfig.setMetadata(new ObjectMeta());
        connectConfig.getMetadata().setName(configMapName(connector));
        connectConfig.setData(new TreeMap<>());

        ConfigMaps.set(
            connectConfig,
            DebeziumConstants.LOGGING_CONFIG_FILENAME,
            DebeziumConstants.LOGGING_CONFIG,
            connectorConfiguration.extractOverrideProperties().entrySet().stream().collect(
                Collectors.toMap(
                    e -> "log4j.logger." + e.getKey(),
                    e -> (String) e.getValue())));

        ConfigMaps.set(
            connectConfig,
            DebeziumConstants.METRICS_CONFIG_FILENAME,
            DebeziumConstants.METRICS_CONFIG);

        //
        // Deployment
        //

        Deployment connectorDeployment = new Deployment();
        connectorDeployment.setMetadata(new ObjectMeta());
        connectorDeployment.getMetadata().setName(connector.getMetadata().getName());

        //
        // Common spec
        //

        connectorDeployment.setSpec(new DeploymentSpec());
        connectorDeployment.getSpec().setProgressDeadlineSeconds(60);
        connectorDeployment.getSpec().setReplicas(1);
        connectorDeployment.getSpec().setRevisionHistoryLimit(10);
        connectorDeployment.getSpec().setSelector(selector(connector));
        connectorDeployment.getSpec().setStrategy(new DeploymentStrategy(null, "Recreate"));

        //
        // Pod Template
        //

        connectorDeployment.getSpec().setTemplate(new PodTemplateSpecBuilder()
            .withMetadata(
                new ObjectMetaBuilder()
                    .addToLabels(Resources.LABEL_CLUSTER_ID, connector.getSpec().getClusterId())
                    .addToLabels(Resources.LABEL_CONNECTOR_ID, connector.getSpec().getConnectorId())
                    .addToLabels(Resources.LABEL_DEPLOYMENT_ID, connector.getSpec().getDeploymentId())
                    .build())
            .withSpec(
                new PodSpecBuilder()
                    .withImagePullSecrets(configuration.imagePullSecretsName())
                    .withVolumes(
                        new VolumeBuilder()
                            .withName("connector-data")
                            .withPersistentVolumeClaim(
                                new PersistentVolumeClaimVolumeSource(connectPvc.getMetadata().getName(), false))
                            .build(),
                        volume(connectSecret),
                        volume(connectConfig))
                    .withContainers(new ContainerBuilder()
                        .withName("connector")
                        .withTerminationMessagePath("/dev/termination-log")
                        .withTerminationMessagePolicy("File")
                        .withImage(shardMetadata.getContainerImage())
                        .withImagePullPolicy("IfNotPresent")
                        // triggers a re-deployment upon secret/config change
                        .addToEnv(env("CONNECTOR_PARAMS_CHECKSUM", Secrets.computeChecksum(connectSecret)))
                        .addToEnv(env("CONNECTOR_RESOURCES_CHECKSUM", ConfigMaps.computeChecksum(connectConfig)))
                        .addToEnv(env("LOG_DIR", KAFKA_HOME))
                        .addToEnv(env("GC_LOG_ENABLED", "false"))
                        .addToEnv(env("KAFKA_GC_LOG_OPTS", " "))
                        .addToEnv(env("KAFKA_LOG4J_OPTS", "-Dlog4j.configuration=file:" + LOGGING_CONFIG_PATH))
                        // TODO: this is likely needed to be improved, ideally the jmx_prometheus_javaagent jar
                        //       should be on a dedicated directory with a stable name or the right jar should
                        //       be referenced in the connector's shard_meta as it is image specific
                        .addToEnv(env("KAFKA_OPTS",
                            "-javaagent:/opt/kafka/libs/jmx_prometheus_javaagent-0.17.2.redhat-00001.jar="
                                + PORT_PROMETHEUS
                                + ":"
                                + METRICS_CONFIG_PATH))
                        .withCommand(
                            "bin/connect-standalone.sh",
                            CONNECT_CONFIG_PATH,
                            CONNECTOR_CONFIG_PATH)
                        .withVolumeMounts(
                            mount(connectSecret, RHOC_SECRETS_HOME),
                            mount(connectConfig, RHOC_CONFIG_HOME),
                            mount("connector-data", RHOC_DATA_HOME))
                        .withPorts(
                            PORT_REST,
                            PORT_PROMETHEUS)
                        .withLivenessProbe(PROBE)
                        .withReadinessProbe(PROBE)
                        // TODO: this could go into the connector metadata so memory/cpu configuration can be
                        //       per connector type
                        .withResources(DEFAULT_RESOURCES)
                        .build())
                    .build())
            .build());

        getFleetShardOperatorConfig().metrics().recorder().tags().labels().stream().flatMap(List::stream).forEach(k -> {
            String v = Resources.getLabel(connector, k);
            if (v != null) {
                Resources.setLabel(connectorDeployment, k, v);
                Resources.setLabel(connectorDeployment.getSpec().getTemplate().getMetadata(), k, v);
            }
        });
        getFleetShardOperatorConfig().metrics().recorder().tags().annotations().stream().flatMap(List::stream).forEach(k -> {
            String v = Resources.getAnnotation(connector, k);
            if (v != null) {
                Resources.setAnnotation(connectorDeployment, k, v);
                Resources.setAnnotation(connectorDeployment.getSpec().getTemplate().getMetadata(), k, v);
            }
        });

        return List.of(
            connectPvc,
            connectService,
            connectSecret,
            connectConfig,
            connectorDeployment);
    }

    @Override
    public void status(ManagedConnector connector) {
        // first compute the kc conditions
        computeKafkaConnectCondition(
            connector,
            Resources.lookupDeployment(getKubernetesClient(), connector),
            Resources.lookupPod(getKubernetesClient(), connector));

        // ... then compute the kctr conditions
        try {
            client.status(connector).ifPresentOrElse(
                detail -> {
                    DebeziumOperandSupport.computeKafkaConnectorCondition(connector, detail);
                },
                () -> {
                    Conditions.set(
                        connector.getStatus().getConnectorStatus().getConditions(),
                        new ConditionBuilder()
                            .withType(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECTOR_READY)
                            .withStatus("False")
                            .withReason("ConnectorNotReady")
                            .withMessage("ConnectorNotReady")
                            .build());
                });
        } catch (Exception e) {
            LOGGER.warn("failure invoking kafka connect rest interface", e);

            Conditions.set(
                connector.getStatus().getConnectorStatus().getConditions(),
                new ConditionBuilder()
                    .withType(DebeziumConstants.CONDITION_TYPE_KAFKA_CONNECTOR_READY)
                    .withStatus("False")
                    .withReason("ConnectorNotReady")
                    .withMessage(e.getMessage() != null ? e.getMessage() : "ConnectorNotReady")
                    .build());
        }

        // ... then compute connector
        computeConnectorCondition(connector);
    }

    @Override
    public boolean stop(ManagedConnector connector) {
        getKubernetesClient().resources(Deployment.class)
            .inNamespace(connector.getMetadata().getNamespace())
            .withName(connector.getMetadata().getName())
            .delete();

        return getKubernetesClient().resources(Deployment.class)
            .inNamespace(connector.getMetadata().getNamespace())
            .withName(connector.getMetadata().getName())
            .get() == null;
    }

    @Override
    public boolean delete(ManagedConnector connector) {
        getKubernetesClient().resources(Deployment.class)
            .inNamespace(connector.getMetadata().getNamespace())
            .withName(connector.getMetadata().getName())
            .delete();

        return getKubernetesClient().resources(Deployment.class)
            .inNamespace(connector.getMetadata().getNamespace())
            .withName(connector.getMetadata().getName())
            .get() == null;
    }

    /**
     * This class is helper event handler with the goal of triggering reconciliation loops at a fixed rate.
     * This is required because Kafka Connect exposes information about running tasks through a REST interface.
     */
    public class Trigger implements EventSource {
        private final AtomicReference<EventHandler> handler = new AtomicReference<>();
        private ScheduledExecutorService scheduler;

        @Override
        public void setEventHandler(EventHandler handler) {
            this.handler.set(handler);
        }

        @Override
        public void start() throws OperatorException {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(this::run, 5, 5, TimeUnit.SECONDS);
        }

        @Override
        public void stop() throws OperatorException {
            if (scheduler != null) {
                scheduler.shutdownNow();
                scheduler = null;
            }
        }

        public void run() {
            if (handler.get() == null) {
                return;
            }

            var resources = getKubernetesClient().resources(ManagedConnector.class)
                .inAnyNamespace()
                .list();

            if (resources.getItems() != null) {
                for (var resource : resources.getItems()) {

                    // The connector moves among a number of technical phases for which it is not necessary
                    // to trigger additional events.
                    if (ManagedConnectorStatus.PhaseType.Monitor == resource.getStatus().getPhase()) {
                        LOGGER.info("Trigger -> {}", ResourceID.fromResource(resource));

                        this.handler.get().handleEvent(
                            new Event(ResourceID.fromResource(resource)));
                    }
                }
            }
        }
    }

    private PersistentVolumeClaim pvc(ManagedConnector connector) {
        var pvcs = getKubernetesClient().persistentVolumeClaims();

        PersistentVolumeClaim pvc = null;

        if (pvcs != null) {
            pvc = pvcs.inNamespace(connector.getMetadata().getNamespace())
                .withName(connector.getMetadata().getName())
                .get();
        }

        Quantity os = configuration.kafkaConnect().offset().storage();

        if (pvc == null) {
            pvc = new PersistentVolumeClaimBuilder()
                .withMetadata(new ObjectMetaBuilder().withName(connector.getMetadata().getName()).build())
                .withSpec(new PersistentVolumeClaimSpecBuilder()
                    .withAccessModes("ReadWriteOnce")
                    .withVolumeMode("Filesystem")
                    .withResources(new ResourceRequirementsBuilder()
                        .withRequests(Map.of("storage", os))
                        .build())
                    .build())
                .build();
        } else {
            if (!os.equals(pvc.getSpec().getResources().getRequests().get("storage"))) {
                pvc.getSpec().getResources().setRequests(Map.of("storage", new Quantity("50Mi")));
            }
        }

        return pvc;
    }
}
