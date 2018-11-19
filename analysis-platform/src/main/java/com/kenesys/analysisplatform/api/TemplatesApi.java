package com.kenesys.analysisplatform.api;

import com.kenesys.analysisplatform.dao.TemplateDao;
import com.kenesys.analysisplatform.domain.Template;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.List;

@Path("/templates")
public class TemplatesApi {

    @Autowired
    private TemplateDao templateDao;

    @GET
    @Produces("application/json")
    public List<Template> getAllTemplates() {

        return templateDao.findAll();
    }

    @POST
    @Consumes("application/json")
    public void addTemplate(Template template) {

        templateDao.save(template);
    }
}

