package com.amplify.ap.services.templates;

import com.amplify.ap.dao.TemplateDao;
import com.amplify.ap.domain.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class TemplateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateService.class);

    @Value("${templates.gitscanner.gitdir}")
    private String gitDirectory;

    @Autowired
    private TemplateDao templateDao;

    public File getTemplateFile(String id) {
        LOGGER.debug("Getting file for template {}", id);
        Template template = templateDao.findById(id).get();
        File templateFile = new File(gitDirectory + File.separator + template.getFilePath());
        return templateFile;
    }

    public void updateTemplate(Template template) {
        if (template != null) {
            Template toUpdate = templateDao.findByFilePath(template.getFilePath());
            if (toUpdate == null) {
                LOGGER.info("Saving new template {}", template);
                templateDao.save(template);
            } else if (template.getLastCommitTime() > toUpdate.getLastCommitTime()) {
                LOGGER.info("Updatingtemplate {} from {}", toUpdate, template);
                toUpdate.update(template);
                templateDao.save(toUpdate);
            }
        }
    }
}
