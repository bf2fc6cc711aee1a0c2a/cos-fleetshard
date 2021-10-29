package org.bf2.cos.fleetshard.operator.camel.model;

public class ProcessorKamelet {
    private final String templateId;
    private final String id;

    public ProcessorKamelet(String templateId, String id) {
        this.templateId = templateId;
        this.id = id;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getId() {
        return id;
    }
}
