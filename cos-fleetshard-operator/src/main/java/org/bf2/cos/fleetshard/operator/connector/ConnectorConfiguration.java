package org.bf2.cos.fleetshard.operator.connector;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.client.utils.Serialization;

public class ConnectorConfiguration<S> {

    private final static String PROPERTY_DATA_SHAPE = "data_shape";
    private final static String PROPERTY_PROCESSORS = "processors";
    private final static String PROPERTY_ERROR_HANDLER = "error_handler";
    private static final Set<String> RESERVED_PROPERTIES = Set.of(PROPERTY_DATA_SHAPE, PROPERTY_PROCESSORS,
        PROPERTY_ERROR_HANDLER);
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final S connectorSpec;
    private final ObjectNode dataShapeSpec;
    private ObjectNode errorHandlerSpec;
    private ArrayNode processorsSpec = JSON_MAPPER.createArrayNode();

    @SuppressWarnings("unchecked")
    public ConnectorConfiguration(ObjectNode connectorSpec, Class<S> type) {
        if (null == connectorSpec || connectorSpec.isEmpty()) {
            throw new RuntimeException("Connector spec can't be empty!");
        }

        var dataShape = connectorSpec.at("/" + PROPERTY_DATA_SHAPE);
        if (dataShape.isMissingNode()) {
            throw new RuntimeException("Missing `data_shape` in connector spec: " + connectorSpec.toPrettyString());
        }
        this.dataShapeSpec = dataShape.deepCopy();

        var errorHandler = connectorSpec.at("/" + PROPERTY_ERROR_HANDLER);
        if (!errorHandler.isMissingNode()) {
            this.errorHandlerSpec = errorHandler.deepCopy();
        }

        var processors = connectorSpec.at("/" + PROPERTY_PROCESSORS);
        if (!processors.isMissingNode()) {
            this.processorsSpec = processors.deepCopy();
        }

        connectorSpec.remove(RESERVED_PROPERTIES);

        if (type.isAssignableFrom(String.class)) {
            this.connectorSpec = (S) connectorSpec.toString();
        } else if (type.isAssignableFrom(Properties.class)) {
            Properties result = new Properties();
            try {
                result.load(new StringReader(connectorSpec.toString()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.connectorSpec = (S) result;
        } else {
            this.connectorSpec = Serialization.unmarshal(connectorSpec.toString(), type);
        }
    }

    public S getConnectorSpec() {
        return this.connectorSpec;
    }

    public ObjectNode getDataShapeSpec() {
        return this.dataShapeSpec;
    }

    public ArrayNode getProcessorsSpec() {
        return this.processorsSpec;
    }

    public ObjectNode getErrorHandlerSpec() {
        return this.errorHandlerSpec;
    }

}
