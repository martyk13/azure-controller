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
import com.microsoft.azure.management.resources.fluentcore.utils.SdkContext;
import com.microsoft.rest.LogLevel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    // TODO : ID Param not used + could specify resourceGroupName
    @RequestMapping(value = "/deployARMTemplate", method = RequestMethod.POST, consumes = {"multipart/form-data"})
    public @ResponseBody
    String deployARMTemplate(@RequestPart("id") @NotBlank String id,
                             @RequestPart("template") @Valid MultipartFile template) {

        System.out.println("Deploy ARM Template: [" + id + "]");
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

                System.out.println("Successfully uploaded template [" + fileName + "] to " + templateFile.getPath());

                // Authenticate to Azure
                System.out.println("Authenticating to AZURE");
                final File credFile = new File(System.getenv("AZURE_AUTH_LOCATION"));
                Azure azure = Azure.configure().withLogLevel(LogLevel.NONE).authenticate(credFile)
                        .withDefaultSubscription();

                // Now run the supplied template
                if (runTemplate(azure, getTemplate(templateFile))) {
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
    public static boolean runTemplate(Azure azure, String templateJson) {

        System.out.println("Running ARM template against AZURE");

        final String rgName = SdkContext.randomResourceName("rgAmpAP", 24);
        final String deploymentName = SdkContext.randomResourceName("dpAmpAP", 24);
        try {

            // =============================================================
            // Create resource group.
            // TODO : Specify as parameter
            System.out.println("Creating a resource group with name: " + rgName);
            ResourceGroup rg = azure.resourceGroups().define(rgName).withRegion(Region.US_WEST).create();
            System.out.println("Created a resource group with name: " + rgName);
            Utils.print(rg);

            // =============================================================
            // Create a deployment for an Azure App Service via an ARM template
            System.out.println("Starting a deployment for an Azure App Service: " + deploymentName);

            // The parameters have been inserted into the template, hence passed as empty
            Deployment dp = azure.deployments().define(deploymentName).withExistingResourceGroup(rg).withTemplate(templateJson)
                    .withParameters("{}").withMode(DeploymentMode.INCREMENTAL).create();
            System.out.println("Started a deployment for an Azure App Service: " + deploymentName);

            return true;
        } catch (Exception f) {

            System.out.println(f.getMessage());
            f.printStackTrace();

        } finally {

            // Clean down the created Resource Group - for testing could specify as an API
            // param (so can check Azure)
            try {
                System.out.println("Deleting Resource Group: " + rgName);
                azure.resourceGroups().beginDeleteByName(rgName);
                System.out.println("Deleted Resource Group: " + rgName);
            } catch (NullPointerException npe) {
                System.out.println("Did not create any resources in Azure. No clean up is necessary");
            } catch (Exception g) {
                g.printStackTrace();
            }

        }
        return false;
    }

    private static String getTemplate(File templateFile)
            throws IllegalAccessException, JsonProcessingException, IOException {

        System.out.println("Converting template to JSON and inserting parameters");

        // TODO : Specify params via file passed with template
        final String hostingPlanName = SdkContext.randomResourceName("hpAmpAP", 24);
        final String webappName = SdkContext.randomResourceName("wnAmpAP", 24);

        // Read the Template file in as JSON
        final InputStream template;
        template = new FileInputStream(templateFile.getAbsolutePath());

        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode templateJson = mapper.readTree(template);

        // Add parameters in to the template JSON, these are specified as their
        // 'default' values...
        validateAndAddFieldValue("string", hostingPlanName, "hostingPlanName", null, templateJson);
        validateAndAddFieldValue("string", webappName, "webSiteName", null, templateJson);
        validateAndAddFieldValue("string", "F1", "skuName", null, templateJson);
        validateAndAddFieldValue("int", "1", "skuCapacity", null, templateJson);

        return templateJson.toString();
    }

    // Helper to insert a parameter object into the 'parameters' on the supplied
    // template.
    private static void validateAndAddFieldValue(String type, String fieldValue, String fieldName, String errorMessage,
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
