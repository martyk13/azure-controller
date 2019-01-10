package com.amplify.ap.domain;

import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Map;

public class ResourceGroup {

    @Id
    private String resourceGroupName;

    @NotNull
    @Size(min = 1)
    private Map<String, ResourceInstance> resourceInstances;

    public ResourceGroup(String resourceGroupName, Map<String, ResourceInstance> resourceInstances) {
        this.resourceGroupName = resourceGroupName;
        this.resourceInstances = resourceInstances;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public Map<String, ResourceInstance> getResourceInstances() {
        return resourceInstances;
    }

    public void addResourceInstance(ResourceInstance resourceInstance) {
        this.resourceInstances.put(resourceInstance.getResourceId(), resourceInstance);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ResourceGroup{");
        sb.append("resourceGroupName='").append(resourceGroupName).append('\'');
        sb.append(", resourceInstances=").append(resourceInstances);
        sb.append('}');
        return sb.toString();
    }
}
