package com.amplify.apc.services.azure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.DeploymentMode;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.rest.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private Azure azureLogin() throws IOException {
        LOGGER.info("Authenticating to AZURE");
        final File credFile = new File(System.getenv("AZURE_AUTH_LOCATION"));
        return Azure.configure().withLogLevel(LogLevel.NONE).authenticate(credFile)
                .withDefaultSubscription();
    }

    public ResourceGroup createResourceGroup(String resourceGroupName) throws IOException {
        Azure azure = azureLogin();
        LOGGER.info("Creating a resource group with name: " + resourceGroupName);
        ResourceGroup resourceGroup = azure.resourceGroups().define(resourceGroupName).withRegion(Region.US_EAST).create();
        LOGGER.info("Created a resource group with name: " + resourceGroup);
        return resourceGroup;
    }

    public ResourceGroup getExistingResourceGroup(String resourceGroupName) throws IOException {
        Azure azure = azureLogin();
        LOGGER.info("Getting a resource group with name: " + resourceGroupName);
        ResourceGroup resourceGroup = azure.resourceGroups().getByName(resourceGroupName);
        LOGGER.info("Got a resource group with name: " + resourceGroup);
        return resourceGroup;
    }

    @Async
    public void createResourceFromArmTemplate(File template, ResourceGroup resourceGroup, String instanceId) {
        try {
            Azure azure = azureLogin();
            String templateJson = getTemplate(template);
            LOGGER.info("Starting a deployment for an Azure App Service: " + instanceId);
            azure.deployments().define(instanceId).withExistingResourceGroup(resourceGroup).withTemplate(templateJson)
                    .withParameters(getProperties(instanceId)).withMode(DeploymentMode.INCREMENTAL).create();

            LOGGER.info("Finished a deployment for an Azure App Service: " + instanceId);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    private String getProperties(String instanceId) {
        String json = Json.createObjectBuilder()
                .add("adminUsername", Json.createObjectBuilder().add("value", "azureadmin"))
                .add("adminPassword", Json.createObjectBuilder().add("value", "AzureP@55w0rd123"))
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
