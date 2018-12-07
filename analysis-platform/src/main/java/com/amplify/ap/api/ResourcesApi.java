package com.amplify.ap.api;

import com.amplify.ap.dao.ResourceDao;
import com.amplify.ap.dao.TemplateDao;
import com.amplify.ap.domain.Resource;
import com.amplify.ap.domain.TemplateInstance;
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
    public List<Resource> getAllResources() {
        return resourceDao.findAll();
    }

    @RequestMapping(method = RequestMethod.POST)
    public void createResources(@RequestParam(name = "resource-group") String resourceGroup,
                                @RequestParam(name = "template-id") String templateId,
                                @RequestParam(name = "notification-email") String notificationEmail) throws IOException {
        if (templateDao.existsById(templateId)) {
            TemplateInstance newTemplateInstance = new TemplateInstance(templateId, UUID.randomUUID().toString(), notificationEmail, TemplateInstanceStatus.CREATING);
            HttpStatus status = resourceService.requestResource(resourceGroup, newTemplateInstance.getInstanceId(), templateService.getTemplateFile(templateId));

            if (HttpStatus.OK.equals(status)) {
                if (!resourceDao.existsById(resourceGroup)) {
                    Map<String, TemplateInstance> templateInstances = new HashMap<>();
                    templateInstances.put(newTemplateInstance.getInstanceId(), newTemplateInstance);
                    Resource resource = new Resource(resourceGroup, templateInstances);
                    resourceDao.save(resource);
                } else {
                    Resource resource = resourceDao.findById(resourceGroup).get();
                    resource.addTemplateInstance(newTemplateInstance);
                    resourceDao.save(resource);
                }
            } else {
                throw new RuntimeException("Request to create resource failed with error code " + status);
            }
        } else {
            throw new IllegalArgumentException("Template ID: " + templateId + " does not exist");
        }
    }

    @RequestMapping(value = "/{id}/{instance-id}", method = RequestMethod.PUT)
    public void updateInstanceStatus(@PathVariable(name = "id") String id,
                                     @PathVariable(name = "instance-id") String instanceId,
                                     @RequestParam(name = "status") TemplateInstanceStatus status) {
        if (resourceDao.existsById(id)) {
            Resource resource = resourceDao.findById(id).get();
            if (resource.getTemplateInstances().containsKey(instanceId)) {
                LOGGER.info("Updating status of resource group: " + id + ", instance: " + instanceId + ", to status: " + status.name());
                resource.getTemplateInstances().get(instanceId).setStatus(status);
                resourceDao.save(resource);
            } else {
                throw new IllegalArgumentException("Resource instance: " + instanceId + " does not exist for resource group: " + id);
            }
        } else {
            throw new IllegalArgumentException("Resource group: " + id + " does not exist");
        }
    }
}
