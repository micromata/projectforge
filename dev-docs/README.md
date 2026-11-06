# Developer documents

Working documents for developers: reference notes and plans that are not for users or administrators.
The documentation for users and administrators is on the website (`site/`, https://projectforge.org).

| Document                                              | Kind      | Status                                                                                |
| ----------------------------------------------------- | --------- | ------------------------------------------------------------------------------------- |
| [next/](next/README.md)                               | Reference | Architecture, page concept, list paging and open points of the Next.js frontend       |
| [deployment.adoc](deployment.adoc)                    | Reference | Release and publish process (`bin/pfDev.sh release`, `publish`)                       |
| [HOWTO-TEST-GATEWAY.md](HOWTO-TEST-GATEWAY.md)        | Reference | Testing the gateway instance locally and on a server                                  |
| [lanes-and-planes.md](lanes-and-planes.md)            | Reference | Lanes & Planes ExtAPI findings and checklist before the first real push               |
| [history-n-plus-1.md](history-n-plus-1.md)            | Done      | History re-index removal, `pg_trgm` indexes, batched history loading; few open points |
| [notification-rules.md](notification-rules.md)        | Reference | Adding a rule type of the notification system (handler, params, texts, tests)          |
| [file-store.md](file-store.md)                        | Reference | File store (`projectforge-files`), start-up guard for the former JCR; few open points |
| [major-upgrades.md](major-upgrades.md)                | Plan      | Spring Boot 4 / Hibernate 7 done; Jackson 3 and the remaining majors open             |
| [kost2-reallocation.md](kost2-reallocation.md)        | Plan      | Retroactive Kost2 reallocation of accounting records; not started                     |
| [invoice-from-order.md](invoice-from-order.md)        | Plan      | "Create invoice" from an order (positions, order number prefilled); waiting for finance |
| [backup-encryption.md](backup-encryption.md)          | Proposal  | Encrypting backups with `age`; not implemented                                        |
| [digital-business-card.md](digital-business-card.md)  | Plan      | Digital business cards: QR/vCard and photo (token url) done; Apple Wallet pass open       |
| [app-api-deprecations.md](app-api-deprecations.md)    | Reference | Fields and endpoints the published app still reads, kept for it; when they can go     |
| [flutter-client.md](flutter-client.md)                | Plan      | Mobile app (Flutter): app login via browser + PKCE, business cards done; timesheets, chat, push open |
| [reviews/pr-391-mcp-server.md](reviews/pr-391-mcp-server.md) | Review | PR #391 remote MCP server: findings, open                                       |
| [cve-radar-db-reset.md](cve-radar-db-reset.md)        | Temporary | CVE-Radar: drop the plugin tables, restore the imported CVEs; remove once released    |
| [requirements/](requirements/)                        | Plans     | Specifications: chat (in implementation), CVE radar (in implementation)               |
| [requirements/PF-CVR_Dependency-Track_Evaluation.md](requirements/PF-CVR_Dependency-Track_Evaluation.md) | Proposal | CVE radar: Dependency-Track as detection engine, PF keeps assets and workflow; not decided |
| [website-screenshots/](website-screenshots/README.md) | Tooling   | Scripts that shoot the website screenshots                                            |
| [misc/](misc/)                                        | Assets    | nginx example configuration, forecast probabilities fixture                           |
