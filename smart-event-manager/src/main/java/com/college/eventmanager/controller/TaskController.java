package com.college.eventmanager.controller;

import com.college.eventmanager.model.Task;
import com.college.eventmanager.service.TaskService;
import com.college.eventmanager.service.UndoService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TaskController {

    public record TaskRequest(String title, int priority, Long eventId) {
    }

    private final TaskService taskService;
    private final UndoService undoService;

    public TaskController(TaskService taskService, UndoService undoService) {
        this.taskService = taskService;
        this.undoService = undoService;
    }

    @GetMapping("/tasks")
    public Map<String, List<Task>> list() {
        Map<String, List<Task>> result = new HashMap<>();
        result.put("pending", taskService.pendingSorted());
        result.put("completed", taskService.completed());
        return result;
    }

    @PostMapping("/tasks")
    public Task add(@RequestBody TaskRequest req) {
        return taskService.add(req.title(), req.priority(), req.eventId());
    }

    @PostMapping("/tasks/next")
    public Task processNext() {
        return taskService.processNext();
    }

    @GetMapping("/undo")
    public Map<String, Object> lastUndoable() {
        Map<String, Object> m = new HashMap<>();
        m.put("next", undoService.peek());
        return m;
    }

    @PostMapping("/undo")
    public Map<String, Object> undo() {
        String undone = undoService.undo();
        return Map.of("undone", undone);
    }
}
