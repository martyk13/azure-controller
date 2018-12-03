package com.amplify.ap.dao;

import com.amplify.ap.domain.Template;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TemplateDao extends MongoRepository<Template, String> {

    Template findByFilePath(String filePath);
}
