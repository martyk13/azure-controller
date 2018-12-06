package com.amplify.apc.services.azure;


import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class ResponseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseService.class);

    @Value("${resource.response.statuspath}")
    private String statusPath;

    @Autowired
    private RestTemplate restTemplate;

    public void updateStatus(String requestOrigin, String resourceGroup, String instanceId, String status) {
        try {
            URIBuilder builder = new URIBuilder()
                    .setScheme("http")
                    .setHost(requestOrigin)
                    .setPath("/" + resourceGroup + "/" + instanceId)
                    .addParameter("status", status);
            URI updateStatusUrl = builder.build();

            LOGGER.info("Updating status of resource instance: {}", updateStatusUrl);
            restTemplate.put(updateStatusUrl, null);
        } catch (URISyntaxException e) {
            LOGGER.error("Error updating resource status " + e.getMessage());
        }
    }
}
