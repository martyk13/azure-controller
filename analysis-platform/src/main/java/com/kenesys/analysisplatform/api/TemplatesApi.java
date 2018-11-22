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

    @RequestMapping(method = RequestMethod.POST)
    public void addTemplate(@RequestBody Template template) {
        templateDao.save(template);
    }

    @RequestMapping(value="/{id}",method= RequestMethod.GET)
    public Template getTemplate(@PathVariable String id) {
        return templateDao.findById(id).get();
    }

    @RequestMapping(value="/{id}",method= RequestMethod.PUT)
    public void updateTemplate(@PathVariable String id, @RequestBody Template template) {
        Template toUpdate = templateDao.findById(id).get();
        toUpdate.update(template);
        templateDao.save(toUpdate);
    }

    @RequestMapping(value="/{id}",method= RequestMethod.DELETE)
    public void deleteTemplate(@PathVariable String id) {
            templateDao.deleteById(id);
    }
}

