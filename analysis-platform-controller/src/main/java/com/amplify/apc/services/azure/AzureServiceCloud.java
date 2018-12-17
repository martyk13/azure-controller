package com.amplify.apc.services.azure;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.DeploymentMode;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.rest.LogLevel;

public class AzureServiceCloud extends AbstractAzureService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AzureServiceCloud.class);

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
	@Override
	public void createResourceFromArmTemplate(File template, String resourceGroupName, String instanceId,
			String responseUrl) {
		try {
			String templateJson = getTemplate(template);

			Azure azure = azureLogin();
			ResourceGroup resourceGroup = getResourceGroup(azure, resourceGroupName);

			LOGGER.info("Starting a deployment for an Azure App Service: " + instanceId);
			azure.deployments().define(instanceId).withExistingResourceGroup(resourceGroup).withTemplate(templateJson)
					.withParameters(getProperties(instanceId)).withMode(DeploymentMode.INCREMENTAL).create();
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
			azure.resourceGroups().deleteByName(resourceGroupName);
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

	private Azure azureLogin() throws IOException {
		LOGGER.info("Authenticating to AZURE");
		AzureEnvironment environment = AzureEnvironment.AZURE;
		AzureTokenCredentials credentials = new ApplicationTokenCredentials(azureClientId, azureDomain, azureSecret,
				environment);

		return Azure.configure().withLogLevel(LogLevel.NONE).authenticate(credentials).withDefaultSubscription();
	}

	private ResourceGroup getResourceGroup(Azure azure, String resourceGroupName) throws IOException {
		ResourceGroup resourceGroup;
		if (azure.resourceGroups().contain(resourceGroupName)) {
			LOGGER.info("Getting an exisiting resource group with name: " + resourceGroupName);
			resourceGroup = azure.resourceGroups().getByName(resourceGroupName);
		} else {
			LOGGER.info("Creating a new resource group with name: " + resourceGroupName);
			resourceGroup = azure.resourceGroups().define(resourceGroupName).withRegion(Region.US_EAST).create();
		}
		return resourceGroup;
	}
}
