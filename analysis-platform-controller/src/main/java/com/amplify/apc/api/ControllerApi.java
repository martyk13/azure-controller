package com.amplify.apc.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.DeploymentMode;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.rest.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.json.Json;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@RestController
public class ControllerApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerApi.class);

    @RequestMapping(value = "/deployARMTemplate", method = RequestMethod.POST, consumes = {"multipart/form-data"})
    public @ResponseBody
    String deployARMTemplate(@RequestPart("resource-group") @NotBlank String resourceGroup,
                             @RequestPart("instance-id") @NotBlank String instanceId,
                             @RequestPart("template") @Valid MultipartFile template) {

        LOGGER.info("Deploy ARM Template: [" + instanceId + "] to group: " + resourceGroup);

        String fileName = null;

        if (!template.isEmpty()) {
            try {
                // Upload the Template file to a temporary file.
                fileName = template.getOriginalFilename();
                byte[] bytes = template.getBytes();
                File templateFile = Files.createTempFile("armTemplate-" + fileName, null).toFile();
                BufferedOutputStream buffStream = new BufferedOutputStream(new FileOutputStream(templateFile));
                buffStream.write(bytes);
                buffStream.close();

                LOGGER.info("Successfully uploaded template [" + fileName + "] to " + templateFile.getPath());

                // Authenticate to Azure
                LOGGER.info("Authenticating to AZURE");
                final File credFile = new File(System.getenv("AZURE_AUTH_LOCATION"));
                Azure azure = Azure.configure().withLogLevel(LogLevel.NONE).authenticate(credFile)
                        .withDefaultSubscription();

                // Now run the supplied template
                if (runTemplate(azure, getTemplate(templateFile), resourceGroup, instanceId)) {
                    return "You have successfully uploaded template [" + fileName + "] to " + templateFile.getPath();
                } else {
                    return "You failed to upload template [" + fileName + "] - check logs";
                }

            } catch (Exception e) {
                return "You failed to upload template [" + fileName + "] : " + e.getMessage();
            }
        } else {
            return "Unable to upload template [" + fileName + "] - File is empty.";
        }
    }

    /**
     * Function which runs the actual template against Azure.
     *
     * @param azure The instance of the {@link Azure} client
     * @return true if template runs successfully
     */
    public boolean runTemplate(Azure azure, String templateJson, String resourceGroup, String instanceId) {

        LOGGER.info("Running ARM template against AZURE");

        try {

            // =============================================================
            // Create resource group.
            // TODO : Specify as parameter
            LOGGER.info("Creating a resource group with name: " + resourceGroup);
            ResourceGroup rg = azure.resourceGroups().define(resourceGroup).withRegion(Region.US_EAST).create();
            LOGGER.info("Created a resource group with name: " + resourceGroup);
            Utils.print(rg);

            // =============================================================
            // Create a deployment for an Azure App Service via an ARM template
            LOGGER.info("Starting a deployment for an Azure App Service: " + instanceId);

            // The parameters have been inserted into the template, hence passed as empty
            Deployment dp = azure.deployments().define(instanceId).withExistingResourceGroup(rg).withTemplate(templateJson)
                    .withParameters(getProperties(instanceId)).withMode(DeploymentMode.INCREMENTAL).create();
            LOGGER.info("Finished a deployment for an Azure App Service: " + instanceId);

            return true;
        } catch (Exception f) {
            LOGGER.error(f.getMessage());
            f.printStackTrace();

        } finally {

            // Clean down the created Resource Group - for testing could specify as an API
            // param (so can check Azure)
            /*try {
                LOGGER.info("Deleting Resource Group: " + resourceGroup);
                azure.resourceGroups().beginDeleteByName(resourceGroup);
                LOGGER.info("Deleted Resource Group: " + resourceGroup);
            } catch (NullPointerException npe) {
                LOGGER.info("Did not create any resources in Azure. No clean up is necessary");
            } catch (Exception g) {
                g.printStackTrace();
            }*/

        }
        return false;
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

        // TODO : Specify params via file passed with template
        //final String hostingPlanName = SdkContext.randomResourceName("hpAmpAP", 24);
        //final String webappName = SdkContext.randomResourceName("wnAmpAP", 24);

        // Read the Template file in as JSON
        final InputStream template;
        template = new FileInputStream(templateFile.getAbsolutePath());

        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode templateJson = mapper.readTree(template);

        // Add parameters in to the template JSON, these are specified as their
        // 'default' values...
        //validateAndAddFieldValue("string", hostingPlanName, "hostingPlanName", null, templateJson);
        //validateAndAddFieldValue("string", webappName, "webSiteName", null, templateJson);
        //validateAndAddFieldValue("string", "F1", "skuName", null, templateJson);
        //validateAndAddFieldValue("int", "1", "skuCapacity", null, templateJson);

        return templateJson.toString();
    }

    // Helper to insert a parameter object into the 'parameters' on the supplied
    // template.
    private void validateAndAddFieldValue(String type, String fieldValue, String fieldName, String errorMessage,
                                          JsonNode templateJson) throws IllegalAccessException {

        // Add count variable for loop....
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode parameter = mapper.createObjectNode();
        parameter.put("type", type);
        if (type == "int") {
            parameter.put("defaultValue", Integer.parseInt(fieldValue));
        } else {
            parameter.put("defaultValue", fieldValue);
        }
        ObjectNode.class.cast(templateJson.get("parameters")).replace(fieldName, parameter);
    }

}
