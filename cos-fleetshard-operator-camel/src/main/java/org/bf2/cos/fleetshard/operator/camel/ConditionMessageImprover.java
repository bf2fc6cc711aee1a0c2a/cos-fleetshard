package org.bf2.cos.fleetshard.operator.camel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Condition;

public final class ConditionMessageImprover {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConditionMessageImprover.class);

    // Pattern for beautifully extracting the error.message field value.
    public static final Pattern CAMEL_ERROR_MESSAGE_PATTERN = Pattern
        .compile("(error\\.message:)([\\s\\S]*?)(error\\.|]|check.|route.)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    // Pattern for beautifully extracting the bootstrap.servers field value.
    public static final Pattern CAMEL_BOOTSTRAP_SERVER_PATTERN = Pattern
        .compile("(bootstrap\\.servers:)([\\s\\S]*?)(error\\.|]|check.|route.)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    // Pattern for beautifully extracting the root cause (last Caused by) message.
    public static final Pattern CAMEL_EXCEPTION_PATTERN = Pattern.compile(
        "(Caused by:(?![\\s\\S]*Caused by:))([\\s\\S]*Exception:)(\\s*)(.*?)([\\n\\r\\t]|\\\\n)",
        Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private static final Map<String, Function<String, String>> PATTERN_TO_PROCESSOR = new HashMap<>();
    private static final Set<Map.Entry<String, Function<String, String>>> PROCESSORS;
    static {
        PATTERN_TO_PROCESSOR.put("No resolvable bootstrap urls", message -> {
            final StringBuilder finalMessage = new StringBuilder("The kafka url is unreachable");
            final Matcher matcher = CAMEL_BOOTSTRAP_SERVER_PATTERN.matcher(message);

            if (matcher.find()) {
                final String bootstrapServers = matcher.group(2).strip();
                if (bootstrapServers != null && !bootstrapServers.isBlank()) {
                    finalMessage.append(": ");
                    finalMessage.append(bootstrapServers);
                }
            }

            finalMessage.append(". Please check if the kafka instance is running and reachable.");
            return finalMessage.toString();
        });

        PROCESSORS = PATTERN_TO_PROCESSOR.entrySet();
    }

    private ConditionMessageImprover() {
    }

    public static void improve(Condition condition) {
        final StringBuilder finalMessage = new StringBuilder();
        final String originalMessage = condition.getMessage();

        if (StringUtils.isNotBlank(originalMessage)) {
            for (Map.Entry<String, Function<String, String>> processorEntry : PROCESSORS) {
                if (originalMessage.contains(processorEntry.getKey())) {
                    final String betterMessage = processorEntry.getValue().apply(originalMessage);
                    finalMessage.append(betterMessage);
                    finalMessage.append("\n");
                }
            }

            if (originalMessage.contains("error.message")) {
                final Matcher matcher = CAMEL_ERROR_MESSAGE_PATTERN.matcher(originalMessage);
                if (matcher.find()) {
                    final String camelErrorMessage = matcher.group(2).strip();
                    if (finalMessage.length() > 0) {
                        finalMessage.append("(Message: ");
                    }
                    finalMessage.append(camelErrorMessage);
                    finalMessage.append(") \n");
                } else {
                    LOGGER.warn("Error message contains 'error.message' field but regex wasn't able to extract the value.");
                }
            }

            if (originalMessage.contains("Caused by")) {
                final Matcher matcher = CAMEL_EXCEPTION_PATTERN.matcher(originalMessage);
                if (matcher.find()) {
                    final String rootCauseMessage = matcher.group(4).strip();
                    if (finalMessage.length() > 0) {
                        finalMessage.append("(Exception: ");
                    }
                    finalMessage.append(rootCauseMessage);
                    finalMessage.append(")");
                } else {
                    LOGGER.warn("Error message contains 'Caused by' field but regex wasn't able to extract the value.");
                }
            }
        }

        if (finalMessage.length() <= 0) {
            finalMessage.append("Unknown error: ");
            finalMessage.append(originalMessage);
        }

        condition.setMessage(finalMessage.toString());
    }

}
