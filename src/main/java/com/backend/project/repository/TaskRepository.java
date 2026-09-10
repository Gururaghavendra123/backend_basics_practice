package com.backend.project.repository;

import com.backend.project.entity.Task;
import com.backend.project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Fetch only the tasks belonging to the authenticated user
    List<Task> findAllByUser(User user);

    Optional<Task> findByIdAndUser(Long id, User user);
}
