package com.college.eventmanager;

import com.college.eventmanager.model.Event;
import com.college.eventmanager.model.Registration;
import com.college.eventmanager.model.Task;
import com.college.eventmanager.service.EventService;
import com.college.eventmanager.service.RegistrationService;
import com.college.eventmanager.service.TaskService;
import com.college.eventmanager.service.UndoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb")
@ActiveProfiles("test")
class EventFlowTest {

    @Autowired EventService eventService;
    @Autowired RegistrationService registrationService;
    @Autowired TaskService taskService;
    @Autowired UndoService undoService;

    private Event newEvent(String name, int capacity, LocalDate date) {
        Event e = new Event();
        e.setName(name);
        e.setCategory("Technical");
        e.setVenue("Hall");
        e.setCapacity(capacity);
        e.setEventDate(date);
        return eventService.create(e);
    }

    @Test
    void waitlistIsFifoAndPromotionIsAutomatic() {
        Event e = newEvent("Waitlist test", 2, LocalDate.now().plusDays(3));

        Registration a = registrationService.register(e.getId(), "A", "a@x.com");
        Registration b = registrationService.register(e.getId(), "B", "b@x.com");
        Registration c = registrationService.register(e.getId(), "C", "c@x.com");
        Registration d = registrationService.register(e.getId(), "D", "d@x.com");

        assertEquals(Registration.CONFIRMED, a.getStatus());
        assertEquals(Registration.CONFIRMED, b.getStatus());
        assertEquals(Registration.WAITLISTED, c.getStatus());
        assertEquals(Registration.WAITLISTED, d.getStatus());

        registrationService.cancel(a.getId()); // frees a seat -> C (first in queue) is promoted
        List<RegistrationService.RegistrationView> views = registrationService.forEvent(e.getId());
        assertEquals("CONFIRMED", views.get(2).status());   // C
        assertEquals("WAITLISTED", views.get(3).status());  // D still waiting
        assertEquals(1, views.get(3).waitlistPosition());
    }

    @Test
    void duplicateRegistrationIsRejected() {
        Event e = newEvent("Duplicate test", 5, LocalDate.now().plusDays(3));
        registrationService.register(e.getId(), "A", "dup@x.com");
        assertThrows(IllegalStateException.class,
                () -> registrationService.register(e.getId(), "A again", "DUP@x.com"));
    }

    @Test
    void tasksComeOutByPriorityAndUndoWorks() {
        Task low = taskService.add("low", 5, null);
        Task high = taskService.add("high", 1, null);
        Task mid = taskService.add("mid", 3, null);

        assertEquals(high.getId(), taskService.processNext().getId());
        undoService.undo(); // reopen "high"
        assertEquals(high.getId(), taskService.processNext().getId());
        assertEquals(mid.getId(), taskService.processNext().getId());
        assertEquals(low.getId(), taskService.processNext().getId());
    }

    @Test
    void eventsAreSortedByDate() {
        newEvent("Late", 5, LocalDate.now().plusDays(300));
        newEvent("Early", 5, LocalDate.now().plusDays(100));
        List<Event> events = eventService.search(null, null);
        for (int i = 1; i < events.size(); i++) {
            assertFalse(events.get(i - 1).getEventDate().isAfter(events.get(i).getEventDate()));
        }
    }
}
