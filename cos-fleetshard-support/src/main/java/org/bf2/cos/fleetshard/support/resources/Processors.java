package org.bf2.cos.fleetshard.support.resources;

public final class Processors {

    private Processors() {
    }

    public static String generateProcessorId(String id) {
        if (id.startsWith(Resources.PROCESSOR_PREFIX)) {
            return id;
        }

        return Resources.PROCESSOR_PREFIX + id;
    }
}
