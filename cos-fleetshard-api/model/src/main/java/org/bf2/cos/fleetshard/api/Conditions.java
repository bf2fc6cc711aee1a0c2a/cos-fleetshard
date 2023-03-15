package org.bf2.cos.fleetshard.api;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class Conditions {
    // types
    public static final String TYPE_READY = "Ready";

    // statuses
    public static final String STATUS_TRUE = "True";
    public static final String STATUS_FALSE = "False";

    // reasons
    public static final String NO_ASSIGNABLE_OPERATOR_REASON = "NoAssignableOperator";
    public static final String FAILED_TO_CREATE_OR_UPDATE_RESOURCE_REASON = "FailedToCreateOrUpdateResource";

    private Conditions() {
    }

    public static String now() {
        return ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    }

}
