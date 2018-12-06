package com.amplify.ap.domain;

import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Map;

public class Resource {

    @Id
    private String resourceGroup;

    @NotNull
    @Size(min = 1)
    private Map<String, TemplateInstance> templateInstances;

    public Resource(String resourceGroup, Map<String, TemplateInstance> templateInstances) {
        this.resourceGroup = resourceGroup;
        this.templateInstances = templateInstances;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public Map<String, TemplateInstance> getTemplateInstances() {
        return templateInstances;
    }

    public void addTemplateInstance(TemplateInstance templateInstance) {
        this.templateInstances.put(templateInstance.getInstanceId(), templateInstance);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Resource{");
        sb.append("resourceGroup='").append(resourceGroup).append('\'');
        sb.append(", templateInstances=").append(templateInstances);
        sb.append('}');
        return sb.toString();
    }
}
