package com.kenesys.analysisplatform.api;

import com.kenesys.analysisplatform.dao.TemplateDao;
import com.kenesys.analysisplatform.domain.Template;
import org.springframework.beans.factory.annotation.Autowired;
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

    @RequestMapping(method= RequestMethod.GET)
    public List<Template> getAllTemplates() {
        return templateDao.findAll();
    }

    @RequestMapping(method = RequestMethod.POST)
    public void addTemplate(@RequestBody Template template) {
        templateDao.save(template);
    }
}

