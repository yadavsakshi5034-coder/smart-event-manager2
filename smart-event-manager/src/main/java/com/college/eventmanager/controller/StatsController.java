package com.college.eventmanager.controller;

import com.college.eventmanager.service.EventService;
import com.college.eventmanager.service.RegistrationService;
import com.college.eventmanager.service.TaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final EventService eventService;
    private final RegistrationService registrationService;
    private final TaskService taskService;

    public StatsController(EventService eventService, RegistrationService registrationService,
                           TaskService taskService) {
        this.eventService = eventService;
        this.registrationService = registrationService;
        this.taskService = taskService;
    }

    @GetMapping
    public Map<String, Object> stats() {
        Map<String, Long> totals = registrationService.totals();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalEvents", eventService.countAll());
        m.put("activeEvents", eventService.countActive());
        m.put("confirmed", totals.get("confirmed"));
        m.put("waitlisted", totals.get("waitlisted"));
        m.put("cancelled", totals.get("cancelled"));
        m.put("attended", totals.get("attended"));
        m.put("pendingTasks", taskService.pendingCount());
        return m;
    }
}
