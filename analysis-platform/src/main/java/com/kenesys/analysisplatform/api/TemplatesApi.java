package com.kenesys.analysisplatform.api;

import com.kenesys.analysisplatform.dao.TemplateDao;
import com.kenesys.analysisplatform.domain.Template;
import com.kenesys.analysisplatform.services.templates.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/templates")
public class TemplatesApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplatesApi.class);

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
    public ResponseEntity<Resource> getTemplateFile(@PathVariable String id) throws IOException {
        File templateFile = templateService.getTemplateFile(id);
        LOGGER.info("Returning file {} for template {}", templateFile.getName(), id);

        Path path = Paths.get(templateFile.getAbsolutePath());
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        HttpHeaders headers = new HttpHeaders(); headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + templateFile.getName());

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(templateFile.length())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(resource);
    }
}

