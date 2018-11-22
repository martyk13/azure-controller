package com.kenesys.analysisplatform.domain;

import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotNull;

/**
 * Domain model for Azure Templates and associated metadata
 */
public class Template {

    @Id
    String id;

    @NotNull
    String name;

    @NotNull
    String description;

    @NotNull
    String templateUrl;

    public Template() {
    }

    public Template(String name, String description, String templateUrl) {
        this.name = name;
        this.description = description;
        this.templateUrl = templateUrl;
    }

    public void update(Template template){
        this.name = template.getName();
        this.description = template.getDescription();
        this.templateUrl = template.getTemplateUrl();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTemplateUrl() {
        return templateUrl;
    }

    public void setTemplateUrl(String templateUrl) {
        this.templateUrl = templateUrl;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Template{");
        sb.append("id='").append(id).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", templateUrl='").append(templateUrl).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
