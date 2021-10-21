package org.bf2.cos.fleetshard.operator;

import java.util.Optional;
import java.util.Set;

import org.bf2.cos.fleetshard.api.ManagedConnector;
import org.bf2.cos.fleetshard.api.ManagedConnectorOperator;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "cos")
public interface FleetShardOperatorConfig {
    /**
     * Configuration options for the {@link ManagedConnectorOperator}
     *
     * @return {@link Operator}
     */
    Operator operator();

    /**
     * Configuration options for operators.
     *
     * @return {@link Operators}
     */
    Operators operators();

    /**
     * Configuration options for connectors.
     *
     * @return {@link Connectors}
     */
    Connectors connectors();

    /**
     * Metrics configuration options.
     *
     * @return {@link Connectors}
     */
    Metrics metrics();

    interface Operator {
        /**
         * The ID assigned to the operator.
         * </p>
         * This value is used by the operator to create a {@link ManagedConnectorOperator} CR upon startup.
         *
         * @return the operator id.
         */
        String id();

        /**
         * The Version assigned to the operator.
         * </p>
         * This value is used by the operator to create a {@link ManagedConnectorOperator} CR upon startup.
         *
         * @return the operator version.
         */
        String version();
    }

    interface Operators {
        /**
         * The namespace where {@link ManagedConnectorOperator} are placed.
         *
         * @return the namespace.
         */
        String namespace();
    }

    interface Connectors {
        /**
         * The namespace where {@link ManagedConnector} are placed.
         *
         * @return the namespace.
         */
        String namespace();

        /**
         * An optional set of labels to be transferred to the generated resources
         *
         * @return the list fo labels
         */
        Optional<Set<String>> targetLabels();

        /**
         * An optional set of annotations to be transferred to the generated resources
         *
         * @return the list fo annotations
         */
        Optional<Set<String>> targetAnnotations();
    }

    interface Metrics {
        /**
         * The base name for metrics created by the operator.
         *
         * @return the base name.
         */
        @WithDefault("cos.fleetshard")
        String baseName();

        ConnectorOperand connectorOperand();

        interface ConnectorOperand {
            /**
             * Determine if the metrics for {@link ConnectorOperand} are enabled or not.
             *
             * @return true if the {@link ConnectorOperand} are enabled.
             */
            @WithDefault("false")
            boolean enabled();
        }
    }
}
