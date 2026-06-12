# AGENTS.md

## Build & Run

- Use `./mvnw` (Maven wrapper), not system `mvn`. Linux line endings enforced via `.gitattributes`.
- **Compile:** `./mvnw compile`
- **Run tests:** `./mvnw test`
- **Run a single test:** `./mvnw test -Dtest=ClassName#methodName`
- **Package:** `./mvnw package`
- **Run app:** `./mvnw spring-boot:run`

No lint/format/typecheck commands are configured (Spring Boot project, no Checkstyle/Spotless).

## Tech Stack & Versions

- **Java 25** (`<java.version>25</java.version>` in pom.xml)
- **Spring Boot 4.0.7** — parent POM, plugin, and all starters are 4.0.x
- **Build:** Maven with wrapper (`mvnw`)
- **Database:** PostgreSQL (runtime driver), JPA (Hibernate)
- **Cache:** Redis (spring-boot-starter-data-redis)
- **API docs:** SpringDoc OpenAPI 3.0.2 (Swagger UI at `/swagger-ui.html`)
- **Lombok** with annotation processor configured in `maven-compiler-plugin` for both compile and testCompile phases
- **Spring Boot DevTools** included (runtime, optional — auto-restart on classpath changes)

## Project Structure

```
src/main/java/id/my/agungdh/api/   — base package (no sub-packages yet)
src/main/resources/application.yaml — minimal config (only `spring.application.name: api`)
src/test/java/id/my/agungdh/api/    — tests
```

The project is a fresh Spring Boot skeleton with no controllers, services, entities, or configuration classes yet. The only files are `ApiApplication.java` (entry point) and `ApiApplicationTests.java` (context-loads smoke test).

## Framework Quirks

- **No datasource or Redis connection configured** — the app will fail to start without providing `spring.datasource.*` and `spring.data.redis.*` properties or disabling auto-configuration.
- **SpringDoc OpenAPI is on the classpath** — it auto-generates API docs; exclude it if you don't want Swagger UI exposed.
- **`.idea/` is gitignored** — IntelliJ project settings are local-only.
- **`HELP.md` is gitignored** — it's the generated Getting Started guide.

## Testing

- Tests use **JUnit 5** (via Spring Boot starter test).
- The only existing test is `@SpringBootTest contextLoads()` — it requires a running PostgreSQL and Redis to pass, or `@SpringBootTest` can be scoped with `@AutoConfigureTestDatabase` / test slices.
- Test starter dependencies are explicitly declared (not via the umbrella starter-test), so test slices (`@DataJpaTest`, `@WebMvcTest`, etc.) are available individually.
