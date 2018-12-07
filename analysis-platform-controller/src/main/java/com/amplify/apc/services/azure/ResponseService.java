package com.amplify.apc.services.azure;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Service
public class ResponseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseService.class);

    @Autowired
    private RestTemplate restTemplate;

    public void updateStatus(String responseUrl, String resourceGroup, String instanceId, String status) {
        StringBuilder uriStringBuilder = new StringBuilder();
        uriStringBuilder.append(responseUrl);
        uriStringBuilder.append("/" + resourceGroup + "/" + instanceId);
        uriStringBuilder.append("?status=" + status);

        URI updateStatusUrl = URI.create(uriStringBuilder.toString());

        LOGGER.info("Updating status of resource instance: {}", updateStatusUrl);
        restTemplate.put(updateStatusUrl, null);

    }
}
