package com.kenesys.analysisplatform.dao;

import com.kenesys.analysisplatform.domain.Template;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TemplateDao extends MongoRepository<Template, String> {

    Template findByFilePath(String filePath);
}
