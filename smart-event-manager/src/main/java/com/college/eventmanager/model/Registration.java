package com.college.eventmanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "registrations")
@Getter
@Setter
@NoArgsConstructor
public class Registration {

    public static final String CONFIRMED = "CONFIRMED";
    public static final String WAITLISTED = "WAITLISTED";
    public static final String CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long eventId;
    private String studentName;
    private String studentEmail;
    private String status;
    private boolean attended;
    private LocalDateTime registeredAt = LocalDateTime.now();
}
