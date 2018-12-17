package com.amplify.apc.services.azure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.json.Json;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

	@Autowired
	protected ResponseService responseService;

	protected String getProperties(String instanceId) {
		String json = Json.createObjectBuilder()
				.add("adminUsername", Json.createObjectBuilder().add("value", adminUsername))
				.add("adminPassword", Json.createObjectBuilder().add("value", adminPassword))
				.add("vmName", Json.createObjectBuilder().add("value", instanceId)).build().toString();
		LOGGER.info("Using Params: {}", json);
		return json;
	}

	protected String getTemplate(File templateFile)
			throws IllegalAccessException, JsonProcessingException, IOException {

		LOGGER.info("Converting template to JSON and inserting parameters");

		// Read the Template file in as JSON
		final InputStream template;
		template = new FileInputStream(templateFile.getAbsolutePath());

		final ObjectMapper mapper = new ObjectMapper();
		final JsonNode templateJson = mapper.readTree(template);

		return templateJson.toString();
	}
}
