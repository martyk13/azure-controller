package com.amplify.ap.controller;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.DeploymentMode;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.rest.LogLevel;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ControllerApplicationTests {
	
	final String jsonTemplate = "{\n" + 
			"    \"$schema\": \"http://schema.management.azure.com/schemas/2015-01-01/deploymentTemplate.json#\",\n" + 
			"    \"contentVersion\": \"1.0.0.0\",\n" + 
			"    \"parameters\": {\n" + 
			"    },\n" + 
			"    \"resources\": [\n" + 
			"        {\n" + 
			"            \"apiVersion\": \"2015-08-01\",\n" + 
			"            \"name\": \"[parameters('hostingPlanName')]\",\n" + 
			"            \"type\": \"Microsoft.Web/serverfarms\",\n" + 
			"            \"location\": \"[resourceGroup().location]\",\n" + 
			"            \"tags\": {\n" + 
			"                \"displayName\": \"HostingPlan\"\n" + 
			"            },\n" + 
			"            \"sku\": {\n" + 
			"                \"name\": \"[parameters('skuName')]\",\n" + 
			"                \"capacity\": \"[parameters('skuCapacity')]\"\n" + 
			"            },\n" + 
			"            \"properties\": {\n" + 
			"                \"name\": \"[parameters('hostingPlanName')]\"\n" + 
			"            }\n" + 
			"        },\n" + 
			"        {\n" + 
			"            \"apiVersion\": \"2015-08-01\",\n" + 
			"            \"name\": \"[parameters('webSiteName')]\",\n" + 
			"            \"type\": \"Microsoft.Web/sites\",\n" + 
			"            \"location\": \"[resourceGroup().location]\",\n" + 
			"            \"tags\": {\n" + 
			"                \"[concat('hidden-related:', resourceGroup().id, '/providers/Microsoft.Web/serverfarms/', parameters('hostingPlanName'))]\": \"Resource\",\n" + 
			"                \"displayName\": \"Website\"\n" + 
			"            },\n" + 
			"            \"dependsOn\": [\n" + 
			"                \"[concat('Microsoft.Web/serverfarms/', parameters('hostingPlanName'))]\"\n" + 
			"            ],\n" + 
			"            \"properties\": {\n" + 
			"                \"name\": \"[parameters('webSiteName')]\",\n" + 
			"                \"serverFarmId\": \"[resourceId('Microsoft.Web/serverfarms', parameters('hostingPlanName'))]\"\n" + 
			"            },\n" + 
			"            \"resources\": [\n" + 
			"                {\n" + 
			"                    \"apiVersion\": \"2015-08-01\",\n" + 
			"                    \"name\": \"web\",\n" + 
			"                    \"type\": \"config\",\n" + 
			"                    \"dependsOn\": [\n" + 
			"                        \"[concat('Microsoft.Web/sites/', parameters('webSiteName'))]\"\n" + 
			"                    ],\n" + 
			"                    \"properties\": {\n" + 
			"                        \"javaVersion\": \"1.8\",\n" + 
			"                        \"javaContainer\": \"TOMCAT\",\n" + 
			"                        \"javaContainerVersion\": \"8.0\"\n" + 
			"                    }\n" + 
			"                }\n" + 
			"            ]\n" + 
			"        }\n" + 
			"    ]\n" + 
			"}";

	@Test
	public void contextLoads() {

		try {
			Azure azure = Azure.configure().withLogLevel(LogLevel.NONE)
					.authenticate(new File(System.getenv("AZURE_AUTH_LOCATION"))).withDefaultSubscription();
			
			azure.deployments().define("AP-TEST")
            .withNewResourceGroup("TEST-1", Region.US_EAST)
            .withTemplate(jsonTemplate)
            .withParameters("{}")
            .withMode(DeploymentMode.INCREMENTAL)
            .create();
			
		} catch (CloudException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
