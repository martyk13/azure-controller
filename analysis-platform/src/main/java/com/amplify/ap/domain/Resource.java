package com.amplify.ap.domain;

import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

public class Resource {

    @Id
    private String resourceGroup;

    @NotNull
    @Size(min = 1)
    private List<TemplateInstance> templateInstances;

    public Resource(String resourceGroup, @NotNull @Size(min = 1) List<TemplateInstance> templateInstances) {
        this.resourceGroup = resourceGroup;
        this.templateInstances = templateInstances;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public List<TemplateInstance> getTemplateInstances() {
        return templateInstances;
    }

    public void addTemplateInstance(TemplateInstance templateInstance) {
        this.templateInstances.add(templateInstance);
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
