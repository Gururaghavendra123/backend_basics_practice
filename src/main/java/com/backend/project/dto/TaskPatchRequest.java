package com.backend.project.dto;

/**
 * DTO specifically for PATCH requests (Partial Updates).
 * Uses boxed Boolean and Object types so that non-provided fields are null.
 * 
 * INTERVIEW TIP:
 * Why boxed Boolean instead of primitive boolean?
 * Primitive boolean defaults to false if omitted! 
 * Boxed Boolean allows null, meaning "the client didn't ask to change this field".
 */
public class TaskPatchRequest {
    private String title;
    private String description;
    private Boolean completed;

    public TaskPatchRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
}
