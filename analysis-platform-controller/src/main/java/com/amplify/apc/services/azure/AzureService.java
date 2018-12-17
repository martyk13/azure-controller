package com.amplify.apc.services.azure;

import java.io.File;
import java.util.List;

import org.springframework.scheduling.annotation.Async;

public interface AzureService {

	@Async
    public void createResourceFromArmTemplate(File template, String resourceGroupName, String instanceId, String responseUrl);

	@Async
    public void deleteResourceGroup(String resourceGroupName, List<String> instanceIds, String responseUrl); 
}
