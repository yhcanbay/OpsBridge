# PostgreSQL / JPA / Flyway decisions

Research checked: 2026-11-04. Consult the deployment's PostgreSQL major, not just the documentation's newest release.

| Concern | Decision criteria |
|---|---|
| Migrations | Add `Vn__description.sql`; retain previously applied migration checksums |
| Null and empty | `NOT NULL` rejects null; add a named `CHECK` if blank text is also invalid |
| Enumerations | String-backed JPA enum plus an appropriate database check; coordinate changes |
| Relationships | Foreign key for ownership, justified cascade behavior, lazy relationships with deliberate fetching |
| Indexes | Existing primary/unique indexes first; new indexes must serve demonstrated query patterns |
| ORM schema | Migration owns schema; ORM validation checks compatibility rather than mutating production schema |
| Verification | Fast isolated tests plus the target engine where available; record actual executed database version |

Primary sources:

- [PostgreSQL 16 constraints](https://www.postgresql.org/docs/16/ddl-constraints.html): nullability, CHECK, keys and references.
- [PostgreSQL 16 indexes](https://www.postgresql.org/docs/16/indexes-intro.html): query acceleration and index costs.
- [Flyway versioned migrations](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/versioned-migrations): ordered application, checksums and forward changes.
- [Spring Data JPA transactions](https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html) and [entity graphs](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.entity-graph): transaction and fetch boundaries.

For small datasets, do not label an index or cache as a performance improvement without measurement. A refactor should not silently make schema generation, seed resurrection or destructive resets part of startup.
