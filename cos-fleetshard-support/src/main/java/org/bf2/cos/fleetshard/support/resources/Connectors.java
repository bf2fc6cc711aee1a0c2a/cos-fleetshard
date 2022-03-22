package org.bf2.cos.fleetshard.support.resources;

public final class Connectors {

    private Connectors() {
    }

    public static String generateConnectorId(String id) {
        if (id.startsWith(Resources.CONNECTOR_PREFIX)) {
            return id;
        }

        return Resources.CONNECTOR_PREFIX + id;
    }
}
