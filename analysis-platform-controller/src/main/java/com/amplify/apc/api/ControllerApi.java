package com.amplify.apc.api;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.amplify.apc.domain.ResourceType;
import com.amplify.apc.domain.ResourceTypeConverter;
import com.amplify.apc.services.azure.AbstractAzureService;
import com.amplify.apc.services.azure.AzureService;

@RestController
public class ControllerApi {

	private static final Logger LOGGER = LoggerFactory.getLogger(ControllerApi.class);

	@Autowired
	private AzureService azureService;

	@InitBinder
	public void initBinder(final WebDataBinder webdataBinder) {
		webdataBinder.registerCustomEditor(ResourceType.class, new ResourceTypeConverter());
	}

	@PostMapping(value = "/deployARMTemplate", consumes = { "multipart/form-data" })
	public ResponseEntity<String> deployARMTemplate(@RequestParam("resource-group") @NotBlank String resourceGroupName,
			@RequestParam("instance-id") @NotBlank String instanceId,
			@RequestParam("response-url") @NotBlank String responseUrl,
			@RequestParam("template") @Valid MultipartFile template,
			@RequestParam("resource-type") @NotBlank ResourceType resourceType) {

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

				azureService.createResourceFromArmTemplate(templateFile, resourceType, resourceGroupName, instanceId,
						responseUrl);

				return new ResponseEntity<>("You have successfully uploaded template [" + fileName + "] to "
						+ templateFile.getPath() + " now processing...", HttpStatus.OK);

			} catch (Exception e) {
				return new ResponseEntity<>("You failed to upload template [" + fileName + "] : " + e.getMessage(),
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} else {
			return new ResponseEntity<>("Unable to upload template [" + fileName + "] - File is empty.",
					HttpStatus.BAD_REQUEST);
		}
	}

	@DeleteMapping(value = "/deleteResourceGroup/{resource-group}")
	public void deleteResourceGroup(@PathVariable(name = "resource-group") String resourceGroupName,
			@RequestParam(name = "response-url") String responseUrl, @RequestBody List<String> instanceIds) {

		LOGGER.info("Request received to delete resource group {}", resourceGroupName);
		azureService.deleteResourceGroup(resourceGroupName, instanceIds, responseUrl);
	}

	@PostMapping(value = "/createStorageAccount")
	public ResponseEntity<String> createStorageAccount(
			@RequestParam("resource-group") @NotBlank String resourceGroupName,
			@RequestParam("instance-id") @NotBlank String instanceId,
			@RequestParam("response-url") @NotBlank String responseUrl
	// TODO : Support multiple tags passed in Map<String,String>
	) {

		LOGGER.info("Request received to Create Storage Account in Resource Group [" + resourceGroupName + "]");

		// Generate Random name for the Storage Account
		String storageAccountName = AbstractAzureService.createRandomName("stacc", 23);

		try {
			azureService.createStorageAccount(resourceGroupName, storageAccountName, instanceId, responseUrl);

			return new ResponseEntity<>("You have successfully submitted a request to create Storage Account ["
					+ storageAccountName + "] in Resource Group [" + resourceGroupName + "]", HttpStatus.OK);

		} catch (Exception e) {
			return new ResponseEntity<>("Unable to create Storage Account [" + storageAccountName
					+ "] in Resource Group [" + resourceGroupName + "] - " + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@DeleteMapping(value = "/deleteStorageAccount/{account-name}")
	public ResponseEntity<String> deleteStorageAccount(
			@RequestParam("resource-group") @NotBlank String resourceGroupName,
			@RequestParam("account-name") @NotBlank String storageAccountName,
			@RequestParam("instance-id") @NotBlank String instanceId,
			@RequestParam("response-url") @NotBlank String responseUrl) {

		LOGGER.info("Request received to Delete Storage Account: [" + storageAccountName + "] in Resource Group ["
				+ resourceGroupName + "]");

		try {
			// TODO : Add protection ?  Don't delete active Accounts ?
			azureService.deleteStorageAccount(resourceGroupName, storageAccountName, instanceId, responseUrl);

			return new ResponseEntity<>("You have successfully submitted a request to delete Storage Account ["
					+ storageAccountName + "] in Resource Group [" + resourceGroupName + "]", HttpStatus.OK);

		} catch (Exception e) {
			return new ResponseEntity<>("Unable to delete Storage Account [" + storageAccountName
					+ "] in Resource Group [" + resourceGroupName + "] - " + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@DeleteMapping(value = "/deleteStorageAccountById/{account-id}")
	public ResponseEntity<String> deleteStorageAccountById(
			@RequestParam("resource-group") @NotBlank String resourceGroupName,
			@RequestParam("account-id") @NotBlank String storageAccountId,
			@RequestParam("instance-id") @NotBlank String instanceId,
			@RequestParam("response-url") @NotBlank String responseUrl) {

		LOGGER.info("Request received to Delete Storage Account: [" + storageAccountId + "] in Resource Group ["
				+ resourceGroupName + "]");

		try {
			// TODO : Add protection ?  Don't delete active Accounts ?
			azureService.deleteStorageAccountById(resourceGroupName, storageAccountId, instanceId, responseUrl);

			return new ResponseEntity<>("You have successfully submitted a request to delete Storage Account ["
					+ storageAccountId + "] in Resource Group [" + resourceGroupName + "]", HttpStatus.OK);

		} catch (Exception e) {
			return new ResponseEntity<>("Unable to delete Storage Account [" + storageAccountId
					+ "] in Resource Group [" + resourceGroupName + "] - " + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/createStorageContainer")
	public ResponseEntity<String> createStorageContainer(
			@RequestParam("resource-group") @NotBlank String resourceGroupName,
			@RequestParam("instance-id") @NotBlank String instanceId,
			@RequestParam("response-url") @NotBlank String responseUrl,
			@RequestParam("account-name") @NotBlank String accountName
	// TODO : Support multiple tags passed in Map<String,String>
	) {

		LOGGER.info("Request received to Create Storage Container in Storage Account [" + accountName
				+ "] in Resource Group [" + resourceGroupName + "]");

		// Generate Random name for the Storage Container
		String containerName = AbstractAzureService.createRandomName("stcont");

		try {
			azureService.createStorageContainer(resourceGroupName, accountName, containerName, instanceId, responseUrl);

			return new ResponseEntity<>("You have successfully submitted a request to create Storage Container ["
					+ containerName + "] in Storage Account [" + accountName + "] in Resource Group ["
					+ resourceGroupName + "]", HttpStatus.OK);

		} catch (Exception e) {
			return new ResponseEntity<>(
					"Unable to create Storage Container [" + containerName + "] in Storage Account [" + accountName
							+ "] in Resource Group [" + resourceGroupName + "] - " + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@DeleteMapping(value = "/deleteStorageContainer")
	public ResponseEntity<String> deleteStorageContainer(
			@RequestParam("resource-group") @NotBlank String resourceGroupName,
			@RequestParam("instance-id") @NotBlank String instanceId,
			@RequestParam("response-url") @NotBlank String responseUrl,
			@RequestParam("account-name") @NotBlank String accountName,
			@RequestParam("container-name") @NotBlank String containerName) {

		LOGGER.info("Request received to Delete Storage Container: [" + containerName + "] in Storage Account ["
				+ accountName + "] in Resource Group [" + resourceGroupName + "]");

		try {
			azureService.deleteStorageContainer(resourceGroupName, accountName, containerName, instanceId, responseUrl);

			return new ResponseEntity<>("You have successfully submitted a request to delete Storage Container ["
					+ containerName + "] in Storage Account [" + accountName + "] in Resource Group ["
					+ resourceGroupName + "]", HttpStatus.OK);

		} catch (Exception e) {
			return new ResponseEntity<>(
					"Unable to delete Storage Container [" + containerName + "] in Storage Account [" + accountName
							+ "] in Resource Group [" + resourceGroupName + "] - " + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
