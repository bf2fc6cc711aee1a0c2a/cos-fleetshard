package org.bf2.cos.fleet.manager.client;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorCluster;
import org.bf2.cos.fleetshard.support.DurationConverter;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "cos")
public interface ClientConfig {
    /**
     * Configuration options for the {@link ManagedConnectorCluster}
     *
     * @return {@link Cluster}
     */
    Cluster cluster();

    /**
     * Configuration options for the fleet manager.
     *
     * @return {@link Manager}
     */
    Manager manager();

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

    interface Manager {
        /**
         * The {@link URI} of the Control Plane.
         *
         * @return the control plane {@link URI}.
         */
        URI uri();

        /**
         * The {@link URI} of the SSO.
         *
         * @return the sso {@link URI}.
         */
        Optional<URI> ssoUri();

        /**
         * The {@link URI} of the endpoint to discover the SSO URI.
         *
         * @return the sso {@link URI}.
         */
        URI ssoProviderUri();

        /**
         * The timeout for sso provider refresh;
         *
         * @return the timeout.
         */
        @WithDefault("1h")
        @WithConverter(DurationConverter.class)
        Duration ssoProviderRefreshTimeout();

        /**
         * The timeout for sso operations;
         *
         * @return the timeout.
         */
        @WithDefault("10s")
        @WithConverter(DurationConverter.class)
        Duration ssoTimeout();

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
