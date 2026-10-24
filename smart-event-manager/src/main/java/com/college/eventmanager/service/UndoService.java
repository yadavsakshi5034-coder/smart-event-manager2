package com.college.eventmanager.service;

import org.springframework.stereotype.Service;

import java.util.Stack;

/**
 * DATA STRUCTURE: Stack (LIFO).
 * Every supported operation pushes the action that reverses it,
 * so "undo" simply pops the most recent one and runs it.
 */
@Service
public class UndoService {

    public record UndoAction(String description, Runnable action) {
    }

    private static final int MAX_HISTORY = 50;

    private final Stack<UndoAction> history = new Stack<>();

    public void push(String description, Runnable reverseAction) {
        history.push(new UndoAction(description, reverseAction));
        if (history.size() > MAX_HISTORY) {
            history.remove(0); // drop the oldest entry
        }
    }

    /** Description of what the next undo would reverse, or null if nothing to undo. */
    public String peek() {
        return history.isEmpty() ? null : history.peek().description();
    }

    public String undo() {
        if (history.isEmpty()) {
            throw new IllegalStateException("Nothing to undo");
        }
        UndoAction last = history.pop();
        last.action().run();
        return last.description();
    }
}
