package org.bf2.cos.fleetshard.operator.debezium;

import java.text.MessageFormat;

public class DebeziumArtifactUri {

    private static final String MAVEN_URL_TEMPLATE = "https://repo1.maven.org/maven2/io/debezium/debezium-connector-{0}/{1}/debezium-connector-{0}-{1}-plugin.tar.gz";

    public static String getMavenCentralUri(String connectorName, String connectorVersion) {
        return MessageFormat.format(MAVEN_URL_TEMPLATE, connectorName, connectorVersion);
    }
}
