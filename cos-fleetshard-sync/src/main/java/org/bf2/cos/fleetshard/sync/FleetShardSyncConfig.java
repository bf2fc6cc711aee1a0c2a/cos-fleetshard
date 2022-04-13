package org.bf2.cos.fleetshard.sync;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.support.DurationConverter;
import org.bf2.cos.fleetshard.sync.resources.ConnectorNamespaceProvisioner;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "cos")
public interface FleetShardSyncConfig {
    /**
     * The main cos namespace.
     *
     * @return the namespace.
     */
    String namespace();

    @WithDefault(ConnectorNamespaceProvisioner.DEFAULT_ADDON_PULLSECRET_NAME)
    String imagePullSecretsName();

    /**
     * Configuration options for the {@link ManagedConnectorCluster}
     *
     * @return {@link Cluster}
     */
    Cluster cluster();

    /**
     * Configuration options for connectors.
     *
     * @return {@link Connectors}
     */
    Connectors connectors();

    /**
     * Configuration options for resources.
     *
     * @return {@link Resources}
     */
    Resources resources();

    /**
     * Configuration options for the addon.
     *
     * @return {@link Addon}
     */
    Addon addon();

    /**
     * Metrics configuration options.
     *
     * @return {@link Connectors}
     */
    Metrics metrics();

    /**
     * Configuration options for the fleet manager.
     *
     * @return {@link Connectors}
     */
    Manager manager();

    /**
     * Configuration options for the tenancyr.
     *
     * @return {@link Tenancy}
     */
    Tenancy tenancy();

    interface Cluster {
        /**
         * The ID assigned to the operator.
         * </p>
         * This value is used by the operator to create a {@link ManagedConnectorCluster} CR upon startup and it is added to any
         * {@link ManagedConnector} that is created by this synchronizer instance.
         *
         * @return the cluster id.
         */
        String id();
    }

    interface Tenancy {
        @WithDefault("redhat-openshift-connectors")
        String namespacePrefix();
    }

    interface Resources {
        /**
         * Determine how often the synchronizer should poll the Control Plane for resources
         *
         * @return the poll interval
         */
        @WithDefault("15s")
        @WithConverter(DurationConverter.class)
        Duration pollInterval();

        /**
         * Determine how often the synchronizer should re-sync resources with the Control Plane.
         *
         * @return the re-sync interval
         */
        @WithDefault("60s")
        @WithConverter(DurationConverter.class)
        Duration resyncInterval();

        /**
         * Determine how often the synchronizer should update the status resources to the Control Plane.
         *
         * @return the timeout.
         */
        @WithDefault("15s")
        @WithConverter(DurationConverter.class)
        Duration updateInterval();

        /**
         * Determine how often the synchronizer should perform house keeping tasks.
         *
         * @return the housekeeping interval
         */
        @WithDefault("30s")
        @WithConverter(DurationConverter.class)
        Duration housekeeperInterval();
    }

    interface Addon {
        /**
         * Determine how often the synchronizer should try to cleanup resources when the addon is being removed.
         *
         * @return the timeout.
         */
        @WithDefault("10s")
        @WithConverter(DurationConverter.class)
        Duration cleanupRetryInterval();

        /**
         * Determine how many times the application should keep trying to delete the created namespaces before
         * giving up and letting the addon proceed with uninstall of the operators. Keep in mind that unwanted
         * namespaces might be left on the cluster if this limit is reached.
         *
         * @return the maximum amount of retries.
         */
        @WithDefault("12")
        long cleanupRetryLimit();

        /**
         * The configured id for the addon that installed the sync application in the cluster.
         *
         * @return the addon id.
         */
        @WithDefault("connectors-operator")
        String id();

        /**
         * The configured label for the addon that installed the sync application in the cluster.
         *
         * @return the addon label.
         */
        @WithDefault("addon-connectors-operator")
        String label();

        @WithDefault("operators.coreos.com")
        String olmOperatorsGroup();

        @WithDefault("v1alpha1")
        String olmOperatorsApiVersion();

        @WithDefault("ClusterServiceVersion")
        String olmOperatorsKind();
    }

    interface Connectors {
        /**
         * An optional map of additional labels to be added to the generated {@link ManagedConnector}.
         *
         * @return the additional labels
         */
        Map<String, String> labels();

        /**
         * An optional map of additional annotations to be added to the generated {@link ManagedConnector}.
         *
         * @return the additional annotations
         */
        Map<String, String> annotations();
    }

    interface Metrics {
        /**
         * The base name for metrics created by the operator.
         *
         * @return the base name.
         */
        @WithDefault("cos.fleetshard.sync")
        String baseName();
    }

    interface Manager {
        /**
         * The {@link URI} of the Control Plane.
         *
         * @return the control plane {@link URI}.
         */
        URI uri();

        /**
         * The connect timeout;
         *
         * @return the timeout.
         */
        @WithDefault("5s")
        @WithConverter(DurationConverter.class)
        Duration connectTimeout();

        /**
         * The read timeout;
         *
         * @return the timeout.
         */
        @WithDefault("10s")
        @WithConverter(DurationConverter.class)
        Duration readTimeout();
    }
}
