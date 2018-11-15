package com.kenesys.analysisplatform.api;

import com.kenesys.analysisplatform.domain.Template;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.Arrays;
import java.util.List;

@Path("/templates")
public class TemplatesApi {

    @GET
    @Produces("application/json")
    public List<Template> getAllTemplates() {

        return Arrays.asList(new Template[]{new Template("123","template-1","template-1 description"), new Template("124","template-2","template-2 description")});
    }
}

