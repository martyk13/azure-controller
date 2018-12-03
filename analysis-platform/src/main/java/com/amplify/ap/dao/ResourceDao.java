package com.amplify.ap.dao;

import com.amplify.ap.domain.Resource;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ResourceDao extends MongoRepository<Resource, String> {
}
