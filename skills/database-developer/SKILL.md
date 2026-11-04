---
name: database-developer
description: Design or change relational schemas, PostgreSQL queries, JPA persistence boundaries, and Flyway migrations. Use for database implementation or review, not visual-only frontend changes.
---

# Database Developer

Make schema intent and data ownership explicit. Prefer readable SQL with named constraints and migration filenames that describe the change. Inspect the actual schema, migrations, query paths and database version before modifying them.

- Keep persistent integrity rules in the database as well as API validation: nullability, foreign keys, uniqueness when it is a real domain rule, and bounded/check values. Do not invent a uniqueness rule from a UI example.
- Use versioned migrations. Once a migration may have been applied, add a new migration instead of rewriting its history. Keep development examples and production data policy distinguishable.
- Model Java enums using stable string representations; plan a coordinated API/entity/database migration when values change. Do not persist enum ordinals.
- Put transaction boundaries around a complete use case. Inspect lazy loading, N+1 queries, collection ownership and flush-dependent timestamps when refactoring JPA. Do not solve every relationship with eager fetching.
- Add indexes based on actual predicates, joins and ordering; explain the query served. Account for indexes already provided by keys. Use query plans with representative data when performance is the task.
- Use bound parameters. Keep secrets in environment/secret configuration and out of migration data, source examples and logs.
- Test relevant constraints and migrations against the actual database engine when available. H2 compatibility mode is useful for fast checks but does not establish PostgreSQL behavior. Report version differences and missing checks explicitly.

Read [technology choices and sources](references/technology-guide.md) for constraint, index or migration decisions. Keep changes within the authorized data scope and preserve existing records during structural refactors.
