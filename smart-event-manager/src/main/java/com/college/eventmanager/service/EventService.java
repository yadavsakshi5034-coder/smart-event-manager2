package com.college.eventmanager.service;

import com.college.eventmanager.model.Event;
import com.college.eventmanager.repository.EventRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * DATA STRUCTURES:
 *  - HashMap  : eventMap gives O(1) lookup of an event by its ID.
 *  - ArrayList: search results are collected into a list for display.
 *  - Sorting  : a hand-written merge sort arranges events by date.
 * The database stays the source of truth; the map is an in-memory index.
 */
@Service
public class EventService {

    private final EventRepository repo;
    private final Map<Long, Event> eventMap = new HashMap<>();

    public EventService(EventRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    public synchronized void loadCache() {
        eventMap.clear();
        for (Event e : repo.findAll()) {
            eventMap.put(e.getId(), e);
        }
    }

    public synchronized Event get(Long id) {
        Event e = eventMap.get(id); // O(1)
        if (e == null) {
            throw new NoSuchElementException("Event " + id + " not found");
        }
        return e;
    }

    public synchronized List<Event> search(String text, String category) {
        List<Event> result = new ArrayList<>();
        for (Event e : eventMap.values()) {
            boolean nameOk = text == null || text.isBlank()
                    || contains(e.getName(), text) || contains(e.getVenue(), text);
            boolean categoryOk = category == null || category.isBlank()
                    || category.equalsIgnoreCase("all")
                    || category.equalsIgnoreCase(e.getCategory());
            if (nameOk && categoryOk) {
                result.add(e);
            }
        }
        return mergeSort(result, Comparator.comparing(Event::getEventDate,
                Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(Event::getId));
    }

    public synchronized Event create(Event input) {
        validate(input);
        input.setId(null);
        input.setStatus(Event.ACTIVE);
        Event saved = repo.save(input);
        eventMap.put(saved.getId(), saved);
        return saved;
    }

    public synchronized Event update(Long id, Event input) {
        Event existing = get(id);
        validate(input);
        existing.setName(input.getName());
        existing.setCategory(input.getCategory());
        existing.setDescription(input.getDescription());
        existing.setEventDate(input.getEventDate());
        existing.setVenue(input.getVenue());
        existing.setCapacity(input.getCapacity());
        Event saved = repo.save(existing);
        eventMap.put(saved.getId(), saved);
        return saved;
    }

    public synchronized Event cancel(Long id) {
        Event existing = get(id);
        existing.setStatus(Event.CANCELLED);
        Event saved = repo.save(existing);
        eventMap.put(saved.getId(), saved);
        return saved;
    }

    public synchronized int countAll() {
        return eventMap.size();
    }

    public synchronized long countActive() {
        return eventMap.values().stream().filter(e -> Event.ACTIVE.equals(e.getStatus())).count();
    }

    // ---------- helpers ----------

    private void validate(Event e) {
        if (e.getName() == null || e.getName().isBlank()) {
            throw new IllegalArgumentException("Event name is required");
        }
        if (e.getEventDate() == null) {
            throw new IllegalArgumentException("Event date is required");
        }
        if (e.getCapacity() < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1");
        }
    }

    private boolean contains(String value, String text) {
        return value != null && value.toLowerCase().contains(text.toLowerCase());
    }

    /** Classic top-down merge sort (stable, O(n log n)). */
    static <T> List<T> mergeSort(List<T> list, Comparator<T> cmp) {
        if (list.size() <= 1) {
            return list;
        }
        int mid = list.size() / 2;
        List<T> left = mergeSort(new ArrayList<>(list.subList(0, mid)), cmp);
        List<T> right = mergeSort(new ArrayList<>(list.subList(mid, list.size())), cmp);

        List<T> merged = new ArrayList<>(list.size());
        int i = 0, j = 0;
        while (i < left.size() && j < right.size()) {
            if (cmp.compare(left.get(i), right.get(j)) <= 0) {
                merged.add(left.get(i++));
            } else {
                merged.add(right.get(j++));
            }
        }
        while (i < left.size()) merged.add(left.get(i++));
        while (j < right.size()) merged.add(right.get(j++));
        return merged;
    }
}
