package com.amplify.ap.services.resources;

import com.amplify.ap.domain.Resource;
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
import org.springframework.web.util.UriComponents;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

@Service
public class ResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${resources.clients.requesturl}")
    private String resourceRequestUrl;

    public HttpStatus requestResource(String resourceGroup, String instanceId, File template) throws IOException {
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

        // Set request header
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Cretae file and content disposition
        LinkedMultiValueMap<String, String> pdfHeaderMap = new LinkedMultiValueMap<>();
        pdfHeaderMap.add("Content-disposition", "form-data; name=template; filename=" + template.getName());
        pdfHeaderMap.add("Content-type", "application/octet-stream");
        HttpEntity<byte[]> templateEntity = new HttpEntity<byte[]>(Files.readAllBytes(template.toPath()), pdfHeaderMap);

        // Get the request URL to respond to once processing has finished
        UriComponents requestUri = ServletUriComponentsBuilder.fromCurrentRequestUri().build();

        MultiValueMap<String, Object> body
                = new LinkedMultiValueMap<>();
        body.add("resource-group", resourceGroup);
        body.add("instance-id", instanceId);
        body.add("response-url", requestUri.toUriString());
        body.add("template", templateEntity);

        HttpEntity<MultiValueMap<String, Object>> requestEntity
                = new HttpEntity<>(body, headers);

        ResponseEntity response = restTemplate.postForEntity(resourceRequestUrl, requestEntity, String.class);
        return response.getStatusCode();
    }

    public HttpStatus deleteResource(Resource resource) {
        LOGGER.info("Sending request to delete Resource Group: {}", resource.getResourceGroup());

        HttpEntity<Set<String>> request = new HttpEntity<Set<String>>(resource.getTemplateInstances().keySet());
        ResponseEntity response = restTemplate.exchange(resourceRequestUrl + "/" + resource.getResourceGroup(), HttpMethod.DELETE, request, ResponseEntity.class);
        return response.getStatusCode();
    }
}
