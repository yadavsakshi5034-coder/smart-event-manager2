package com.college.eventmanager.service;

import com.college.eventmanager.model.Task;
import com.college.eventmanager.repository.TaskRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

/**
 * DATA STRUCTURE: PriorityQueue (min-heap).
 * Tasks with a smaller priority number (1 = most urgent) always come out first.
 * Ties are broken by id, so older tasks go first.
 */
@Service
public class TaskService {

    private static final Comparator<Task> ORDER =
            Comparator.comparingInt(Task::getPriority).thenComparing(Task::getId);

    private final TaskRepository repo;
    private final UndoService undoService;
    private final PriorityQueue<Task> pending = new PriorityQueue<>(ORDER);

    public TaskService(TaskRepository repo, UndoService undoService) {
        this.repo = repo;
        this.undoService = undoService;
    }

    @PostConstruct
    public synchronized void loadPending() {
        pending.clear();
        pending.addAll(repo.findByDoneFalse());
    }

    public synchronized Task add(String title, int priority, Long eventId) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Task title is required");
        }
        if (priority < 1 || priority > 5) {
            throw new IllegalArgumentException("Priority must be between 1 (highest) and 5 (lowest)");
        }
        Task t = new Task();
        t.setTitle(title.trim());
        t.setPriority(priority);
        t.setEventId(eventId);
        t = repo.save(t);
        pending.add(t); // O(log n)

        final Task added = t;
        undoService.push("Added task \"" + t.getTitle() + "\"", () -> removeTask(added));
        return t;
    }

    /** Take the most urgent task off the heap and mark it done. */
    public synchronized Task processNext() {
        Task t = pending.poll(); // always the highest priority
        if (t == null) {
            throw new NoSuchElementException("No pending tasks");
        }
        t.setDone(true);
        repo.save(t);
        undoService.push("Completed task \"" + t.getTitle() + "\"", () -> reopenTask(t));
        return t;
    }

    /** Pending tasks in priority order (a PriorityQueue iterates unordered, so we sort a copy). */
    public synchronized List<Task> pendingSorted() {
        List<Task> list = new ArrayList<>(pending);
        list.sort(ORDER);
        return list;
    }

    public synchronized List<Task> completed() {
        return repo.findByDoneTrueOrderByIdDesc();
    }

    public synchronized int pendingCount() {
        return pending.size();
    }

    // ---------- undo actions ----------

    private synchronized void removeTask(Task t) {
        pending.remove(t);
        repo.deleteById(t.getId());
    }

    private synchronized void reopenTask(Task t) {
        t.setDone(false);
        repo.save(t);
        pending.add(t);
    }
}
