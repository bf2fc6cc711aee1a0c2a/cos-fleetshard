package org.bf2.cos.fleetshard.operator.connector;

import java.io.StringReader;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.bf2.cos.fleetshard.support.json.JacksonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.ConfigMap;

public class ConnectorConfiguration<S, D> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorConfiguration.class);
    private static final String PROPERTY_DATA_SHAPE = "data_shape";
    private static final String PROPERTY_PROCESSORS = "processors";
    private static final String PROPERTY_ERROR_HANDLER = "error_handler";
    private static final Properties EMPTY_PROPERTIES = new Properties(0);

    private final S connectorSpec;
    private final D dataShapeSpec;
    private final ObjectNode errorHandlerSpec;
    private final ArrayNode processorsSpec;
    private final ConfigMap configMap;

    public ConnectorConfiguration(ObjectNode connectorSpec, Class<S> connectorSpecType, Class<D> dataShapeType,
        ConfigMap configMap) {
        if (null == connectorSpec || connectorSpec.isEmpty()) {
            throw new RuntimeException("Connector spec can't be empty!");
        }

        this.configMap = configMap;

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

    public Properties extractOverrideProperties() {
        final ConfigMap configMap = this.getConfigMap();
        if (configMap == null) {
            return EMPTY_PROPERTIES;
        }

        final Map<String, String> data = configMap.getData();
        if (data == null || data.isEmpty()) {
            return EMPTY_PROPERTIES;
        }

        String propertiesAsStr = data.get("override.properties");
        if (propertiesAsStr == null) {
            LOGGER.error("Connector ConfigMap can only have properties in a override.properties embedded file."
                + "Current content will be ignored: {}", data);
            return EMPTY_PROPERTIES;
        }
        propertiesAsStr = propertiesAsStr.replace("|-", "");

        Properties contents = new Properties();
        try {
            contents.load(new StringReader(propertiesAsStr));
            LOGGER.info("ConfigMap ({}/{}) for connector contains data: {}",
                configMap.getMetadata().getNamespace(),
                configMap.getMetadata().getName(),
                StringUtils.normalizeSpace(contents.toString()));
            return contents;
        } catch (Exception e) {
            LOGGER.error(
                "Unable to read properties from override.properties embedded in ConfigMap. Properties will get ignored.",
                e);
            return EMPTY_PROPERTIES;
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

    public ConfigMap getConfigMap() {
        return this.configMap;
    }

}
