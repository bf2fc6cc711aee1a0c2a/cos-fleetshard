package org.bf2.cos.fleetshard.support.metrics;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MetricsRecorderConfig {

    Tags tags();

    interface Tags {
        /**
         * List common tags that should be added to any metric.
         *
         * @return the list of labels.
         */
        Map<String, String> common();

        /**
         * List the Kubernetes labels that should be propagated as metrics tags.
         * Labels are sanitized to include only allowed characters.
         *
         * @return the list of labels.
         */
        Optional<List<String>> labels();

        /**
         * List the Kubernetes annotations that should be propagated as metrics tags.
         * Annotations are sanitized to include only allowed characters.
         *
         * @return the list of labels.
         */
        Optional<List<String>> annotations();
    }
}
