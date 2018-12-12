package com.amplify.apc.services.azure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.arm.resources.Region;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.profile_2018_03_01_hybrid.Azure;
import com.microsoft.azure.management.resources.v2018_02_01.DeploymentMode;
import com.microsoft.azure.management.resources.v2018_02_01.DeploymentProperties;
import com.microsoft.azure.management.resources.v2018_02_01.ResourceGroup;
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
import java.util.List;

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
    public void createResourceFromArmTemplate(File template, String resourceGroupName, String instanceId, String responseUrl) {
        try {
            String templateJson = getTemplate(template);

            Azure azure = azureLogin();
            ResourceGroup resourceGroup = getResourceGroup(azure, resourceGroupName);

            LOGGER.info("Starting a deployment for an Azure App Service: " + instanceId);
            DeploymentProperties dp = new DeploymentProperties();
            dp.withTemplate(templateJson).withParameters(getProperties(instanceId)).withMode(DeploymentMode.INCREMENTAL);

            azure.deployments().define(instanceId).withResourceGroupName(resourceGroupName).withProperties(dp);
            //withTemplate(templateJson)
            //.withParameters(getProperties(instanceId)).withMode(DeploymentMode.INCREMENTAL).create();
            LOGGER.info("Finished a deployment for an Azure App Service: " + instanceId);
            responseService.updateStatus(responseUrl, resourceGroupName, instanceId, "READY");
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            responseService.updateStatus(responseUrl, resourceGroupName, instanceId, "FAILED");
        }
    }

    @Async
    public void deleteResourceGroup(String resourceGroupName, List<String> instanceIds, String responseUrl) {
        try {
            Azure azure = azureLogin();
            LOGGER.info("Deleting resource group: {}", resourceGroupName);
            azure.resourceGroups().deleteAsync(resourceGroupName).doOnCompleted(() -> {
                for (String instanceId : instanceIds) {
                    responseService.updateStatus(responseUrl, resourceGroupName, instanceId, "DELETED");
                }
            }).doOnError((throwable) -> {
                LOGGER.error(throwable.getMessage());
                for (String instanceId : instanceIds) {
                    responseService.updateStatus(responseUrl, resourceGroupName, instanceId, "FAILED");
                }
            });
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            for (String instanceId : instanceIds) {
                responseService.updateStatus(responseUrl, resourceGroupName, instanceId, "FAILED");
            }
        }
    }

    private Azure azureLogin() {
        LOGGER.info("Authenticating to AZURE");
        AzureEnvironment environment = AzureEnvironment.AZURE;
        AzureTokenCredentials credentials = new ApplicationTokenCredentials(azureClientId, azureDomain, azureSecret, environment);

        return Azure.configure().withLogLevel(LogLevel.NONE).authenticate(credentials)
                .withDefaultSubscription();
    }

    private ResourceGroup getResourceGroup(Azure azure, String resourceGroupName) throws IOException {
        //ResourceGroup resourceGroup;
        azure.resourceGroups().getAsync(resourceGroupName).doOnCompleted((resourceGroup) -> {
            //checkExistence(resourceGroupName)) {
            //LOGGER.info("Getting an exisiting resource group with name: " + resourceGroupName);
            return resourceGroup;
        }).doOnError((throwable) -> {
            LOGGER.info("Creating a new resource group with name: " + resourceGroupName);
            return azure.resourceGroups().define(resourceGroupName).withExistingSubscription().withLocation(Region.US_EAST.name()).create();
        });
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
