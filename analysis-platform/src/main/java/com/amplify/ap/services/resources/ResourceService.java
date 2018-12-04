package com.amplify.ap.services.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;

@Service
public class ResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${resources.clients.requesturl}")
    private String resourceRequestUrl;

    public void requestResource(String resourceGroup, String instanceId, File template) {
        StringBuilder sb = new StringBuilder();
        sb.append("Requesting new resource from ");
        sb.append(resourceRequestUrl);
        sb.append(", Details: {resourceGroup: ");
        sb.append(resourceGroup);
        sb.append(", instanceId: ");
        sb.append(instanceId);
        sb.append(", templateFile: ");
        sb.append(template.getAbsolutePath());
        sb.append("}");
        LOGGER.info(sb.toString());
    }
}
