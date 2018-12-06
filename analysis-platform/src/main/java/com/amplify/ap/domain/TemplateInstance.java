package com.amplify.ap.domain;

import javax.validation.constraints.NotNull;

public class TemplateInstance {

    @NotNull
    private String templateId;

    @NotNull
    private String instanceId;

    private String connectionUrl;

    @NotNull
    private String notificationEmail;

    @NotNull
    private TemplateInstanceStatus status;

    public TemplateInstance(String templateId, String instanceId, String notificationEmail, TemplateInstanceStatus status) {
        this.templateId = templateId;
        this.instanceId = instanceId;
        this.notificationEmail = notificationEmail;
        this.status = status;
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

    public String getNotificationEmail() {
        return notificationEmail;
    }

    public TemplateInstanceStatus getStatus() {
        return status;
    }

    public void setStatus(TemplateInstanceStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TemplateInstance{");
        sb.append("templateId='").append(templateId).append('\'');
        sb.append(", instanceId='").append(instanceId).append('\'');
        sb.append(", connectionUrl='").append(connectionUrl).append('\'');
        sb.append(", notificationEmail='").append(notificationEmail).append('\'');
        sb.append(", status=").append(status);
        sb.append('}');
        return sb.toString();
    }
}
