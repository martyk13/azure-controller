package com.amplify.apc.api;

import com.amplify.apc.services.azure.AzureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;

@RestController
public class ControllerApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerApi.class);

    @Autowired
    private AzureService azureService;

    @RequestMapping(value = "/deployARMTemplate", method = RequestMethod.POST, consumes = {"multipart/form-data"})
    public ResponseEntity<String> deployARMTemplate(@RequestParam("resource-group") @NotBlank String resourceGroupName,
                                                    @RequestParam("instance-id") @NotBlank String instanceId,
                                                    @RequestParam("response-url") @NotBlank String responseUrl,
                                                    @RequestParam("template") @Valid MultipartFile template) {

        LOGGER.info("Deploy ARM Template: [" + instanceId + "] to group: " + resourceGroupName);
        LOGGER.info("Response URL: {}", responseUrl);

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

                azureService.createResourceFromArmTemplate(templateFile, resourceGroupName, instanceId, responseUrl);

                return new ResponseEntity<>("You have successfully uploaded template [" + fileName + "] to " + templateFile.getPath() + " now processing...", HttpStatus.OK);

            } catch (Exception e) {
                return new ResponseEntity<>("You failed to upload template [" + fileName + "] : " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Unable to upload template [" + fileName + "] - File is empty.", HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/deployARMTemplate/{resource-group}", method = RequestMethod.DELETE)
    public void deleteResourceGroup(@PathVariable(name = "resource-group") String resourceGroupName, @RequestBody List<String> instanceIds) {
        LOGGER.info("Request received to delete resource group {}", resourceGroupName);

    }

}
