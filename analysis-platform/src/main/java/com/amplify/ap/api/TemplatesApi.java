package com.amplify.ap.api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.validation.constraints.NotBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amplify.ap.dao.TemplateDao;
import com.amplify.ap.domain.ResourceType;
import com.amplify.ap.domain.ResourceTypeConverter;
import com.amplify.ap.domain.Template;
import com.amplify.ap.services.templates.TemplateService;

@RestController
@RequestMapping("/templates")
public class TemplatesApi {

	private static final Logger LOGGER = LoggerFactory.getLogger(TemplatesApi.class);

	@Autowired
	private TemplateDao templateDao;

	@Autowired
	private TemplateService templateService;

	@RequestMapping(method = RequestMethod.GET)
	public List<Template> getAllTemplates() {
		return templateDao.findAll();
	}

	@InitBinder
	public void initBinder(final WebDataBinder webdataBinder) {
		webdataBinder.registerCustomEditor(ResourceType.class, new ResourceTypeConverter());
	}
	
	@GetMapping(value = "/{id}")
	public Template getTemplate(@PathVariable String id) {
		return templateDao.findById(id).get();
	}

	@DeleteMapping(value = "/{id}")
	public void deleteTemplate(@PathVariable String id) {
		templateDao.deleteById(id);
	}

	@PutMapping(value = "/{id}")
	public void updateTemplateMetadata(@RequestParam("id") @NotBlank String id,
			@RequestParam("description") String description, @RequestParam("type") ResourceType resourceType) {
		Template toUpdate = templateDao.findById(id).get();

		if (!StringUtils.isEmpty(description)) {
			toUpdate.setDescription(description);
		}

		if (resourceType != null) {
			toUpdate.setResourceType(resourceType);
		}
		templateDao.save(toUpdate);
	}

	@GetMapping(value = "/{id}/file")
	public ResponseEntity<Resource> getTemplateFile(@PathVariable String id) throws IOException {
		File templateFile = templateService.getTemplateFile(id);
		LOGGER.info("Returning file {} for template {}", templateFile.getName(), id);

		Path path = Paths.get(templateFile.getAbsolutePath());
		ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + templateFile.getName());

		return ResponseEntity.ok().headers(headers).contentLength(templateFile.length())
				.contentType(MediaType.parseMediaType("application/octet-stream")).body(resource);
	}
}
