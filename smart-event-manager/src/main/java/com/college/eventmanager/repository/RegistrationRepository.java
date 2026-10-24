package com.college.eventmanager.repository;

import com.college.eventmanager.model.Registration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    List<Registration> findByEventIdOrderByIdAsc(Long eventId);

    List<Registration> findByStudentEmailIgnoreCaseOrderByIdDesc(String email);

    List<Registration> findByStatusOrderByIdAsc(String status);

    long countByEventIdAndStatus(Long eventId, String status);

    long countByStatus(String status);

    long countByAttendedTrue();

    boolean existsByEventIdAndStudentEmailIgnoreCaseAndStatusNot(Long eventId, String email, String status);
}
