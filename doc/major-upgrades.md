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
| Groovy | 4.0.33 | 5.0.8 (5.1.x) | user scripts; Boot 4 manages 5.0.8, pinned to 4 via `extra["groovy.version"]` until Phase 3 |
| Gradle | 9.8.0 | 9.x (9.8.0) | Boot 4 supports Gradle 8.14+ and 9 |
| mockito-kotlin | 6.4.0 | 6.x | |
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
- [x] **JUnit 6 + mockito-kotlin 6**: collapse `org-junit-jupiter`/`org-junit-platform-launcher` into one
      catalog key. Keep the JUnit override in `projectforge-application/build.gradle.kts`
      (`extra["junit-jupiter.version"]`). Done with JUnit 6.0.3 (the version of the Boot 4.1 BOM) and
      mockito-kotlin 6.4.0; no code changes needed.
- [x] **Gradle 9** (Boot 3.5 plugin and Kotlin 2.2/2.3 support it; check node-gradle 7.1.0 and buildSrc).
      Done with 9.8.0. Gradle 9 sorts archive entries by default; Boot's sorting copy action then read the
      `zipTree` of the extracted Kotlin compiler jars after Gradle had closed them (`ClosedChannelException`
      in `bootJar`), so `bootJar` keeps the unsorted order. Left over: a plugin calls
      `Configuration.setVisible` (removed in Gradle 11).
- [x] Fix the 8 stale entries of `rest-endpoint-access-baseline.txt`, so `RestEndpointAccessCheckTest` is
      green again and stays meaningful for the Spring 7 changes to request mapping. Green since the
      develop merge of 2026-10-05, nothing to do here.

## Phase 1 – Spring Boot 4, Spring 7, Hibernate 7, Tomcat 11

All tests green (1516, 1 skipped), fat jar smoke test passed on HSQLDB (pfDev slot 7), e2e suite run (5 failures,
none caused by the upgrade, see "Completion of Phase 1"). What is left is listed there.

### Build

- [x] Catalog: Boot 4.1.1, Spring 7.0.9, Security 7.1.1, Spring Data 4.1.1, Tomcat 11.0.26, Hibernate ORM
      7.4.12, Hibernate Search 8.4.0, HV 9.1.4, jakarta.persistence-api 3.2.0, jakarta.validation-api 3.1.1,
      jackson-datatype-hibernate7 2.21.7 (Jackson 2 line). The `spring-jcl` pin is gone (removed in Spring 7).
- [x] Boot 4 modules: the hand-pinned list in `projectforge-application/build.gradle.kts` was kept and
      extended instead of rebuilt. Needed beyond the starters (`spring-boot-starter-web` →
      `spring-boot-starter-webmvc`):
  - `spring-boot-flyway`: Flyway runs through Boot's auto-configuration (`spring.flyway.*`); without the
    module the migrations would silently **not** run.
  - `spring-boot-security` and `spring-boot-security-oauth2-client`: without them there is no `HttpSecurity`
    bean for `SpringSecurityConfig`/`GatewaySecurityConfig` and no OAuth2 client registration from
    `spring.security.oauth2.client.*`.
  - `spring-boot-restclient` (`RestTemplateBuilder`), `spring-boot-jdbc` (`DataSourceBuilder`, projectforge-jcr),
    `spring-boot-web-server` (`ServerProperties`, projectforge-business).
- [x] Jackson 2 stays: `spring-boot-jackson2` instead of `spring-boot-starter-json`. The Boot 4 starters pull in
      `spring-boot-starter-jackson` (Jackson 3); it is excluded in `buildSrc` and in the application, so the fat
      jar contains no `tools.jackson` jar and Spring MVC keeps the Jackson 2 converter.
- [x] `lucene-backward-codecs` (version of `org-apache-lucene`, must match Hibernate Search's Lucene): Lucene
      9.12 moved the `Lucene99` codec of the indexes written by Hibernate Search 7 into that jar. Without it,
      indexing fails ("Could not load codec 'Lucene99'"). With it, existing indexes are read and new segments
      are written in the new format, so **no reindex is required** (an optional reindex removes the old
      segments). Not the other way round: Phase 0 can't open an index Phase 1 has written to
      ("HSEARCH000284: Unable to open index readers", saving then fails with "Cannot rollback transaction in
      current status [COMMITTED]"). A rollback to a Boot 3 release needs a backup of the index directory or a
      reindex.
- [x] `tomcat-embed-el` in projectforge-business: Hibernate Validator 9 needs an EL implementation, which no
      longer comes transitively ("HV000183 … jakarta.el.ExpressionFactory").
- [x] Compare the dependency tree with Phase 0 (see "Verification"): no downgrades, no two versions of one
      artifact, no `tools.jackson`. One finding: the Boot 4 BOM manages **Groovy 5.0.8**, so every Groovy module
      except the pinned `groovy`/`groovy-all`/`groovy-ant` (json, xml, sql, templates, …, plus jline 3 and jna
      via `groovy-groovysh`) came as 5.0.8 next to the Groovy 4 core, also in the fat jar. Fixed like the JUnit
      override: `extra["groovy.version"]` in `projectforge-application/build.gradle.kts`. Groovy 5 stays in
      Phase 3. All other additions are the split Boot modules, Hibernate 7 artifacts (`hibernate-models`,
      `jackson-datatype-hibernate7`), `jspecify`, `lucene-backward-codecs` and Netty modules of reactor-netty.

### Code

- [x] **Boot packages**: `ServerProperties` → `org.springframework.boot.web.server.autoconfigure` (the session
      cookie settings are unchanged), `EntityScan` → `boot.persistence.autoconfigure`, `RestTemplateBuilder` →
      `boot.restclient`, `ErrorController` → `boot.webmvc.error`, `TomcatServletWebServerFactory` →
      `boot.tomcat.servlet`, `ConnectorStartFailedException` → `boot.tomcat`, `ServletComponentScan` →
      `boot.web.server.servlet.context`. `DataSourceBuilder`, `ServletContextInitializer`,
      `WebServerFactoryCustomizer`, `ConditionalOnProperty` and `ConfigurationProperties` kept their packages.
- [x] **Spring 7 / JSpecify**: few compile errors. `ResponseEntity<T>` needs a non-null `T`
      (`TwoFactorLoginNextRest`, `PasswordResetNextRest`), `exchangeToMono`/`block()` of the sipgate and d.velop
      clients: `execute` now returns `T?` (responses with `NO_CONTENT` really have no body), the callers
      handle `null`.
- [x] **Spring MVC**: `RestEndpointAccessCheckTest` is green. e2e suite against the fat jar (slot 7): 284 passed,
      16 skipped, 5 failed; none of the 5 is caused by the upgrade, see "Completion of Phase 1".
- [ ] **Spring Security 7**: both configs compile unchanged and the password login of next works
      (`/rsPublic/nextLogin`, CSRF, session cookie). Still to test: gateway mode, OAuth2 (Keycloak/Authentik),
      WebDAV/CardDAV methods through `StrictHttpFirewall`.
- [x] **Hibernate 7**:
  - [x] `ScanResultCollector` is gone: `MyJpaWithExtLibrariesScanner` returns an empty `ScanResult`, which is
        what the collector returned before (no archive was visited; entities come from the explicitly listed
        class names). `hibernate-scan-jandex` isn't needed.
  - [x] `SqmNode` was only imported by mistake (`SqmNode.log` in two files), replaced by kotlin-logging.
        No other removed API was used.
  - [x] `merge()` with user-assigned primary keys: inserting and updating a customer (`KundeDO`, number typed
        by the user) and a cost type 2 (`Kost2ArtDO`) through `/rs/<category>/saveorupdate` works in the fat jar.
  - [x] HQL/SQM: all tests green, including history and `BaseDao` queries.
  - [x] Schema: the `hbm2ddl` export of Phase 0 and Phase 1 is the same on HSQLDB and PostgreSQL apart from
        `not null` on `deleted` and extra parentheses, see "Completion of Phase 1".
- [x] **Hibernate Search 8**: `BooleanPredicateOptionsCollector` has two type parameters (`<*, *>` in
      `DBPredicate`). Analyzers, bridges and `MassIndexer` unchanged. The global search finds old (Lucene99) and
      new entries after the upgrade. The warnings "Search property … declared as additional field" existed
      before.
- [x] **Tomcat 11**: `TomcatConfig` (`maxPartCount`) only needed the new import; server starts.
      `ResponseHeaderFilter` unchanged.

### Completion of Phase 1

Remaining steps, in this order. Each finding gets its own commit on `deps/major-upgrades-phase1`.

1. **Clear the e2e failures** (done). Baseline: the 5 specs on Phase 0 (`cf319bffc`, temp worktree `/tmp/pf-baseline`,
   slot 8 with a copy of the slot 7 database, :8088).
   - Not caused by the upgrade (fail on Phase 0 with the same data):
     `creditor-invoice-selection` "takes the arrow keys without a click first",
     `invoice-edit` "reports an invoice left without positions",
     `list-page-memory` "returns to the page, the offset and the entry",
     `table-loading` "a server-laid-out list page" (the slot database has no vacation entries).
     Report them separately; they don't block Phase 1.
   - `invoice-selection` "takes the arrow keys without a click first" (passed on Phase 0, failed on Phase 1)
     is not caused by the upgrade either: the spec ticks the first row and extends the range by two rows,
     so it needs at least 3 invoices in the list. The slot test data has 2 (numbers 1000 and 1001). On
     Phase 0 the list had 10, because the disturbed `invoice-edit` run there (copied Phase 1 index) left
     8 drafts `ZZ e2e invoice (delete me)` behind in the slot 8 database (ids 12451–12458).
     `/rs/outgoingInvoice/listPage` with a reset filter, after a fresh reindex of slot 8: 10 rows on
     :8088, 2 on :8087. Fix in the spec (skip below 3 rows, or extend by only one row), reported
     separately.
2. **Spring Security 7**: gateway mode (`GatewaySecurityConfig`), OAuth2 login against Keycloak/Authentik,
   CardDAV with a real client (PROPFIND/REPORT through `StrictHttpFirewall`), WebDAV of the attachments.
3. **Schema compare** (done). `-Dhibernate.hbm2ddl.auto=validate` (ProjectForge's own key, see `JpaConfig`) is
   no help: Phase 0 and Phase 1 both stop at the first, old mismatch (`T_ADDRESS.pk` is `integer` in the
   Flyway schema, the `Long` id expects `bigint`). Instead the DDL of both versions was exported
   (`-Djakarta.persistence.schema-generation.scripts.action=create`, `...scripts.create-target=<file>`, no
   database action) on HSQLDB and with `-Dhibernate.dialect=org.hibernate.dialect.PostgreSQLDialect`, and
   compared column by column: 291 statements and 1410 columns on both sides, no changed type (`@Lob`,
   `Duration`, enums, `float` unchanged). Only two differences, neither of them relevant at runtime:
   - 64 × `deleted boolean` → `deleted boolean not null`: Hibernate 7 derives NOT NULL from the primitive
     type (Kotlin `Boolean`). The schema comes from Flyway, and `update` doesn't tighten existing columns.
   - Check constraints of enums get an extra pair of parentheses.
4. **Copy of the production PostgreSQL database**: start incl. Flyway, global search on the old index, then a full
   reindex (measure the duration for the release window), and the remaining items of "Verification": password
   and passkey login (webauthn4j 0.30, still Jackson 2), list/edit pages of next and React, invoice
   PDF/ZUGFeRD, Excel export, iCal export, attachments (JCR/Oak), DATEV import, Kotlin and Groovy scripts.
5. **Release notes / operations**: no reindex needed on upgrade; a rollback needs the index backup or a reindex;
   take a database and index backup before the first start.
6. Merge `develop` once more, full `./gradlew build` and e2e, then merge to `develop`.

Clean-up afterwards: `git worktree remove /tmp/pf-baseline`, delete `~/ProjectForge-8`, stop the servers on
:8087/:8088 (smoke-test data in slot 7: customer 987, Kost2Art 97).

After that, Phase 2 (Jackson 3 + webauthn4j 0.31) on its own branch from `develop`, then Phase 3.

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
- Fat jar smoke test on a pfDev slot (HSQLDB) and against a copy of the production PostgreSQL database:
  start incl. Flyway, login (password, passkey/WebAuthn, OAuth2/Keycloak), full-text search (old and new entries),
  list/edit pages of next and React, invoice PDF/ZUGFeRD (Mustang), Excel export (POI), iCal export,
  CardDAV, Kotlin and Groovy scripting, attachments (JCR/Oak), DATEV import.
- Compare the dependency tree before and after (`./gradlew :projectforge-application:dependencies
  --configuration runtimeClasspath`) for unwanted downgrades or two versions of the same library.

## Open questions

- Jackson 3 defaults: restore Jackson 2 behaviour globally (less risk) or adopt the new defaults and adapt
  the frontends?
- Window for the Flyway run of the first Boot 4 release in production (and optionally a reindex to drop
  the old Lucene segments; duration from step 4 of "Completion of Phase 1").
- Rollback strategy after the Boot 4 release: keep the index backup, or accept a reindex on rollback?
