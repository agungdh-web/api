# AGENTS.md

## Build & Run

- Use `./mvnw` (Maven wrapper), not system `mvn`. Linux line endings enforced via `.gitattributes`.
- **Compile:** `./mvnw compile`
- **Run tests:** `./mvnw test`
- **Run a single test:** `./mvnw test -Dtest=ClassName#methodName`
- **Package:** `./mvnw package`
- **Run app:** `./mvnw spring-boot:run`

No lint/format/typecheck commands are configured (Spring Boot project, no Checkstyle/Spotless).

### Flyway (manual via Makefile)

Flyway does **not** auto-migrate on startup. Use `make`:

- `make` (or `make help`) — show available commands
- `make migrate` — run pending migrations
- `make refresh` — drop all tables and re-run migrations (confirms `y/N`)

**Always run `make migrate` before starting the app** if the schema hasn't been applied yet.

### Docker services

```sh
docker-compose up -d   # postgres, valkey (redis), minio, mailpit, adminer
```

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
  cli/                             — FlywayCli (manual migration runner)
  config/                          — FlywayConfig (manual Flyway bean, no auto-migrate)
  dto/                             — Java records, public-facing
  entity/                          — JPA entities
  mapper/                          — MapStruct interfaces
src/main/resources/application.yaml — datasource, Redis, mail, MinIO
src/main/resources/db/migration/    — Flyway SQL migrations
src/test/java/id/my/agungdh/api/    — tests
```

## Pagination

All list endpoints use **cursor-based pagination** (no offset/Pageable).

### Contract

Request: `?cursor=<uuid>&size=<n>` (both optional; `size` default 20, max 100)

Response:
```json
{
  "data": [...],
  "meta": { "nextCursor": "<uuid-or-null>", "size": 20, "hasMore": true }
}
```

### Cursor

- The cursor is the **uuid** of the last visible item in the previous response (`meta.nextCursor`).
- FE just copies `nextCursor` from the response into the next request — no encoding/decoding needed.
- Server resolves `uuid` → internal `id` (via hash index, O(1)) and runs `WHERE id < :id ORDER BY id DESC LIMIT n+1`.
- Invalid uuid format or uuid not found → `400 BAD_REQUEST`.
- `size` must be `1..100` (see `CursorResponse.MAX_SIZE`) — otherwise `400 BAD_REQUEST`.

### Sort

- Default: `id DESC` (newest first, by internal auto-increment `id`). PK-indexed, scales well.

### Repository convention

Two methods per repository, both returning `List<E>` (not `Page` / `Slice`):
- `findAllByOrderByIdDesc(Pageable)` — first page (no cursor)
- `findByIdLessThanOrderByIdDesc(Long id, Pageable)` — subsequent pages

For filtered lists (e.g. `Comment` with `parent IS NULL`), prefix the methods with the filter clause:
- `findByParentIsNullOrderByIdDesc(Pageable)`
- `findByParentIsNullAndIdLessThanOrderByIdDesc(Long id, Pageable)`

Service fetches `size + 1` and checks `result.size() > size` to detect `hasMore`.

### Why cursor (not offset)?

- Stable when items are inserted/deleted between requests (offsets shift).
- `id DESC` uses the PK index — no extra sort cost.
- No "total count" query (expensive on large tables).

## Entity / DTO / Mapper Conventions

### Entity

- **`id`**: `SERIAL` / `@GeneratedValue(strategy = IDENTITY)` — internal, **never exposed** in API responses
- **`uuid`**: `UUID` field generated via `UUID.randomUUID()` — the **public identifier**, always exposed in DTOs
- **UUID index**: use a **hash index** in Flyway migrations (`CREATE INDEX ... USING hash (uuid)`) — **no unique constraint** on `uuid`
- **FKs in DB**: reference `id` (internal), never `uuid`
- **Slug columns**: always `UNIQUE` (in migration and `@Column(unique = true)`)

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
- **Always add `@Mapping(target = "id", ignore = true)`** on every `toEntity` method — DTOs never carry `id`
- MapStruct generates the implementation at compile time

Example:
```java
@Mapper(componentModel = "spring")
public interface FooMapper {
    @Mapping(target = "id", ignore = true)
    Foo toEntity(FooDTO dto);

    FooDTO toDTO(Foo entity);

    List<FooDTO> toDTOList(List<Foo> entities);
}
```

## Framework Quirks

- **Flyway auto-configuration is not provided by Spring Boot 4.0.x** — Flyway is configured manually via `FlywayConfig.java` (a `@Configuration` class that creates a `Flyway` bean). Migrations must be triggered manually via `make migrate` (runs `FlywayCli.java`).
- **SpringDoc OpenAPI is on the classpath** — it auto-generates API docs; exclude it if you don't want Swagger UI exposed.
- **`.idea/` is gitignored** — IntelliJ project settings are local-only.
- **`HELP.md` is gitignored** — it's the generated Getting Started guide.
- **DevTools triggers auto-restart** on classpath changes in dev.
- **Virtual threads enabled** (`spring.threads.virtual.enabled: true`).

## Testing

- Tests use **JUnit 5** (via Spring Boot starter test).
- The only existing test is `@SpringBootTest contextLoads()` — it requires a running PostgreSQL and Redis to pass.
- Test starter dependencies are explicitly declared (not via the umbrella starter-test), so test slices (`@DataJpaTest`, `@WebMvcTest`, etc.) are available individually.
