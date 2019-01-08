package com.amplify.ap.services.resources;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.amplify.ap.domain.Resource;
import com.amplify.ap.domain.ResourceType;

@Service
public class ResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${resources.clients.requesturl}")
    private String resourceRequestUrl;

    public HttpStatus requestResource(String resourceGroup, String instanceId, File template, String resourceType) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Requesting new resource from ");
        sb.append(resourceRequestUrl);
        sb.append(", Details: {resourceGroup: ");
        sb.append(resourceGroup);
        sb.append(", instanceId: ");
        sb.append(instanceId);
        sb.append(", templateFile: ");
        sb.append(template.getAbsolutePath());
        sb.append(", resourceType: ");
        sb.append(resourceType);
        sb.append("}");
        LOGGER.info(sb.toString());

        // Set request header
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Cretae file and content disposition
        LinkedMultiValueMap<String, String> pdfHeaderMap = new LinkedMultiValueMap<>();
        pdfHeaderMap.add("Content-disposition", "form-data; name=template; filename=" + template.getName());
        pdfHeaderMap.add("Content-type", "application/octet-stream");
        HttpEntity<byte[]> templateEntity = new HttpEntity<byte[]>(Files.readAllBytes(template.toPath()), pdfHeaderMap);

        MultiValueMap<String, Object> body
                = new LinkedMultiValueMap<>();
        body.add("resource-group", resourceGroup);
        body.add("instance-id", instanceId);
        body.add("response-url", getResponseUrl());
        body.add("template", templateEntity);
        body.add("resource-type", resourceType);

        HttpEntity<MultiValueMap<String, Object>> requestEntity
                = new HttpEntity<>(body, headers);

        ResponseEntity response = restTemplate.postForEntity(resourceRequestUrl, requestEntity, String.class);
        return response.getStatusCode();
    }

    public HttpStatus deleteResource(Resource resource) {
        LOGGER.info("Sending request to delete Resource Group: {}", resource.getResourceGroup());

        String requestUrl = resourceRequestUrl + "/" + resource.getResourceGroup() + "?response-url=" + getResponseUrl();

        HttpEntity<Set<String>> request = new HttpEntity<Set<String>>(resource.getTemplateInstances().keySet());
        ResponseEntity response = restTemplate.exchange(requestUrl, HttpMethod.DELETE, request, ResponseEntity.class);
        return response.getStatusCode();
    }

    private String getResponseUrl() {
        // Get the request URL to respond to once processing has finished
        return ServletUriComponentsBuilder.fromCurrentRequestUri().build().getHost();
    }
}
