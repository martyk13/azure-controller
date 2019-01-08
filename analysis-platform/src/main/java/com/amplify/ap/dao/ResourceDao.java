package com.amplify.ap.dao;

import com.amplify.ap.domain.ResourceGroup;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ResourceDao extends MongoRepository<ResourceGroup, String> {
}
