package com.backend.project.controller;

import com.backend.project.dto.TaskPatchRequest;
import com.backend.project.dto.TaskRequest;
import com.backend.project.entity.Task;
import com.backend.project.entity.User;
import com.backend.project.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Protected REST API Controller demonstrating all standard HTTP methods:
 * - GET    -> Retrieve resource(s) [Safe, Idempotent]
 * - POST   -> Create a new resource [Not Idempotent] -> 201 Created
 * - PUT    -> Replace existing resource entirely [Idempotent] -> 200 OK
 * - PATCH  -> Update specific fields partially -> 200 OK
 * - DELETE -> Remove a resource [Idempotent] -> 204 No Content
 *
 * All endpoints require "Authorization: Bearer <token>".
 * @AuthenticationPrincipal injects the currently authenticated User directly from SecurityContext!
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository taskRepository;

    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * 1. GET /api/tasks
     * Returns all tasks belonging to the currently logged in user.
     * Status: 200 OK
     */
    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks(@AuthenticationPrincipal User user) {
        List<Task> tasks = taskRepository.findAllByUser(user);
        return ResponseEntity.ok(tasks);
    }

    /**
     * 2. GET /api/tasks/{id}
     * Returns a specific task by its ID.
     * Status: 200 OK or 404 NOT FOUND
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTaskById(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return taskRepository.findByIdAndUser(id, user)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Task with ID " + id + " not found!")));
    }

    /**
     * 3. POST /api/tasks
     * Creates a brand new task.
     * Status: 201 CREATED (Returns the created object with generated ID)
     */
    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody TaskRequest request,
                                           @AuthenticationPrincipal User user) {
        Task task = new Task(request.getTitle(), request.getDescription(), request.isCompleted(), user);
        Task savedTask = taskRepository.save(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTask);
    }

    /**
     * 4. PUT /api/tasks/{id}
     * FULL REPLACEMENT: Overwrites ALL fields with the incoming request body.
     * Status: 200 OK
     *
     * INTERVIEW CONCEPT:
     * With PUT, even if description was already there, if the client doesn't send it,
     * it gets overwritten. PUT is idempotent.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTaskEntirely(@PathVariable Long id,
                                                @RequestBody TaskRequest request,
                                                @AuthenticationPrincipal User user) {
        var optionalTask = taskRepository.findByIdAndUser(id, user);
        if (optionalTask.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Task with ID " + id + " not found!"));
        }

        Task task = optionalTask.get();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setCompleted(request.isCompleted());
        Task updated = taskRepository.save(task);

        return ResponseEntity.ok(updated);
    }

    /**
     * 5. PATCH /api/tasks/{id}
     * PARTIAL UPDATE: Only modifies fields that are explicitly provided (non-null).
     * Status: 200 OK
     *
     * INTERVIEW CONCEPT:
     * This is what they asked you in the interview!
     * Notice: If request.getTitle() is null, we DO NOT change the title.
     * Only provided fields are patched.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<?> patchTaskPartially(@PathVariable Long id,
                                                @RequestBody TaskPatchRequest request,
                                                @AuthenticationPrincipal User user) {
        var optionalTask = taskRepository.findByIdAndUser(id, user);
        if (optionalTask.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Task with ID " + id + " not found!"));
        }

        Task task = optionalTask.get();
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getCompleted() != null) {
            task.setCompleted(request.getCompleted());
        }
        Task patched = taskRepository.save(task);

        return ResponseEntity.ok(patched);
    }

    /**
     * 6. DELETE /api/tasks/{id}
     * Removes the task.
     * Status: 204 NO CONTENT (Standard REST response for successful deletion without a body)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id,
                                         @AuthenticationPrincipal User user) {
        return taskRepository.findByIdAndUser(id, user)
                .map(task -> {
                    taskRepository.delete(task);
                    return ResponseEntity.noContent().build(); // 204 No Content
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Task not found!")));
    }
}
