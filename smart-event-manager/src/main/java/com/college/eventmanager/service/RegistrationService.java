package com.college.eventmanager.service;

import com.college.eventmanager.model.Event;
import com.college.eventmanager.model.Registration;
import com.college.eventmanager.repository.RegistrationRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * DATA STRUCTURE: Queue (FIFO).
 * Each event has its own waitlist queue. People who register after the event is
 * full join the back of the queue; when a confirmed person cancels, the person
 * at the FRONT of the queue is promoted automatically.
 */
@Service
public class RegistrationService {

    /** Flat view sent to the frontend. */
    public record RegistrationView(Long id, Long eventId, String eventName, String studentName,
                                   String studentEmail, String status, boolean attended,
                                   int waitlistPosition) {
    }

    private final RegistrationRepository repo;
    private final EventService eventService;
    private final UndoService undoService;

    private final Map<Long, Queue<Registration>> waitlists = new HashMap<>();

    public RegistrationService(RegistrationRepository repo, EventService eventService,
                               UndoService undoService) {
        this.repo = repo;
        this.eventService = eventService;
        this.undoService = undoService;
    }

    /** Rebuild the in-memory queues from the database after a restart. */
    @PostConstruct
    public synchronized void rebuildWaitlists() {
        waitlists.clear();
        for (Registration r : repo.findByStatusOrderByIdAsc(Registration.WAITLISTED)) {
            waitlists.computeIfAbsent(r.getEventId(), k -> new LinkedList<>()).add(r);
        }
    }

    public synchronized Registration register(Long eventId, String name, String email) {
        Event event = eventService.get(eventId);
        if (!Event.ACTIVE.equals(event.getStatus())) {
            throw new IllegalStateException("This event has been cancelled");
        }
        if (name == null || name.isBlank() || email == null || email.isBlank()) {
            throw new IllegalArgumentException("Name and email are required");
        }
        if (repo.existsByEventIdAndStudentEmailIgnoreCaseAndStatusNot(eventId, email.trim(),
                Registration.CANCELLED)) {
            throw new IllegalStateException("This email is already registered for the event");
        }

        Registration r = new Registration();
        r.setEventId(eventId);
        r.setStudentName(name.trim());
        r.setStudentEmail(email.trim());

        long confirmed = repo.countByEventIdAndStatus(eventId, Registration.CONFIRMED);
        if (confirmed < event.getCapacity()) {
            r.setStatus(Registration.CONFIRMED);
            return repo.save(r);
        }
        r.setStatus(Registration.WAITLISTED);
        r = repo.save(r);
        waitlists.computeIfAbsent(eventId, k -> new LinkedList<>()).add(r); // enqueue
        return r;
    }

    public synchronized Registration cancel(Long registrationId) {
        Registration r = repo.findById(registrationId)
                .orElseThrow(() -> new NoSuchElementException("Registration not found"));
        if (Registration.CANCELLED.equals(r.getStatus())) {
            throw new IllegalStateException("Registration is already cancelled");
        }
        boolean wasConfirmed = Registration.CONFIRMED.equals(r.getStatus());

        Queue<Registration> queue = waitlists.get(r.getEventId());
        if (!wasConfirmed && queue != null) {
            queue.removeIf(w -> w.getId().equals(r.getId()));
        }
        r.setStatus(Registration.CANCELLED);
        repo.save(r);

        if (wasConfirmed) {
            promoteNext(r.getEventId());
        }
        return r;
    }

    /** Promote the front of the waitlist (dequeue) to CONFIRMED. */
    private void promoteNext(Long eventId) {
        Queue<Registration> queue = waitlists.get(eventId);
        if (queue == null || queue.isEmpty()) {
            return;
        }
        Registration next = queue.poll(); // FIFO: the person who waited longest
        next.setStatus(Registration.CONFIRMED);
        repo.save(next);
    }

    public synchronized Registration setAttendance(Long registrationId, boolean attended) {
        Registration r = repo.findById(registrationId)
                .orElseThrow(() -> new NoSuchElementException("Registration not found"));
        if (!Registration.CONFIRMED.equals(r.getStatus())) {
            throw new IllegalStateException("Only confirmed participants can be marked present");
        }
        boolean previous = r.isAttended();
        r.setAttended(attended);
        repo.save(r);
        undoService.push((attended ? "Marked " : "Unmarked ") + r.getStudentName() + " present",
                () -> restoreAttendance(registrationId, previous));
        return r;
    }

    private synchronized void restoreAttendance(Long registrationId, boolean value) {
        repo.findById(registrationId).ifPresent(r -> {
            r.setAttended(value);
            repo.save(r);
        });
    }

    /** When an organizer cancels an event, everyone's registration is cancelled too. */
    public synchronized void cancelAllForEvent(Long eventId) {
        waitlists.remove(eventId);
        for (Registration r : repo.findByEventIdOrderByIdAsc(eventId)) {
            if (!Registration.CANCELLED.equals(r.getStatus())) {
                r.setStatus(Registration.CANCELLED);
                repo.save(r);
            }
        }
    }

    public synchronized List<RegistrationView> forEvent(Long eventId) {
        List<RegistrationView> views = new ArrayList<>();
        for (Registration r : repo.findByEventIdOrderByIdAsc(eventId)) {
            views.add(toView(r));
        }
        return views;
    }

    public synchronized List<RegistrationView> forEmail(String email) {
        List<RegistrationView> views = new ArrayList<>();
        for (Registration r : repo.findByStudentEmailIgnoreCaseOrderByIdDesc(email.trim())) {
            views.add(toView(r));
        }
        return views;
    }

    public synchronized long confirmedCount(Long eventId) {
        return repo.countByEventIdAndStatus(eventId, Registration.CONFIRMED);
    }

    public synchronized int waitlistSize(Long eventId) {
        Queue<Registration> q = waitlists.get(eventId);
        return q == null ? 0 : q.size();
    }

    public synchronized Map<String, Long> totals() {
        Map<String, Long> t = new HashMap<>();
        t.put("confirmed", repo.countByStatus(Registration.CONFIRMED));
        t.put("waitlisted", repo.countByStatus(Registration.WAITLISTED));
        t.put("cancelled", repo.countByStatus(Registration.CANCELLED));
        t.put("attended", repo.countByAttendedTrue());
        return t;
    }

    // ---------- helpers ----------

    private RegistrationView toView(Registration r) {
        String eventName;
        try {
            eventName = eventService.get(r.getEventId()).getName();
        } catch (NoSuchElementException e) {
            eventName = "(deleted event)";
        }
        int position = 0;
        if (Registration.WAITLISTED.equals(r.getStatus())) {
            Queue<Registration> q = waitlists.get(r.getEventId());
            if (q != null) {
                int i = 1;
                for (Registration w : q) {
                    if (w.getId().equals(r.getId())) {
                        position = i;
                        break;
                    }
                    i++;
                }
            }
        }
        return new RegistrationView(r.getId(), r.getEventId(), eventName, r.getStudentName(),
                r.getStudentEmail(), r.getStatus(), r.isAttended(), position);
    }
}
