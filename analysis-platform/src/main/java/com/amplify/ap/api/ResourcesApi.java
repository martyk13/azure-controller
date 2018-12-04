package com.amplify.ap.api;

import com.amplify.ap.dao.ResourceDao;
import com.amplify.ap.dao.TemplateDao;
import com.amplify.ap.domain.Resource;
import com.amplify.ap.domain.TemplateInstance;
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

    @RequestMapping(method= RequestMethod.GET)
    public List<Resource> getAllResources() {
        return resourceDao.findAll();
    }

    @RequestMapping(method= RequestMethod.POST)
    public void createResources(@RequestParam(name = "resource-group") String resourceGroup, @RequestParam(name = "template-id") String templateId) {
        if(templateDao.existsById(templateId)) {
            TemplateInstance newTemplateInstance = new TemplateInstance(templateId, UUID.randomUUID().toString());
            if(!resourceDao.existsById(resourceGroup)) {
                Resource newResource = new Resource(resourceGroup, Arrays.asList(newTemplateInstance));
                resourceDao.save(newResource);
            }
        } else {
            throw new IllegalArgumentException("Template ID: " + templateId + " does not exist");
        }
    }
}
