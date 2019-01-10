package com.amplify.ap.domain;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class ResourceInstance {

    @NotNull
    private String templateId;

    @NotNull
    private String resourceId;

    private String connectionUrl;

    @NotNull
    private String notificationEmail;

    @NotNull
    private TemplateInstanceStatus status;

    @NotNull
    private ResourceType resourceType;

    public ResourceInstance(String templateId, String resourceId, String notificationEmail, TemplateInstanceStatus status, ResourceType resourceType) {
        this.templateId = templateId;
        this.resourceId = resourceId;
        this.notificationEmail = notificationEmail;
        this.status = status;
        this.resourceType = resourceType;
    }

    public static String generateID() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 15).toLowerCase();
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getResourceId() {
        return resourceId;
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

    public ResourceType getResourceType() {
        return resourceType;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ResourceInstance{");
        sb.append("templateId='").append(templateId).append('\'');
        sb.append(", resourceId='").append(resourceId).append('\'');
        sb.append(", connectionUrl='").append(connectionUrl).append('\'');
        sb.append(", notificationEmail='").append(notificationEmail).append('\'');
        sb.append(", status=").append(status);
        sb.append(", resourceType=").append(resourceType);
        sb.append('}');
        return sb.toString();
    }
}
