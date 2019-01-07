package com.amplify.apc.services.azure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amplify.apc.domain.ResourceType;
import com.amplify.apc.domain.StorageAccountType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public abstract class AbstractAzureService implements AzureService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AzureService.class);

	@Value("${azure.login.clientid}")
	protected String azureClientId;

	@Value("${azure.login.domain}")
	protected String azureDomain;

	@Value("${azure.login.secret}")
	protected String azureSecret;

	@Value("${azure.props.adminuser}")
	protected String adminUsername;

	@Value("${azure.props.adminpassword}")
	protected String adminPassword;
	
	@Value("${azure.resource.region.default}")
	protected String defaultRegion;

	@Autowired
	protected ResponseService responseService;

	/**
	 * Creates and returns a randomized name based on the prefix and a UUID (minus the hyphens)
	 *
	 * @param namePrefix The prefix string to be used in generating the name.
	 * @param maxLength The maximum length of the name (no limit if <= 0)
	 * @return The randomized name
	 */
	public static String createRandomName(String namePrefix, int maxLength) {

		String name = namePrefix + UUID.randomUUID().toString().replace("-", "");
		return (maxLength > 0 ? name.substring(0, maxLength) : name);
	}

	public static String createRandomName(String namePrefix) {

		// No length limitation
		return (createRandomName(namePrefix, -1));
	}

	
	protected String getProperties(ResourceType type, String instanceId) {

		JsonObject props = null;

		switch (type) {
		case COMPUTE:
			props = getComputeProperties(instanceId);
			break;
		case STORAGE:
			props = getStorageProperties(instanceId);
			break;

		}
		LOGGER.info("Using Params: {}", props);
		return props.toString();
	}

	// Get the properties applicable to COMPUTE resource
	private JsonObject getComputeProperties(String instanceId) {
		return Json.createObjectBuilder().add("adminUsername", Json.createObjectBuilder().add("value", adminUsername))
				.add("adminPassword", Json.createObjectBuilder().add("value", adminPassword))
				.add("vmName", Json.createObjectBuilder().add("value", instanceId)).build();
	}

	// Get the properties applicable to STORAGE resource
	private JsonObject getStorageProperties(String instanceId) {
		// The 'location' parameter gets defaulted to the location of the Resource Group
		// We're just defaulting the storage type to STANDARD_LRS for now
		return Json.createObjectBuilder().add("storageAccountType",
				Json.createObjectBuilder().add("value", StorageAccountType.STANDARD_LRS.getValue())).build();
	}

	protected JsonNode getTemplate(File templateFile)
			throws IllegalAccessException, JsonProcessingException, IOException {

		LOGGER.info("Converting template to JSON and inserting parameters");

		// Read the Template file in as JSON
		final InputStream template;
		template = new FileInputStream(templateFile.getAbsolutePath());

		final ObjectMapper mapper = new ObjectMapper();
		final JsonNode templateJson = mapper.readTree(template);

		return templateJson;
	}

}
