package com.kenesys.analysisplatform.services.templates;

import com.kenesys.analysisplatform.dao.TemplateDao;
import com.kenesys.analysisplatform.domain.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class TemplateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateService.class);

    @Autowired
    private TemplateDao templateDao;

    public File getTemplateFile(Template template) throws IOException {
        return null;
    }

    public void updateTemplate(Template template) {
        if(template != null) {
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
