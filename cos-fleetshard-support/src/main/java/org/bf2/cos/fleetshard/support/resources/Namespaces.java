package org.bf2.cos.fleetshard.support.resources;

public final class Namespaces {
    public static String STATUS_ACTIVE = "Active";
    public static String STATUS_TERMINATING = "Terminating";

    public static String PHASE_READY = "ready";
    public static String PHASE_DELETED = "deleted";
    public static String PHASE_PROVISIONING = "provisioning";
    public static String PHASE_DEPROVISIONING = "deprovisioning";

    private Namespaces() {
    }

    public static String generateNamespaceId(String id) {
        if (id.startsWith(Resources.CONNECTOR_PREFIX)) {
            return id;
        }

        return Resources.CONNECTOR_PREFIX + id;
    }

}
