package com.amplify.apc.services.azure;

import java.io.File;
import java.util.List;

public interface AzureService {

    public void createResourceFromArmTemplate(File template, String resourceGroupName, String instanceId, String responseUrl);

    public void deleteResourceGroup(String resourceGroupName, List<String> instanceIds, String responseUrl); 

}
