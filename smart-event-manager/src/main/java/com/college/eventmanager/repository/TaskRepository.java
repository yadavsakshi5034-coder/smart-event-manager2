package com.college.eventmanager.repository;

import com.college.eventmanager.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByDoneFalse();

    List<Task> findByDoneTrueOrderByIdDesc();
}
