package com.amplify.apc.services.azure;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.profile_2018_03_01_hybrid.Azure;
import com.microsoft.azure.management.resources.v2018_02_01.DeploymentMode;
import com.microsoft.azure.management.resources.v2018_02_01.DeploymentProperties;
import com.microsoft.azure.management.resources.v2018_02_01.ResourceGroup;
import com.microsoft.rest.LogLevel;

public class AzureServiceStack extends AbstractAzureService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureServiceStack.class);

    // Some additional Stack-specific properties
    @Value("${azure.activedirectory.endpoint}")
    private String adEndpoint;

    @Value("${azure.login.subscriptionid}")
    private String subscriptionId;

    @Value("${azure.login.location}")
    private String location;

    @Async
    @Override
    public void createResourceFromArmTemplate(File template, String resourceGroupName, String instanceId, String responseUrl) {
        try {
            String templateJson = getTemplate(template);

            Azure azure = azureLogin();
            createResourceGroup(azure, resourceGroupName);

            DeploymentProperties deploymentProperties = new DeploymentProperties();
            deploymentProperties.withTemplate(templateJson)
                    .withParameters(getProperties(instanceId))
                    .withMode(DeploymentMode.INCREMENTAL);

            LOGGER.info("Starting a deployment for an Azure App Service: " + instanceId);
            azure.deployments()
                    .define(instanceId)
                    .withResourceGroupName(resourceGroupName)
                    .withProperties(deploymentProperties)
                    .create();

            LOGGER.info("Finished a deployment for an Azure App Service: " + instanceId);
            responseService.updateStatus(responseUrl, resourceGroupName, instanceId, "READY");
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            responseService.updateStatus(responseUrl, resourceGroupName, instanceId, "FAILED");
        }
    }

    @Async
    @Override
    public void deleteResourceGroup(String resourceGroupName, List<String> instanceIds, String responseUrl) {
        try {
            Azure azure = azureLogin();
            LOGGER.info("Deleting resource group: {}", resourceGroupName);
            azure.resourceGroups()
                    .deleteAsync(resourceGroupName)
                    .await();

            LOGGER.info("Deleting resource complete, updating status of resources");
            for (String instanceId : instanceIds) {
                responseService.updateStatus(responseUrl, resourceGroupName, instanceId, "DELETED");
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            for (String instanceId : instanceIds) {
                responseService.updateStatus(responseUrl, resourceGroupName, instanceId, "FAILED");
            }
        }
    }

    private Azure azureLogin() {
        LOGGER.info("Setting up authentication credentials");
        // Get Azure Stack cloud endpoints
        final Map<String, String> settings = getActiveDirectorySettings(adEndpoint);
        final Map<String, String> endpoints = new HashMap<>();
        endpoints.put("managementEndpointUrl", settings.get("audience"));
        endpoints.put("resourceManagerEndpointUrl", adEndpoint);
        endpoints.put("galleryEndpointUrl", settings.get("galleryEndpoint"));
        endpoints.put("activeDirectoryEndpointUrl", settings.get("login_endpoint"));
        endpoints.put("activeDirectoryResourceId", settings.get("audience"));
        endpoints.put("activeDirectoryGraphResourceId", settings.get("graphEndpoint"));
        endpoints.put("storageEndpointSuffix", adEndpoint.substring(adEndpoint.indexOf('.')));
        endpoints.put("keyVaultDnsSuffix", ".vault" + adEndpoint.substring(adEndpoint.indexOf('.')));

        AzureEnvironment stackEnvironment = new AzureEnvironment(endpoints);

        LOGGER.info("Authenticating to AZURE");
        AzureTokenCredentials credentials = new ApplicationTokenCredentials(azureClientId, azureDomain, azureSecret, stackEnvironment)
                .withDefaultSubscriptionId(subscriptionId);

        return Azure.configure()
                .withLogLevel(LogLevel.NONE)
                .authenticate(credentials, credentials.defaultSubscriptionId());
    }

    private static Map<String, String> getActiveDirectorySettings(String adEndpoint) {
        Map<String, String> adSettings = new HashMap<>();
        try {
            // create HTTP Client
            HttpClient httpClient = HttpClientBuilder.create().build();

            // Create new getRequest with below mentioned URL
            HttpGet getRequest = new HttpGet(String.format("%s/metadata/endpoints?api-version=1.0", adEndpoint));

            // Add additional header to getRequest which accepts application/xml data
            getRequest.addHeader("accept", "application/xml");

            // Execute request and catch response
            HttpResponse response = httpClient.execute(getRequest);

            // Check for HTTP response code: 200 = success
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
            }

            // Parse the json response
            String responseStr = EntityUtils.toString(response.getEntity());
            JsonReader jsonReader = Json.createReader(new StringReader(responseStr));
            JsonObject responseJson = jsonReader.readObject();

            JsonObject authentication = responseJson.getJsonObject("authentication");
            String audience = authentication.getJsonObject("audiences").toString().split("\"")[1];

            adSettings.put("galleryEndpoint", responseJson.getString("galleryEndpoint"));
            adSettings.put("login_endpoint", authentication.getString("loginEndpoint"));
            adSettings.put("audience", audience);
            adSettings.put("graphEndpoint", responseJson.getString("graphEndpoint"));

        } catch (ClientProtocolException cpe) {
            cpe.printStackTrace();
            throw new RuntimeException(cpe);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new RuntimeException(ioe);
        }
        return adSettings;
    }

    private void createResourceGroup(Azure azure, String resourceGroupName) throws IOException {
        try {
            LOGGER.info("Querying existing resource group names...");
            List<ResourceGroup> resourceGroups = azure.resourceGroups()
                    .listAsync()
                    .toList()
                    .toBlocking()
                    .last();
            if (resourceGroups == null || !resourceGroups.contains(resourceGroupName)) {
                LOGGER.info("Creating a new resource group with name: " + resourceGroupName);
                azure.resourceGroups()
                        .define(resourceGroupName)
                        .withExistingSubscription()
                        .withLocation(location)
                        .create();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }
}
