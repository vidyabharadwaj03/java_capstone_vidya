# Digital Library Management System API

A Spring Boot API for browsing a book catalog, reserving books, and managing checkouts/returns with JWT-based auth.

- **Milestone 1** — Data model: `User`, `Book`, and `Reservation` entities with JPA repositories.
- **Milestone 2** — Auth: registration, login, and JWT-secured profile endpoint.
- **Milestone 3** — Catalog: paginated book browsing with search and filters.
- **Milestone 4** — Reservations: reserve, checkout, return, and borrowing history with late fees.
- **Milestone 5** — Testing: unit and repository tests at 95%+ line coverage.
- **Milestone 6** — Deployment: not yet done.

Run locally with `./mvnw spring-boot:run` (H2 in-memory database, port 8080).
