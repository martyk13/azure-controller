package com.amplify.ap.domain;

import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotNull;

/**
 * Domain model for Azure Templates and associated metadata
 */
public class Template {

    @Id
    private String id;

    @NotNull
    private String filePath;

    private String description;

    @NotNull
    private String author;

    @NotNull
    private String authorEmail;

    @NotNull
    private String lastCommitter;

    @NotNull
    private String lastCommitterEmail;

    @NotNull
    private String lastCommitMessage;

    @NotNull
    private int lastCommitTime;

    public Template() {
    }

    public Template(String filePath, String description, String author, String authorEmail, String lastCommitter, String lastCommitterEmail, String lastCommitMessage, int lastCommitTime) {
        this.filePath = filePath;
        this.description = description;
        this.author = author;
        this.authorEmail = authorEmail;
        this.lastCommitter = lastCommitter;
        this.lastCommitterEmail = lastCommitterEmail;
        this.lastCommitMessage = lastCommitMessage;
        this.lastCommitTime = lastCommitTime;
    }

    public void update(Template template){
        this.lastCommitter = template.getLastCommitter();
        this.lastCommitterEmail = template.getLastCommitterEmail();
        this.lastCommitMessage = template.getLastCommitMessage();
        this.lastCommitTime = template.getLastCommitTime();
    }

    public String getId() {
        return id;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public String getLastCommitter() {
        return lastCommitter;
    }

    public void setLastCommitter(String lastCommitter) {
        this.lastCommitter = lastCommitter;
    }

    public String getLastCommitterEmail() {
        return lastCommitterEmail;
    }

    public void setLastCommitterEmail(String lastCommitterEmail) {
        this.lastCommitterEmail = lastCommitterEmail;
    }

    public String getLastCommitMessage() {
        return lastCommitMessage;
    }

    public void setLastCommitMessage(String lastCommitMessage) {
        this.lastCommitMessage = lastCommitMessage;
    }

    public int getLastCommitTime() {
        return lastCommitTime;
    }

    public void setLastCommitTime(int lastCommitTime) {
        this.lastCommitTime = lastCommitTime;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Template{");
        sb.append("id='").append(id).append('\'');
        sb.append(", filePath='").append(filePath).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", author='").append(author).append('\'');
        sb.append(", authorEmail='").append(authorEmail).append('\'');
        sb.append(", lastCommitter='").append(lastCommitter).append('\'');
        sb.append(", lastCommitterEmail='").append(lastCommitterEmail).append('\'');
        sb.append(", lastCommitMessage='").append(lastCommitMessage).append('\'');
        sb.append(", lastCommitTime=").append(lastCommitTime);
        sb.append('}');
        return sb.toString();
    }
}
