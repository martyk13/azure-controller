package com.amplify.ap.api;

import com.amplify.ap.dao.ResourceDao;
import com.amplify.ap.dao.TemplateDao;
import com.amplify.ap.domain.ResourceGroup;
import com.amplify.ap.domain.ResourceInstance;
import com.amplify.ap.domain.TemplateInstanceStatus;
import com.amplify.ap.services.resources.ResourceService;
import com.amplify.ap.services.templates.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/resources")
public class ResourcesApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcesApi.class);

    @Autowired
    private ResourceDao resourceDao;

    @Autowired
    private TemplateDao templateDao;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private TemplateService templateService;

    @RequestMapping(method = RequestMethod.GET)
    public List<ResourceGroup> getAllResources() {
        return resourceDao.findAll();
    }

    @RequestMapping(method = RequestMethod.POST)
    public void createResources(@RequestParam(name = "resource-group") String resourceGroupName,
                                @RequestParam(name = "template-id") String templateId,
                                @RequestParam(name = "resource-type") String resourceType,
                                @RequestParam(name = "notification-email") String notificationEmail) throws IOException {

        if (templateDao.existsById(templateId)) {
            ResourceInstance newResourceInstance = new ResourceInstance(templateId, UUID.randomUUID().toString(),
                    notificationEmail, TemplateInstanceStatus.CREATING);
            HttpStatus status = resourceService.requestResource(resourceGroupName, newResourceInstance.getResourceId(),
                    templateService.getTemplateFile(templateId), resourceType);

            if (HttpStatus.OK.equals(status)) {
                if (!resourceDao.existsById(resourceGroupName)) {
                    Map<String, ResourceInstance> templateInstances = new HashMap<>();
                    templateInstances.put(newResourceInstance.getResourceId(), newResourceInstance);
                    ResourceGroup resourceGroup = new ResourceGroup(resourceGroupName, templateInstances);
                    resourceDao.save(resourceGroup);
                } else {
                    ResourceGroup resource = resourceDao.findById(resourceGroupName).get();
                    resource.addResourceInstance(newResourceInstance);
                    resourceDao.save(resource);
                }
            } else {
                throw new RuntimeException("Request to create resource failed with error code " + status);
            }
        } else {
            throw new IllegalArgumentException("Template ID: " + templateId + " does not exist");
        }
    }

    @RequestMapping(value = "/{resource-group}", method = RequestMethod.DELETE)
    public void deleteResourceGroup(@RequestParam(name = "resource-group") String resourceGroupName) {
        if (resourceDao.existsById(resourceGroupName)) {
            ResourceGroup resourceGroup = resourceDao.findById(resourceGroupName).get();
            resourceService.deleteResource(resourceGroup);
            for (String instanceID : resourceGroup.getResourceInstances().keySet()) {
                resourceGroup.getResourceInstances().get(instanceID).setStatus(TemplateInstanceStatus.DELETING);
            }
            resourceDao.save(resourceGroup);
        } else {
            throw new IllegalArgumentException("ResourceGroup group: " + resourceGroupName + " does not exist");
        }
    }

    @RequestMapping(value = "/{resource-group}/{instance-id}", method = RequestMethod.PUT)
    public void updateInstanceStatus(@PathVariable(name = "resource-group") String resourceGroupName,
                                     @PathVariable(name = "instance-id") String instanceId,
                                     @RequestParam(name = "status") TemplateInstanceStatus status) {
        if (resourceDao.existsById(resourceGroupName)) {
            ResourceGroup resourceGroup = resourceDao.findById(resourceGroupName).get();
            if (resourceGroup.getResourceInstances().containsKey(instanceId)) {
                LOGGER.info("Updating status of resourceGroup: " + resourceGroupName + ", instance: " + instanceId + ", to status: "
                        + status.name());
                resourceGroup.getResourceInstances().get(instanceId).setStatus(status);
                resourceDao.save(resourceGroup);
            } else {
                throw new IllegalArgumentException(
                        "Resource instance: " + instanceId + " does not exist for resourceGroup: " + resourceGroupName);
            }
        } else {
            throw new IllegalArgumentException("ResourceGroup: " + resourceGroupName + " does not exist");
        }
    }
}
