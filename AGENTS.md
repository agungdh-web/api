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

Request: `?cursor=<uuid>&sort=<field>&dir=<asc|desc>&size=<n>`
- All params optional; `size` default 20, max 100.
- No `sort` param → default sort = `id DESC` (newest first by internal auto-increment `id`).
- `sort` provided without `dir` → default `dir` = `asc`.
- Invalid `sort` field or `dir` value → `400 BAD_REQUEST`.

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
- Server resolves `uuid` → internal `id` (via hash index, O(1)) AND looks up the sort field value of the cursor entity. WHERE clause is:
  ```
  (sort_field > :sortValue) OR (sort_field = :sortValue AND id > :id)
  ```
  (flipped for DESC). `id` is always the secondary sort (tie-breaker).
- **FE must pass the same `sort` and `dir` on every request along with the cursor** — the server uses them to generate the WHERE clause. Switching `sort` between requests requires a fresh cursor (the old one no longer points to the right place).
- Invalid uuid format or uuid not found → `400 BAD_REQUEST`.
- `size` must be `1..100` (see `CursorResponse.MAX_SIZE`) — otherwise `400 BAD_REQUEST`.

### Sort

- Default: `id DESC` (newest first, by internal auto-increment `id`). PK-indexed, scales well.
- Each entity has an allowlist of sortable fields. Fields not in the allowlist → 400.
  - Category: `id`, `name`
  - Tag: `id`, `name`
  - Post: `id` (only — `publishedAt` is nullable, not currently allowed)
  - Comment: `id`, `createdAt`
- `id` is always the secondary sort (tie-breaker) for any sort.

### Implementation

- `BaseRepository<E>` extends `JpaRepository<E, Long>` + `JpaSpecificationExecutor<E>`.
- Service builds a `Specification` for the cursor WHERE clause (composable with entity-specific filters, e.g. `parent IS NULL` for top-level comments).
- `CursorSupport` (static utility) holds:
  - `validateSize`, `parseOrNull`, `build` (response wrapper)
  - `parseSort(sort, dir, allowedFields)` → `ParsedSort(field, dir, Sort)` (with `id` as secondary sort)
  - `whereAfterCursor(cursorId, cursorSortValue, sortField, dir)` → `Specification<E>` with the OR-clause
- `findAll(Specification, Pageable).getContent()` returns the entity list (an extra count query is run by `JpaSpecificationExecutor` — acceptable trade-off vs. raw `EntityManager`).

### Why cursor (not offset)?

- Stable when items are inserted/deleted between requests (offsets shift).
- Sort uses the PK index (`id`) plus one extra column — still scales.
- No "total count" query needed for the client (the count inside `JpaSpecificationExecutor` is internal and not exposed).

## Entity / DTO / Mapper Conventions

### Entity

- **`id`**: `SERIAL` / `@GeneratedValue(strategy = IDENTITY)` — internal, **never exposed** in API responses
- **`uuid`**: `UUID` field auto-generated by `@PrePersist` callback (see [UUID auto-generation](#uuid-auto-generation)) — the **public identifier**, always exposed in DTOs
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

    @PrePersist
    private void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }
}
```

### UUID auto-generation

All entities have a `@PrePersist` lifecycle callback that sets `uuid` to `UUID.randomUUID()` if not already set. UUIDs are generated by the application (not the database) so they're available in memory immediately after `save()` — no entity refresh needed.

DTOs may carry a `uuid` field in **responses**, but **never on input** — mappers use `@Mapping(target = "uuid", ignore = true)` on every `toEntity` method to drop any FE-provided uuid. FE cannot inject or override the uuid.

### Update pattern (no explicit save)

Service `update` methods rely on JPA dirty checking. The entity is loaded via `findByUuid` within a `@Transactional` boundary (managed state), mutated by the mapper, and changes are auto-flushed on transaction commit. Explicit `save()` is **not** called:

```java
@Transactional
public CategoryDTO update(UUID uuid, CategoryDTO dto) {
    Category category = categoryRepository.findByUuid(uuid).orElseThrow(...);
    categoryMapper.updateEntity(dto, category);
    return categoryMapper.toDTO(category);
    // no save() — JPA dirty checking flushes on @Transactional commit
}
```

`create` still calls `save()` (entity is new, needs `persist`/INSERT).

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
