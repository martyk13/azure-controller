package com.kenesys.analysisplatform.api;

import com.kenesys.analysisplatform.dao.TemplateDao;
import com.kenesys.analysisplatform.domain.Template;
import com.kenesys.analysisplatform.services.templates.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/templates")
public class TemplatesApi {

    @Autowired
    private TemplateDao templateDao;

    @Autowired
    private TemplateService templateService;

    @RequestMapping(method= RequestMethod.GET)
    public List<Template> getAllTemplates() {
        return templateDao.findAll();
    }

    @RequestMapping(value="/{id}",method= RequestMethod.GET)
    public Template getTemplate(@PathVariable String id) {
        return templateDao.findById(id).get();
    }

    @RequestMapping(value="/{id}",method= RequestMethod.DELETE)
    public void deleteTemplate(@PathVariable String id) {
            templateDao.deleteById(id);
    }

    @RequestMapping(value="/{id}/description",method= RequestMethod.PUT)
    public void updateTemplateDescription(@PathVariable String id, @RequestBody Template template) {
        Template toUpdate = templateDao.findById(id).get();
        toUpdate.setDescription(template.getDescription());
        templateDao.save(toUpdate);
    }

    @RequestMapping(value="/{id}/file",method= RequestMethod.GET)
    public File getTemplateFile(@PathVariable String id) throws IOException {
        Template template = templateDao.findById(id).get();
        return templateService.getTemplateFile(template);
    }
}

