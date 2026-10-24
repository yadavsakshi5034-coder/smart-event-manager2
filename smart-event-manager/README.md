# Smart Event Manager

A college event-management web app built with **Java 17, Spring Boot 3, H2 and Bootstrap**.
Students browse and register for events (with an automatic waitlist); organizers create events,
track attendance, and manage prioritized tasks with undo.

## Data structures and where they are used

| Data structure | Where | File |
|---|---|---|
| `HashMap` | O(1) event lookup by ID | `service/EventService.java` |
| `ArrayList` | Event lists, participant lists | `EventService`, `RegistrationService` |
| `Queue` (`LinkedList`) | FIFO waitlist per event, automatic promotion | `service/RegistrationService.java` |
| `PriorityQueue` | Tasks processed by priority (1 = highest) | `service/TaskService.java` |
| `Stack` | Undo history (add task, complete task, attendance) | `service/UndoService.java` |
| Merge sort | Events sorted by date | `EventService.mergeSort` |

The H2 database stores everything permanently. The collections above are rebuilt from the
database at startup, so nothing is lost on restart.

## Run locally

Requirements: JDK 17+ and Maven 3.8+.

```bash
mvn spring-boot:run
```

Open:
- Student portal: http://localhost:8080
- Organizer dashboard: http://localhost:8080/organizer.html
- Database console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/eventdb`, user `sa`, empty password)

Three sample events and tasks are added on the first run.

Run the tests: `mvn test`

## Demo script (for your presentation)

1. Organizer dashboard, Events tab: the "Java Coding Contest" has capacity **3**.
2. Student portal: register four different people for it. The fourth is **waitlisted**.
3. Organizer, Participants tab: cancel the first student. The waitlisted student becomes **CONFIRMED** automatically.
4. Mark a participant present, then press **Undo**.
5. Tasks tab: add tasks with different priorities, press **Process next task**. The most urgent one is completed first.

## REST API

| Method | URL | Purpose |
|---|---|---|
| GET | `/api/events?search=&category=` | List/search events (sorted by date) |
| GET / POST / PUT / DELETE | `/api/events[/{id}]` | Get, create, update, cancel event |
| POST | `/api/events/{id}/register` | Register or join waitlist `{name, email}` |
| GET | `/api/events/{id}/registrations` | Participants of an event |
| GET | `/api/registrations/status?email=` | A student's registrations |
| DELETE | `/api/registrations/{id}` | Cancel (promotes next in queue) |
| PUT | `/api/registrations/{id}/attend?present=true` | Mark attendance |
| GET / POST | `/api/tasks` | List / add task `{title, priority, eventId}` |
| POST | `/api/tasks/next` | Complete highest-priority task |
| GET / POST | `/api/undo` | Peek / perform undo |
| GET | `/api/stats` | Dashboard numbers |

## Deploy

### Render (free) using Docker
1. Push this folder to a GitHub repository.
2. On https://render.com choose **New > Web Service**, connect the repository.
3. Set **Runtime = Docker**. No other settings are needed (the app reads the `PORT` variable).
4. Deploy and open the URL Render gives you.

Note: on Render's free plan the disk is temporary, so the H2 data resets when the service restarts.
That is fine for a demo. For permanent data, create a free PostgreSQL database, add the
`org.postgresql:postgresql` dependency to `pom.xml`, and set these environment variables:
`DB_URL=jdbc:postgresql://HOST/DBNAME`, `spring.datasource.username`, `spring.datasource.password`.

### Railway
New Project > Deploy from GitHub repo. It detects the Dockerfile automatically.

### Run the packaged jar anywhere with Java 17
```bash
mvn clean package
java -jar target/smart-event-manager-1.0.0.jar
```

## Project structure

```
src/main/java/com/college/eventmanager/
  model/        Event, Registration, Task
  repository/   Spring Data JPA repositories
  service/      EventService, RegistrationService, TaskService, UndoService
  controller/   REST controllers + error handler
  config/       SampleDataLoader
src/main/resources/
  application.properties
  static/       index.html, organizer.html, css/, js/
src/test/       EventFlowTest (waitlist, priority, undo, sorting)
```
