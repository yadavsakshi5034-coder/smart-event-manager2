package com.college.eventmanager.config;

import com.college.eventmanager.model.Event;
import com.college.eventmanager.service.EventService;
import com.college.eventmanager.service.TaskService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Adds a few demo events on the very first run so the portal is not empty. */
@Component
@Profile("!test")
public class SampleDataLoader implements CommandLineRunner {

    private final EventService eventService;
    private final TaskService taskService;

    public SampleDataLoader(EventService eventService, TaskService taskService) {
        this.eventService = eventService;
        this.taskService = taskService;
    }

    @Override
    public void run(String... args) {
        if (eventService.countAll() > 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        eventService.create(event("Java Coding Contest", "Technical", today.plusDays(7),
                "Computer Lab 2", 3, "Solve data-structure problems against the clock."));
        eventService.create(event("Annual Cultural Fest", "Cultural", today.plusDays(14),
                "Main Auditorium", 100, "Music, dance and drama performances."));
        eventService.create(event("AI Workshop", "Workshop", today.plusDays(21),
                "Seminar Hall", 40, "Hands-on introduction to machine learning."));

        taskService.add("Book projector and mic", 2, null);
        taskService.add("Confirm chief guest", 1, null);
        taskService.add("Print certificates", 4, null);
    }

    private Event event(String name, String category, LocalDate date, String venue,
                        int capacity, String description) {
        Event e = new Event();
        e.setName(name);
        e.setCategory(category);
        e.setEventDate(date);
        e.setVenue(venue);
        e.setCapacity(capacity);
        e.setDescription(description);
        return e;
    }
}
