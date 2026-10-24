package com.college.eventmanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    /** 1 = highest priority, 5 = lowest. */
    private int priority;

    /** Optional: the event this task belongs to. */
    private Long eventId;

    private boolean done;
}
