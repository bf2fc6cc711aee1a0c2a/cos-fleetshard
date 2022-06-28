package org.bf2.cos.fleetshard.operator.connector;

import org.bf2.cos.fleetshard.support.json.JacksonUtil;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ConnectorConfiguration<S, D> {
    private static final String PROPERTY_DATA_SHAPE = "data_shape";
    private static final String PROPERTY_PROCESSORS = "processors";
    private static final String PROPERTY_ERROR_HANDLER = "error_handler";

    private final S connectorSpec;
    private final D dataShapeSpec;
    private final ObjectNode errorHandlerSpec;
    private final ArrayNode processorsSpec;

    public ConnectorConfiguration(ObjectNode connectorSpec, Class<S> connectorSpecType, Class<D> dataShapeType) {
        if (null == connectorSpec || connectorSpec.isEmpty()) {
            throw new RuntimeException("Connector spec can't be empty!");
        }

        var dataShape = connectorSpec.remove(PROPERTY_DATA_SHAPE);
        if (dataShape != null) {
            this.dataShapeSpec = JacksonUtil.treeToValue(dataShape, dataShapeType);
        } else {
            this.dataShapeSpec = null;
        }

        var errorHandler = connectorSpec.remove(PROPERTY_ERROR_HANDLER);
        if (errorHandler != null) {
            this.errorHandlerSpec = errorHandler.deepCopy();
        } else {
            this.errorHandlerSpec = null;
        }

        var processors = connectorSpec.remove(PROPERTY_PROCESSORS);
        if (processors != null) {
            this.processorsSpec = processors.deepCopy();
        } else {
            this.processorsSpec = JacksonUtil.asArrayNode();
        }

        this.connectorSpec = JacksonUtil.treeToValue(connectorSpec, connectorSpecType);
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
