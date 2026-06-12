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
- **Database:** PostgreSQL (runtime driver), JPA (Hibernate), **Flyway** for migrations
- **Cache:** Redis (spring-boot-starter-data-redis)
- **API docs:** SpringDoc OpenAPI 3.0.2 (Swagger UI at `/swagger-ui.html`)
- **Lombok** with annotation processor configured in `maven-compiler-plugin` for both compile and testCompile phases
- **MapStruct 1.6.3** for entity ↔ DTO mapping, with `lombok-mapstruct-binding` (0.2.0)
- **Spring Boot DevTools** included (runtime, optional — auto-restart on classpath changes)

## Project Structure

```
src/main/java/id/my/agungdh/api/   — base package
src/main/resources/application.yaml — application config (datasource, Redis, Flyway, MinIO, mail)
src/main/resources/db/migration/    — Flyway SQL migrations
src/test/java/id/my/agungdh/api/    — tests
```

## Entity / DTO / Mapper Conventions

### Entity

- **`id`**: `SERIAL` / `@GeneratedValue(strategy = IDENTITY)` — internal, **never exposed** in API responses
- **`uuid`**: `UUID` field generated via `UUID.randomUUID()` — the **public identifier**, always exposed in DTOs
- **UUID index**: use a **hash index** in Flyway migrations (`CREATE INDEX ... USING hash (uuid)`) — **no unique constraint** on `uuid`

Example entity structure:
```java
@Entity
@Table(name = "foo")
public class Foo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID uuid;

    // business fields...
}
```

### Flyway Migrations

- Place SQL files in `src/main/resources/db/migration/`
- Naming: `V{version}__{description}.sql` (double underscore after version)
- Always define hash indexes on `uuid` columns in the migration:
```sql
CREATE TABLE foo (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL,
    -- other columns...
);
CREATE INDEX idx_foo_uuid ON foo USING hash (uuid);
```

### DTO

- Use **Java `record`** (not class with Lombok)
- Only expose the public `uuid` field — never expose `id`
- Use validation annotations from `jakarta.validation` on the record components

Example:
```java
public record FooDTO(
    UUID uuid,
    @NotBlank String name
) {}
```

### Mapper (MapStruct)

- Interface annotated `@Mapper(componentModel = "spring")`
- Define methods: `toEntity(DTO)`, `toDTO(Entity)`, `toDTOList(List<Entity>)`
- MapStruct generates the implementation at compile time

Example:
```java
@Mapper(componentModel = "spring")
public interface FooMapper {
    Foo toEntity(FooDTO dto);
    FooDTO toDTO(Foo entity);
    List<FooDTO> toDTOList(List<Foo> entities);
}
```

## Framework Quirks

- **No datasource or Redis connection configured** — the app will fail to start without providing `spring.datasource.*` and `spring.data.redis.*` properties or disabling auto-configuration.
- **SpringDoc OpenAPI is on the classpath** — it auto-generates API docs; exclude it if you don't want Swagger UI exposed.
- **`.idea/` is gitignored** — IntelliJ project settings are local-only.
- **`HELP.md` is gitignored** — it's the generated Getting Started guide.

## Testing

- Tests use **JUnit 5** (via Spring Boot starter test).
- The only existing test is `@SpringBootTest contextLoads()` — it requires a running PostgreSQL and Redis to pass, or `@SpringBootTest` can be scoped with `@AutoConfigureTestDatabase` / test slices.
- Test starter dependencies are explicitly declared (not via the umbrella starter-test), so test slices (`@DataJpaTest`, `@WebMvcTest`, etc.) are available individually.
