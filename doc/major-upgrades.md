# Major library upgrades (Spring Boot 4, Hibernate 7, Jackson 3, …)

> Plan for the major upgrades deliberately left out of the branch `deps/lib-upgrades-2026-10`
> (waves A–C: patch/minor updates, Spring Boot 3.5.16, Kotlin 2.2.21).
> Versions researched on Maven Central, as of 2026-10-05.

## Why

- Spring Boot 3.5 is the last 3.x line. Its OSS support ends in 2026 (check the date on
  [spring.io/projects/spring-boot#support](https://spring.io/projects/spring-boot#support)); after that,
  security fixes for Spring, Tomcat and Hibernate 6 ship only in the commercial edition.
- Several libraries are blocked by the 3.x stack: webauthn4j 0.31+ needs Jackson 3, Hibernate Search 8
  needs Hibernate ORM 7, and Tomcat 11 needs Spring 7.

## Target versions

The reference is the BOM of Spring Boot **4.1.1** (`spring-boot-dependencies-4.1.1.pom`). Where a newer
version exists, it is in parentheses.

| Area | Current (branch) | Target | Notes |
|---|---|---|---|
| Spring Boot | 3.5.16 | 4.1.1 | modularised auto-configuration, new packages |
| Spring Framework | 6.2.19 | 7.0.9 | JSpecify null-safety, requires Kotlin 2.2+ |
| Spring Security | 6.5.11 | 7.1.1 | lambda DSL only, `PathPatternRequestMatcher` |
| Spring Data JPA | 3.5.13 | 4.1.x (train 2026.0.1) | |
| Tomcat | 10.1.60 | 11.0.24 (11.0.26) | Jakarta EE 11, Servlet 6.1 |
| Hibernate ORM | 6.6.58 | 7.4.5 (7.4.12) | Jakarta Persistence 3.2 |
| Hibernate Search | 7.2.6 | 8.x (8.4.0) | needs ORM 7, most likely a new Lucene major → full reindex |
| Hibernate Validator | 8.0.5 | 9.1.3 (9.1.4) | Jakarta Validation 3.1 |
| jakarta.persistence-api | 3.1.0 | 3.2.0 | |
| Jackson | 2.21.7 | 3.1.5 (3.2.3), `tools.jackson.*` | Boot 4 still manages Jackson 2 (2.21.5) as deprecated fallback |
| jackson-datatype-hibernate | hibernate6 | hibernate7 (2.22.x or 3.x) | |
| webauthn4j | 0.30.3 | 0.31.x | only possible together with Jackson 3 |
| Flyway | 11.20.3 | 12.4.0 (13.9.0) | |
| HikariCP | 6.3.3 | 7.0.2 (7.1.0) | |
| JUnit Jupiter / Platform | 5.14.4 / 1.14.4 | 6.0.3 (6.1.3) | Jupiter and Platform share one version from 6.0 on |
| Kotlin | 2.2.21 | 2.3.21 (2.4.20) | **scripting engine**, see below |
| Groovy | 4.0.33 | 5.0.8 (5.1.x) | user scripts |
| Gradle | 8.14.5 | 9.x (9.8.0) | Boot 4 supports Gradle 8.14+ and 9 |
| mockito-kotlin | 5.4.0 | 6.x | |
| kotlin-logging | `io.github.oshai` 7.0.6 (develop) | – | out of scope, logging is addressed separately |
| logback | 1.5.38 | 1.5.38 (Boot 4.1); 1.6.x is optional | |
| Jackrabbit Oak | 1.92.0 | 2.x (2.6.0) | independent of Spring; storage format must be checked |

Java target stays at 17 for now: Boot 4 still requires only Java 17. If an upgrade fails because of
Java 17 (a library requiring 21, or Gradle/Kotlin tooling), switch to Java 21 as part of that phase. The
switch is already prepared in a separate branch.

## Impact on the code (inventory)

Counted in `plugins/` and `projectforge-*` (main and test, Kotlin and Java), excluding `build/`.

| Topic | Files | Hotspots |
|---|---|---|
| Jackson (`com.fasterxml.jackson.*`) | 177 | 98 databind, 103 annotations, 24 custom `Std(De)Serializer`, 34 `ObjectMapper()` instances, `JsonUtils.kt` (Hibernate6Module), `UserPrefDao`, 18 × `JsonProcessingException`, 17 × `SerializerProvider` |
| Hibernate ORM API (`org.hibernate.*`, without Search) | ~40 | `MyJpaWithExtLibrariesScanner.kt` (internal `boot.archive.scan`), `SqmNode`, `HibernateCriteriaBuilder`, `AbstractLazyInitializer`, `SingleTableEntityPersister`, `StatisticsImplementor` |
| JPA Criteria / `createQuery` | 37 | mostly JPA API, stable |
| Hibernate Search | 97 | mainly mapping annotations; bridges (`TypeBinder`, `ValueBridge`), `MassIndexer`, `LuceneAnalysisConfigurer`, `SearchPredicateFactory` |
| Spring Boot packages | ~45 | `ServerProperties`, `DataSourceBuilder`, `TomcatServletWebServerFactory`, `ErrorController`, `ServletContextInitializer`, `RestTemplateBuilder`, `EntityScan` (packages moved in Boot 4) |
| Spring Security | 2 | `SpringSecurityConfig.kt`, `GatewaySecurityConfig.kt` (already lambda DSL with `requestMatchers`) |
| RestTemplate / WebClient | 13 | sipgate, d.velop, Keycloak/Authentik, gateway push, `RestCallService` |
| Flyway API | 11 | |
| JUnit 5 | 443 | mostly annotations/assertions, compatible |
| webauthn4j | 5 | passkeys |

## Order and dependencies

```
Phase 0 (on Boot 3.5, independent, can start now)
   └─> Phase 1: Boot 4 + Spring 7 + Hibernate 7 + Tomcat 11  (still Jackson 2)
          └─> Phase 2: Jackson 3 (+ webauthn4j 0.31)
                 └─> Phase 3: remaining majors (Flyway, HikariCP, Groovy 5, Oak 2, …)
```

Boot 4, Spring 7, Spring Data 4, Hibernate 7, Hibernate Search 8 and Tomcat 11 only fit together and
must move in **one** step. Jackson 3 can be split off because Boot 4 still supports Jackson 2 (deprecated
module `spring-boot-jackson2`). This keeps the largest single change (177 files plus the JSON wire format
to next/React) out of Phase 1.

Every phase gets its own branch and is merged to `develop` separately.

## Phase 0 – Preparations on Boot 3.5

Each item can be committed and released on its own, so the risk of Phase 1 shrinks.

- [ ] **Remove deprecations** for which Hibernate 6.6, Spring 6.2 and Security 6.5 already offer the
      replacement API (compile with `-Xlint:deprecation` / Kotlin warnings, fix the warnings):
  - [x] `ClassMetadata`: the only users were the unused package `de.micromata.hibernate.history.delta`
        (incl. `PropertyDelta.java`), removed as dead code.
  - [x] `PfAbstarctScannerImpl.java` was unused and is removed. The active scanner is
        `MyJpaWithExtLibrariesScanner.kt` (`persistence.xml`, plugin entities in the fat jar); it uses the
        internal `org.hibernate.boot.archive.scan.*` classes (`ScanResultCollector`), which change in 7
        (scanning moves into the separate artifact `hibernate-scan-jandex`) → adapt in Phase 1.
  - [ ] Replace deprecated Spring/Spring Security APIs (e.g. `RestTemplate` call sites that already have a
        `RestClient` equivalent; optional, `RestTemplate` still exists in Spring 7).
- kotlin-logging is **not** part of this plan: the logging framework is addressed separately (develop
  already moved from `mu.` to `io.github.oshai` 7.0.6). It has no Spring/Hibernate dependency and doesn't
  block the phases.
- [x] **Kotlin 2.3.21** (the version Boot 4.1 manages). Run `KotlinScriptExecutionTest` **and** the fat jar
      check (see "Kotlin scripting engine"). Done; the only change: K2 2.3 marks the `=` token instead of
      the initializer for a type mismatch, the test no longer depends on the marked range.
- [ ] **JUnit 6 + mockito-kotlin 6**: collapse `org-junit-jupiter`/`org-junit-platform-launcher` into one
      catalog key. Keep the JUnit override in `projectforge-application/build.gradle.kts`
      (`extra["junit-jupiter.version"]`).
- [ ] **Gradle 9** (Boot 3.5 plugin and Kotlin 2.2/2.3 support it; check node-gradle 7.1.0 and buildSrc).
- [ ] Fix the 8 stale entries of `rest-endpoint-access-baseline.txt`, so `RestEndpointAccessCheckTest` is
      green again and stays meaningful for the Spring 7 changes to request mapping.

## Phase 1 – Spring Boot 4, Spring 7, Hibernate 7, Tomcat 11

### Build

- [ ] Catalog: Boot 4.1.x, Spring 7.0.x, Security 7.1.x, Spring Data 4.1.x, Tomcat 11.0.x, Hibernate ORM
      7.4.x, Hibernate Search 8.x, HV 9.1.x, jakarta.persistence-api 3.2.0,
      jackson-datatype-hibernate7 (Jackson 2 line).
- [ ] `projectforge-application/build.gradle.kts` pins many Boot and Spring modules by hand ("force to avoid
      downgrades"). Boot 4 splits `spring-boot-autoconfigure` into many modules (`spring-boot-webmvc`,
      `spring-boot-jdbc`, `spring-boot-hibernate`, `spring-boot-flyway`, `spring-boot-tomcat`, …) and renames
      starters (e.g. `spring-boot-starter-web` → `spring-boot-starter-webmvc`). Rebuild the list from
      `./gradlew :projectforge-application:dependencies` instead of patching it. Keep `spring-boot-starter-classic`
      in mind as a transitional starter (all auto-configurations, as in Boot 3).
- [ ] Add `spring-boot-jackson2` (Jackson 2 stays, see Phase 2).
- [ ] Check `buildSrc` (Jackson `force`, `resolutionStrategy`) and the Kotlin compiler jars of the fat jar.

### Code

- [ ] **Boot packages**: adapt the imports of `ServerProperties`, `DataSourceBuilder`,
      `TomcatServletWebServerFactory`, `ConnectorStartFailedException`, `ErrorController`,
      `ServletContextInitializer`/`ServletComponentScan`, `RestTemplateBuilder`, `EntityScan` (packages moved
      with the modularisation; see the Boot 4 migration guide).
- [ ] **Spring 7 / JSpecify**: Spring APIs are now annotated with JSpecify. Kotlin sees them as non-null or
      nullable, so overrides of Spring interfaces (filters, `HandlerInterceptor`, converters, `Condition`, …)
      may no longer compile. Fix the compile errors, don't suppress them.
- [ ] **Spring MVC**: `AntPathMatcher` path matching for controllers is gone (only `PathPattern`); check
      patterns with `**` in the middle, suffix patterns and trailing slashes. `RestEndpointAccessCheckTest`
      (Phase 0) and the e2e tests catch differences.
- [ ] **Spring Security 7**: `SpringSecurityConfig.kt` and `GatewaySecurityConfig.kt` – `requestMatchers(String)`
      now uses `PathPatternRequestMatcher`; check `StrictHttpFirewall` settings, the CSRF/session handling of
      `/rsPublic/nextLogin` and the OAuth2 client (Keycloak/Authentik).
- [ ] **Hibernate 7**:
  - [ ] removed APIs (`ClassMetadata` if not done in Phase 0, legacy `Session` methods
        `save`/`update`/`saveOrUpdate`/`delete`/`load` – currently not used with Hibernate; the `session.save()`
        hits in `projectforge-jcr` are JCR); internal APIs (`SqmNode`, `AbstractLazyInitializer`,
        `SingleTableEntityPersister`, `StatisticsImplementor`) – check each call site.
  - [ ] `merge()` of a detached entity whose row doesn't exist throws `OptimisticLockException` instead of
        inserting. Check the entities with user-assigned primary keys (Kunde, Kost2Art, …) and the
        `BaseDao` insert path.
  - [ ] Stricter HQL/SQM parsing and changed type inference: run all tests that execute HQL, especially the
        history (`de.micromata.hibernate.history`), `BaseDao` queries with `NullPrecedence`/`SortDirection`
        and the JSON columns (`@JdbcTypeCode(SqlTypes.JSON)`).
  - [ ] Schema: compare `hbm2ddl` validation/export on PostgreSQL and HSQLDB with the Flyway schema
        (no new Flyway migration should be necessary; if Hibernate 7 expects different types, decide per
        column).
- [ ] **Hibernate Search 8**: remove deprecated APIs; check `LuceneAnalysisConfigurer` (analyzers/tokenizers
      of a new Lucene major), bridges and `MassIndexer`. Plan a full reindex at the first start (index format).
- [ ] **Tomcat 11**: check `TomcatServletWebServerFactory` customisations, the connector settings and
      `ResponseHeaderFilter`.

## Phase 2 – Jackson 3

- [ ] Catalog: `tools.jackson` 3.x (core, databind, module-kotlin, dataformat-cbor/-yaml/-toml,
      datatype-hibernate7). `jackson-annotations` stays `com.fasterxml.jackson.annotation` (2.x
      artifact, compatible) – the 103 annotation-only files don't change.
- [ ] Remove `spring-boot-jackson2`; Spring's `JacksonJsonHttpMessageConverter` (Jackson 3) takes over.
- [ ] Mechanical renames: packages `com.fasterxml.jackson.{core,databind}` → `tools.jackson.{core,databind}`,
      `SerializerProvider` → `SerializationContext`, `JsonSerializer`/`JsonDeserializer` →
      `ValueSerializer`/`ValueDeserializer`, `JsonProcessingException` → `JacksonException` (unchecked),
      `BeanSerializerModifier` → `ValueSerializerModifier`, JSR-310 support is built in.
- [ ] `ObjectMapper` is immutable: 34 instantiations (incl. `JsonUtils.kt`, `UserPrefDao`) become
      `JsonMapper.builder()...build()`. Pool them in a few central mappers where possible.
- [ ] **Changed defaults** of Jackson 3 (e.g. alphabetical property order, dates as ISO strings instead of
      timestamps, stricter handling of nulls for primitives). Decide per default whether to restore the
      Jackson 2 behaviour. Affected:
  - [ ] REST contract to next and React (dates, number formats, property order in snapshots/e2e fixtures)
  - [ ] JSON stored in the database: user prefs (`UserPrefDao`), history, JSON columns – existing rows must
        still deserialise
  - [ ] XStream/JSON exports and imports
- [ ] webauthn4j 0.31.x (passkeys): API changes in the 5 files, then test registration and login.

## Phase 3 – Remaining majors (each on its own)

- [ ] Flyway 12 (13): read the release notes; the migration scripts stay, check configuration/API in the 11
      files. Test on PostgreSQL **and** HSQLDB with an existing production-like database.
- [ ] HikariCP 7.
- [ ] Groovy 5: user scripts (`ScriptDO` type GROOVY) – run the Groovy test scripts; communicate breaking
      changes to script authors.
- [ ] Jackrabbit Oak 2.x: check the storage format of the existing repositories (attachments!) and
      `RepoBackupService` backup/restore before switching.
- [ ] Optional: logback 1.6, log4j 2.26, mockserver 6+, alphanumeric-comparator 2.0, apacheds 2.0.

## Kotlin scripting engine (in every phase)

The embedded Kotlin compiler (`kotlin-compiler-embeddable`, `KotlinScriptExecutor`, `JarExtractor`) is the
most fragile part of each Kotlin or Spring change, and the class path mode differs from the fat jar mode.
The Kotlin 2.2 upgrade broke the fat jar: K2 doesn't resolve Kotlin classes from jars under a symlinked
path (see `JarExtractor.createFixedTempDirectory`). Check for each phase:

1. `./gradlew :projectforge-business:test --tests "org.projectforge.business.scripting.*"`
   (`KotlinScriptExecutionTest`: example scripts, parameters, compile errors).
2. Build the fat jar (`./gradlew :projectforge-application:bootJar`), start it on a pfDev slot and run
   `helloWorld`, `simpleExcelExport` and `advancedExcelExport` through `/rs/scriptExecute/execute`, plus a script
   with a compile error.
3. If new modules end up in the fat jar that scripts reference (e.g. split Boot modules or `tools.jackson`
   classes in the bindings), check `JarExtractor.copyJars`.

## Verification (per phase)

- `./gradlew build` (all tests) and the e2e suite (`pfDev.sh e2e <n>`).
- Fat jar smoke test on a pfDev slot (HSQLDB) and against a copy of the production PostgreSQL database:
  start incl. Flyway, login (password, passkey/WebAuthn, OAuth2/Keycloak), full-text search after reindex,
  list/edit pages of next and React, invoice PDF/ZUGFeRD (Mustang), Excel export (POI), iCal export,
  CardDAV, Kotlin and Groovy scripting, attachments (JCR/Oak), DATEV import.
- Compare the dependency tree before and after (`./gradlew :projectforge-application:dependencies
  --configuration runtimeClasspath`) for unwanted downgrades or two versions of the same library.

## Open questions

- Jackson 3 defaults: restore Jackson 2 behaviour globally (less risk) or adopt the new defaults and adapt
  the frontends?
- Window for the reindex and the Flyway run of the first Boot 4 release in production.
