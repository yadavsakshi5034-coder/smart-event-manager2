package com.college.eventmanager.controller;

import com.college.eventmanager.model.Event;
import com.college.eventmanager.service.EventService;
import com.college.eventmanager.service.RegistrationService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    /** Event plus live seat information for the UI. */
    public record EventView(Long id, String name, String category, String description,
                            LocalDate eventDate, String venue, int capacity, String status,
                            long confirmed, int waitlisted, long seatsLeft) {
    }

    private final EventService eventService;
    private final RegistrationService registrationService;

    public EventController(EventService eventService, RegistrationService registrationService) {
        this.eventService = eventService;
        this.registrationService = registrationService;
    }

    @GetMapping
    public List<EventView> list(@RequestParam(required = false) String search,
                                @RequestParam(required = false) String category) {
        List<EventView> views = new ArrayList<>();
        for (Event e : eventService.search(search, category)) {
            views.add(toView(e));
        }
        return views;
    }

    @GetMapping("/{id}")
    public EventView get(@PathVariable Long id) {
        return toView(eventService.get(id));
    }

    @PostMapping
    public EventView create(@RequestBody Event event) {
        return toView(eventService.create(event));
    }

    @PutMapping("/{id}")
    public EventView update(@PathVariable Long id, @RequestBody Event event) {
        return toView(eventService.update(id, event));
    }

    @DeleteMapping("/{id}")
    public EventView cancel(@PathVariable Long id) {
        Event e = eventService.cancel(id);
        registrationService.cancelAllForEvent(id);
        return toView(e);
    }

    private EventView toView(Event e) {
        long confirmed = registrationService.confirmedCount(e.getId());
        int waitlisted = registrationService.waitlistSize(e.getId());
        long seatsLeft = Math.max(0, e.getCapacity() - confirmed);
        return new EventView(e.getId(), e.getName(), e.getCategory(), e.getDescription(),
                e.getEventDate(), e.getVenue(), e.getCapacity(), e.getStatus(),
                confirmed, waitlisted, seatsLeft);
    }
}
