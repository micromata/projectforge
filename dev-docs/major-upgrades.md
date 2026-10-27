# Major library upgrades (Spring Boot 4, Hibernate 7, Jackson 3, …)

> Plan for the major upgrades. Phase 0 and Phase 1 (Spring Boot 4, Spring 7, Hibernate 7, Tomcat 11) are
> done and merged to `develop`; Phase 2 (Jackson 3) and Phase 3 (remaining majors) are open.
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

| Area | Current (`develop`) | Target | Notes |
|---|---|---|---|
| Spring Boot | 4.1.1 | 4.1.1 | modularised auto-configuration, new packages |
| Spring Framework | 7.0.9 | 7.0.9 | JSpecify null-safety, requires Kotlin 2.2+ |
| Spring Security | 7.1.1 | 7.1.1 | lambda DSL only, `PathPatternRequestMatcher` |
| Spring Data JPA | 4.1.1 | 4.1.x (train 2026.0.1) | |
| Tomcat | 11.0.26 | 11.0.24 (11.0.26) | Jakarta EE 11, Servlet 6.1 |
| Hibernate ORM | 7.4.12 | 7.4.5 (7.4.12) | Jakarta Persistence 3.2 |
| Hibernate Search | 8.4.0 | 8.x (8.4.0) | needs ORM 7; Lucene 9.11 → 9.12 (no new major), old indexes need `lucene-backward-codecs` |
| Hibernate Validator | 9.1.4 | 9.1.3 (9.1.4) | Jakarta Validation 3.1 |
| jakarta.persistence-api | 3.2.0 | 3.2.0 | |
| Jackson | 2.21.7 | 3.1.5 (3.2.3), `tools.jackson.*` | Boot 4 still manages Jackson 2 (2.21.5) as deprecated fallback |
| jackson-datatype-hibernate | hibernate7 2.21.7 | hibernate7 (2.22.x or 3.x) | |
| webauthn4j | 0.30.3 | 0.31.x | only possible together with Jackson 3 |
| Flyway | 11.20.3 | 12.4.0 (13.9.0) | 11 works with the Flyway auto-configuration of Boot 4.1 (Phase 3) |
| HikariCP | 6.3.3 | 7.0.2 (7.1.0) | |
| JUnit Jupiter / Platform | 6.0.3 | 6.0.3 (6.1.3) | Jupiter and Platform share one version from 6.0 on |
| Kotlin | 2.3.21 | 2.3.21 (2.4.20) | **scripting engine**, see below |
| Groovy | 4.0.33 | 5.0.8 (5.1.x) | user scripts, mail and PDF templates (core + datetime, dateutil, json, templates, xml; no `groovy-all`); Boot 4 manages 5.0.8, pinned to 4 via `extra["groovy.version"]` until Phase 3 |
| Gradle | 9.8.0 | 9.x (9.8.0) | Boot 4 supports Gradle 8.14+ and 9 |
| mockito-kotlin | 6.4.0 | 6.x | |
| kotlin-logging | `io.github.oshai` 7.0.6 (develop) | – | out of scope, logging is addressed separately |
| logback | 1.5.38 | 1.5.38 (Boot 4.1); 1.6.x is optional | |
| Jackrabbit Oak | 1.92.0 | 2.x (2.6.0) | independent of Spring; storage format must be checked |

Java target stays at 17 for now: Boot 4 still requires only Java 17. If an upgrade fails because of
Java 17 (a library requiring 21, or Gradle/Kotlin tooling), switch to Java 21 as part of that phase. The
switch is prepared in a separate branch.

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

## Phase 0 and 1 – done

Phase 0 (preparations on Boot 3.5: Kotlin 2.3, JUnit 6, Gradle 9, dead code removed) and Phase 1 are on
`develop`; the details are in the git history. What is still worth knowing:

- Open leftover of Phase 0 (optional): replace `RestTemplate` call sites that have a `RestClient` equivalent
  (`RestTemplate` still exists in Spring 7).
- The hand-pinned Boot module list in `projectforge-application/build.gradle.kts` needs `spring-boot-flyway`
  (without it the migrations silently do **not** run), `spring-boot-security` and
  `spring-boot-security-oauth2-client`, `spring-boot-restclient`, `spring-boot-jdbc`, `spring-boot-web-server`.
- Jackson 2 stays until Phase 2: `spring-boot-jackson2` instead of `spring-boot-starter-json`;
  `spring-boot-starter-jackson` (Jackson 3) is excluded in `buildSrc` and the application, so the fat jar has
  no `tools.jackson`.
- `lucene-backward-codecs` (same version as Hibernate Search's Lucene) is required: existing indexes are read
  without a reindex, but an index written by Phase 1 cannot be opened by a Boot 3 release. A rollback needs
  an index backup or a reindex. Take a database and index backup before the first start.
- `tomcat-embed-el` is needed by Hibernate Validator 9.
- The Boot 4 BOM manages Groovy 5; Groovy stays on 4 via `extra["groovy.version"]` until Phase 3.
- Not caused by the upgrade: Hibernate 7 derives `NOT NULL` for primitive booleans (`deleted`); the schema
  comes from Flyway, so nothing changes in existing databases.
- The gateway answers blocked paths with 403 (Spring Security denies them before `GatewayEndpointFilter`
  would answer 404); `dev-docs/HOWTO-TEST-GATEWAY.md` is corrected. OAuth2 login (Keycloak/Authentik) needs the
  real IdP and is checked live after a release.
- A full reindex takes about a minute on a database with tens of thousands of invoices.

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
- [ ] Groovy 5: drop the `extra["groovy.version"]` pin in the application build; user scripts (`ScriptDO` type GROOVY) – run the Groovy test scripts; communicate breaking
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
- Fat jar smoke test on a pfDev slot (HSQLDB) and against a production-like PostgreSQL database:
  start incl. Flyway, login (password, passkey/WebAuthn, OAuth2/Keycloak), full-text search (old and new entries),
  list/edit pages of next and React, invoice PDF/ZUGFeRD (Mustang), Excel export (POI), iCal export,
  CardDAV, Kotlin and Groovy scripting, attachments (JCR/Oak), DATEV import.
- Compare the dependency tree before and after (`./gradlew :projectforge-application:dependencies
  --configuration runtimeClasspath`) for unwanted downgrades or two versions of the same library.

## Open questions

- Jackson 3 defaults: restore Jackson 2 behaviour globally (less risk) or adopt the new defaults and adapt
  the frontends?
- Window for the Flyway run of the first Boot 4 release in production (and optionally a reindex to drop
  the old Lucene segments: a full reindex took about a minute on the test database, see step 4 of "Completion of Phase 1").
- Rollback strategy after the Boot 4 release: keep the index backup, or accept a reindex on rollback?
