package com.amplify.apc.services.azure;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.amplify.apc.domain.ResourceType;
import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.DeploymentMode;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.BlobRequestOptions;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.rest.LogLevel;

public class AzureServiceCloud extends AbstractAzureService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AzureServiceCloud.class);

	@Autowired
	private ResponseService responseService;

	private Azure azureService;

	// CLOUD implementation of Azure Login
	private synchronized Azure azureLogin() throws IOException {

		if (azureService == null) {
			LOGGER.info("Authenticating to AZURE");
			AzureEnvironment environment = AzureEnvironment.AZURE;
			AzureTokenCredentials credentials = new ApplicationTokenCredentials(azureClientId, azureDomain, azureSecret,
					environment);
			azureService = Azure.configure().withLogLevel(LogLevel.NONE).authenticate(credentials)
					.withDefaultSubscription();
		}
		return azureService;
	}

	private ResourceGroup getResourceGroup(String resourceGroupName) throws IOException {

		azureLogin();
		ResourceGroup resourceGroup;

		if (azureService.resourceGroups().contain(resourceGroupName)) {
			LOGGER.info("Getting an existing resource group with name: " + resourceGroupName);
			resourceGroup = azureService.resourceGroups().getByName(resourceGroupName);
		} else {
			LOGGER.info("Creating a new resource group with name: " + resourceGroupName);
			// TODO : Resource Group should be created in appropriate Region
			resourceGroup = azureService.resourceGroups().define(resourceGroupName).withRegion(defaultRegion).create();
		}
		return resourceGroup;
	}

	@Override
	public void createResourceFromArmTemplate(File template, ResourceType resourceType, String resourceGroupName,
			String instanceId, String responseUrl) {
		try {
			JsonNode templateJson = getTemplate(template);
			azureLogin();

			LOGGER.info("Starting a deployment for an Azure App Service: " + instanceId);
			azureService.deployments().define(instanceId).withExistingResourceGroup(getResourceGroup(resourceGroupName))
					.withTemplate(templateJson.toString()).withParameters(getProperties(resourceType, instanceId))
					.withMode(DeploymentMode.INCREMENTAL).create();
			LOGGER.info("Finished a deployment for an Azure App Service: " + instanceId);

			responseService.updateStatus(responseUrl, resourceGroupName, instanceId, "READY");
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			responseService.updateStatus(responseUrl, resourceGroupName, instanceId, "FAILED");
		}
	}

	@Override
	public void deleteResourceGroup(String resourceGroupName, List<String> instanceIds, String responseUrl) {
		try {
			azureLogin();
			LOGGER.info("Deleting resource group: {}", resourceGroupName);
			azureService.resourceGroups().deleteByName(resourceGroupName);
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

	@Override
	public void createStorageAccount(String resourceGroupName, String storageAccountName) {

		try {
			// TODO : How should the response service work for this type of request ?

			// If the Resource Group already exists then use it's Region for the Storage
			// Account, otherwise default it.
			ResourceGroup group = getResourceGroup(resourceGroupName);

			StorageAccount storageAccount = azureService.storageAccounts().define(storageAccountName)
					.withRegion(group.region()).withExistingResourceGroup(group).create();

			if (storageAccount != null) {
				LOGGER.info("Request for creation of Storage Account: [" + storageAccountName + "] in Resource Group ["
						+ resourceGroupName + "] SUCCESS");
			} else {
				LOGGER.info("Request for creation of Storage Account: [" + storageAccountName + "] in Resource Group ["
						+ resourceGroupName + "] FAILED - already exists");
			}

		} catch (

		Exception e) {
			LOGGER.error(e.getMessage());
		}
	}

	@Override
	public void deleteStorageAccount(String resourceGroupName, String storageAccountName) {
		try {
			// TODO : How should the response service work for this type of request ?

			StorageAccount storageAccount = azureService.storageAccounts().getByResourceGroup(resourceGroupName,
					storageAccountName);

			deleteStorageAccountById(resourceGroupName, storageAccount.id());

		} catch (

		Exception e) {
			LOGGER.error(e.getMessage());
		}
	}

	@Override
	public void deleteStorageAccountById(String resourceGroupName, String storageAccountId) {
		try {
			// TODO : How should the response service work for this type of request ?

			StorageAccount storageAccount = azureService.storageAccounts().getById(storageAccountId);

			// TODO : Protection against mistaken deletion ? Use of Access Policies, Soft
			// Delete, ... ?
			if (storageAccountId != null) {
				azureService.storageAccounts().deleteById(storageAccount.id());
				LOGGER.info("Request for deletion of Storage Account with ID : [" + storageAccountId
						+ "] in Resource Group [" + resourceGroupName + "] SUCCESS");
			} else {
				LOGGER.info("Request for deletion of Storage Account with ID : [" + storageAccountId
						+ "] in Resource Group [" + resourceGroupName + "] FAILED as it does not exist");
			}

		} catch (

		Exception e) {
			LOGGER.error(e.getMessage());
		}
	}

	@Override
	public void createStorageContainer(String resourceGroupName, String storageAccountName, String containerName) {
		try {
			// TODO : How should the response service work for this type of request ?

			// Get hold of a reference to the Container (NOTE this doesn't create the
			// container - it may or may not exist)
			CloudBlobContainer container = getContainerReference(resourceGroupName, storageAccountName, containerName);

			if (container != null) {
				// Create the container if it does not exist with NO public access.
				if (!container.exists()) {
					container.create(BlobContainerPublicAccessType.OFF, new BlobRequestOptions(),
							new OperationContext());
					LOGGER.info("Request for creation of Storage Container: [" + containerName
							+ "] in Storage Account [" + storageAccountName + "] in Resource Group ["
							+ resourceGroupName + "] SUCCESS");
				} else {
					LOGGER.info("Request for creation of Storage Container: [" + containerName
							+ "] in Storage Account [" + storageAccountName + "] in Resource Group ["
							+ resourceGroupName + "] FAILED - already exists");
				}
			}

		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
	}

	@Override
	public void deleteStorageContainer(String resourceGroupName, String storageAccountName, String containerName) {
		try {
			// TODO : How should the response service work for this type of request ?
			// Get hold of a reference to the Container (NOTE this doesn't create the
			// container - it may or may not exist)
			CloudBlobContainer container = getContainerReference(resourceGroupName, storageAccountName, containerName);

			if (container != null) {
				// TODO : Protection against mistaken deletion ? Use of Access Policies, Soft
				// Delete, ... ?
				// Delete the container if it exists
				if (container.exists()) {
					container.delete();
				} else {
					LOGGER.info("Request for deletion of Storage Container: [" + containerName
							+ "] in Storage Account [" + storageAccountName + "] in Resource Group ["
							+ resourceGroupName + "] FAILED as it doesn not exist");
				}
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
	}

	/**
	 * Helper for getting hold of a Reference to a Container.
	 * 
	 * @param resourceGroupName The Resource Group Name
	 * @param accountName       The Storage Account Name
	 * @param containerName     The Storage Container name
	 * @return The {@link CloudBlobContainer} to use for the Container.
	 */
	private CloudBlobContainer getContainerReference(String resourceGroupName, String accountName,
			String containerName) {

		try {
			azureLogin();
			StorageAccount storageAccount = azureService.storageAccounts().getByResourceGroup(resourceGroupName,
					accountName);
			CloudBlobClient blobClient = BlobClientProvider.getBlobClientReference(storageAccount);
			return blobClient.getContainerReference(containerName);

		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			return null;
		}
	}
}
