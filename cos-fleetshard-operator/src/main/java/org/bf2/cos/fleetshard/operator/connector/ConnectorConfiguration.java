package org.bf2.cos.fleetshard.operator.connector;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ConnectorConfiguration<S, D> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorConfiguration.class);

    private static final String PROPERTY_DATA_SHAPE = "data_shape";
    private static final String PROPERTY_PROCESSORS = "processors";
    private static final String PROPERTY_ERROR_HANDLER = "error_handler";
    private static final Set<String> RESERVED_PROPERTIES = Set.of(PROPERTY_DATA_SHAPE, PROPERTY_PROCESSORS,
        PROPERTY_ERROR_HANDLER);
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final S connectorSpec;
    private final D dataShapeSpec;
    private final ObjectNode errorHandlerSpec;
    private final ArrayNode processorsSpec;

    @SuppressWarnings("unchecked")
    public ConnectorConfiguration(ObjectNode connectorSpec, Class<S> connectorSpecType, Class<D> dataShapeType) {
        if (null == connectorSpec || connectorSpec.isEmpty()) {
            throw new RuntimeException("Connector spec can't be empty!");
        }

        var dataShape = connectorSpec.at("/" + PROPERTY_DATA_SHAPE);
        if (dataShape.isMissingNode()) {
            // @TODO Eventually, once most/all connectors support `data_shape`, the exception should be enabled and error logged in `AbstractOperandController` instead
            // throw new IncompleteConnectorSpecException("Missing `data_shape` in connector spec!");
            LOGGER.error("Missing `data_shape` in connector spec!");
            this.dataShapeSpec = null;
        } else {
            try {
                this.dataShapeSpec = JSON_MAPPER.readValue(dataShape.toString(), dataShapeType);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        var errorHandler = connectorSpec.at("/" + PROPERTY_ERROR_HANDLER);
        if (!errorHandler.isMissingNode()) {
            this.errorHandlerSpec = errorHandler.deepCopy();
        } else {
            // @TODO Eventually, when `error_handler` becomes mandatory and most/all connectors support `error_handler` the exception should be enabled and error logged in `AbstractOperandController` instead
            // throw new IncompleteConnectorSpecException("Missing `error_handler` in connector spec!");
            LOGGER.warn("Missing `error_handler` in connector spec!");
            this.errorHandlerSpec = null;
        }

        var processors = connectorSpec.at("/" + PROPERTY_PROCESSORS);
        if (!processors.isMissingNode()) {
            this.processorsSpec = processors.deepCopy();
        } else {
            this.processorsSpec = JSON_MAPPER.createArrayNode();
        }

        connectorSpec.remove(RESERVED_PROPERTIES);

        if (connectorSpecType.isAssignableFrom(String.class)) {
            this.connectorSpec = (S) connectorSpec.toString();
        } else {
            try {
                this.connectorSpec = JSON_MAPPER.readValue(connectorSpec.toString(), connectorSpecType);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public S getConnectorSpec() {
        return this.connectorSpec;
    }

    public D getDataShapeSpec() {
        return this.dataShapeSpec;
    }

    public ArrayNode getProcessorsSpec() {
        return this.processorsSpec;
    }

    public ObjectNode getErrorHandlerSpec() {
        return this.errorHandlerSpec;
    }

}
