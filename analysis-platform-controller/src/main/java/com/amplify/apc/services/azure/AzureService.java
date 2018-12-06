package com.amplify.apc.services.azure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.DeploymentMode;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.rest.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.json.Json;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class AzureService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureService.class);

    @Value("${azure.login.clientid}")
    private String azureClientId;

    @Value("${azure.login.domain}")
    private String azureDomain;

    @Value("${azure.login.secret}")
    private String azureSecret;

    @Value("${azure.props.adminuser}")
    private String adminUsername;

    @Value("${azure.props.adminpassword}")
    private String adminPassword;

    @Autowired
    private ResponseService responseService;

    @Async
    public void createResourceFromArmTemplate(File template, String resourceGroupName, String instanceId, String requestOrigin) {
        try {
            String templateJson = getTemplate(template);

            Azure azure = azureLogin();
            ResourceGroup resourceGroup = getResourceGroup(azure, resourceGroupName);

            LOGGER.info("Starting a deployment for an Azure App Service: " + instanceId);
            azure.deployments().define(instanceId).withExistingResourceGroup(resourceGroup).withTemplate(templateJson)
                    .withParameters(getProperties(instanceId)).withMode(DeploymentMode.INCREMENTAL).create();
            LOGGER.info("Finished a deployment for an Azure App Service: " + instanceId);
            responseService.updateStatus(requestOrigin, resourceGroupName, instanceId, "FINISHED");
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            responseService.updateStatus(requestOrigin, resourceGroupName, instanceId, "FAILED");
        }
    }

    private Azure azureLogin() throws IOException {
        LOGGER.info("Authenticating to AZURE");
        AzureEnvironment environment = AzureEnvironment.AZURE;
        AzureTokenCredentials credentials = new ApplicationTokenCredentials(azureClientId, azureDomain, azureSecret, environment);

        return Azure.configure().withLogLevel(LogLevel.NONE).authenticate(credentials)
                .withDefaultSubscription();
    }

    private ResourceGroup getResourceGroup(Azure azure, String resourceGroupName) throws IOException {
        ResourceGroup resourceGroup;
        if (azure.resourceGroups().checkExistence(resourceGroupName)) {
            LOGGER.info("Getting an exisiting resource group with name: " + resourceGroupName);
            resourceGroup = azure.resourceGroups().getByName(resourceGroupName);
        } else {
            LOGGER.info("Creating a new resource group with name: " + resourceGroupName);
            resourceGroup = azure.resourceGroups().define(resourceGroupName).withRegion(Region.US_EAST).create();
        }
        return resourceGroup;
    }

    private String getProperties(String instanceId) {
        String json = Json.createObjectBuilder()
                .add("adminUsername", Json.createObjectBuilder().add("value", adminUsername))
                .add("adminPassword", Json.createObjectBuilder().add("value", adminPassword))
                .add("vmName", Json.createObjectBuilder().add("value", instanceId))
                .build()
                .toString();
        LOGGER.info("Using Params: {}", json);
        return json;
    }

    private String getTemplate(File templateFile)
            throws IllegalAccessException, JsonProcessingException, IOException {

        LOGGER.info("Converting template to JSON and inserting parameters");

        // Read the Template file in as JSON
        final InputStream template;
        template = new FileInputStream(templateFile.getAbsolutePath());

        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode templateJson = mapper.readTree(template);

        return templateJson.toString();
    }
}
