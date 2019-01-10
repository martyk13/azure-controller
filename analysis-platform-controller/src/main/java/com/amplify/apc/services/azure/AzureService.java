package com.amplify.apc.services.azure;

import java.io.File;
import java.util.List;

import org.springframework.scheduling.annotation.Async;

import com.amplify.apc.domain.ResourceType;

public interface AzureService {

	@Async
	public void createResourceFromArmTemplate(File template, ResourceType resourceType, String resourceGroupName,
			String instanceId, String responseUrl);

	@Async
	public void deleteResourceGroup(String resourceGroupName, List<String> instanceIds, String responseUrl);

	@Async
	public void createStorageAccount(String resourceGroupName, String storageAccountName);

	@Async
	public void deleteStorageAccount(String resourceGroupName, String storageAccountName);

	@Async
	void deleteStorageAccountById(String resourceGroupName, String storageAccountId);
	
	@Async
	public void createStorageContainer(String resourceGroupName, String storageAccountName, String containerName);

	@Async
	public void deleteStorageContainer(String resourceGroupName, String storageAccountName, String containerName);


}
