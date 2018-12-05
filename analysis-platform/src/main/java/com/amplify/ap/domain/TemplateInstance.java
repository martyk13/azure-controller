package com.amplify.ap.domain;

import javax.validation.constraints.NotNull;

public class TemplateInstance {

    @NotNull
    private String templateId;

    @NotNull
    private String instanceId;

    @NotNull
    private String connectionUrl;

    public TemplateInstance(@NotNull String templateId, @NotNull String instanceId) {
        this.templateId = templateId;
        this.instanceId = instanceId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TemplateInstance{");
        sb.append("templateId='").append(templateId).append('\'');
        sb.append(", instanceId='").append(instanceId).append('\'');
        sb.append(", connectionUrl='").append(connectionUrl).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
