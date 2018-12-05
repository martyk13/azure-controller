package com.amplify.ap.api;

import com.amplify.ap.dao.ResourceDao;
import com.amplify.ap.dao.TemplateDao;
import com.amplify.ap.domain.Resource;
import com.amplify.ap.domain.TemplateInstance;
import com.amplify.ap.services.resources.ResourceService;
import com.amplify.ap.services.templates.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
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
    public void createResources(@RequestParam(name = "resource-group") String resourceGroup, @RequestParam(name = "template-id") String templateId) {
        if (templateDao.existsById(templateId)) {
            TemplateInstance newTemplateInstance = new TemplateInstance(templateId, UUID.randomUUID().toString());
            if (!resourceDao.existsById(resourceGroup)) {
                Resource resource = new Resource(resourceGroup, Arrays.asList(newTemplateInstance));
                resourceDao.save(resource);
            } else {
                Resource resource = resourceDao.findById(resourceGroup).get();
                resource.addTemplateInstance(newTemplateInstance);
                resourceDao.save(resource);
            }
            resourceService.requestResource(resourceGroup, newTemplateInstance.getInstanceId(), templateService.getTemplateFile(templateId));
        } else {
            throw new IllegalArgumentException("Template ID: " + templateId + " does not exist");
        }
    }
}
