package com.college.eventmanager.controller;

import com.college.eventmanager.model.Registration;
import com.college.eventmanager.service.RegistrationService;
import com.college.eventmanager.service.RegistrationService.RegistrationView;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RegistrationController {

    public record RegisterRequest(String name, String email) {
    }

    private final RegistrationService service;

    public RegistrationController(RegistrationService service) {
        this.service = service;
    }

    @PostMapping("/events/{eventId}/register")
    public Map<String, Object> register(@PathVariable Long eventId, @RequestBody RegisterRequest req) {
        Registration r = service.register(eventId, req.name(), req.email());
        String message = Registration.CONFIRMED.equals(r.getStatus())
                ? "You are registered! Your seat is confirmed."
                : "The event is full - you have been added to the waitlist.";
        return Map.of("id", r.getId(), "status", r.getStatus(), "message", message);
    }

    @GetMapping("/events/{eventId}/registrations")
    public List<RegistrationView> forEvent(@PathVariable Long eventId) {
        return service.forEvent(eventId);
    }

    @GetMapping("/registrations/status")
    public List<RegistrationView> status(@RequestParam String email) {
        return service.forEmail(email);
    }

    @DeleteMapping("/registrations/{id}")
    public Map<String, Object> cancel(@PathVariable Long id) {
        Registration r = service.cancel(id);
        return Map.of("id", r.getId(), "status", r.getStatus());
    }

    @PutMapping("/registrations/{id}/attend")
    public Map<String, Object> attend(@PathVariable Long id,
                                      @RequestParam(defaultValue = "true") boolean present) {
        Registration r = service.setAttendance(id, present);
        return Map.of("id", r.getId(), "attended", r.isAttended());
    }
}
