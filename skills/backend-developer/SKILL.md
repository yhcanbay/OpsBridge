---
name: backend-developer
description: Build or refactor Java and Spring Boot APIs with explicit layers, DTO boundaries, mapping, persistence, and appropriate authentication. Use for backend implementation or review, not frontend-only or standalone SQL work.
---

# Backend Developer

Optimize for code a developer can follow and change, rather than few lines. Use descriptive names, explicit imports, normal formatting and one responsibility per class. Do not compress methods or hide orchestration in nested expressions.

For Spring projects, make controller, service, repository, entity, DTO, mapper, enum, configuration and exception responsibilities visible. Follow the user's requested package convention; when starting a layered application, use `controller/`, `service/`, `repository/`, `entity/`, `dto/request/`, `dto/response/`, `mapper/`, `enums/`, `config/`, `exception/`. Java's `enum` keyword cannot be a package name. Preserve an established domain layout unless restructuring is requested.

- Controllers handle HTTP and validated request DTOs; services own use cases and transaction boundaries. Do not return JPA entities through the API.
- Keep entities independent of request/response DTOs and HTTP exceptions. Keep conversions in a named mapper. For a small model, explicit mapping is easy to inspect; evaluate MapStruct when mapping volume justifies annotation processing. Avoid reflective field copying and generic base services that obscure behavior.
- Use constructor injection. Separate request and response contracts, including authentication DTOs; avoid nesting unrelated API records in controllers. Translate domain failures centrally to consistent HTTP errors.
- Inspect lazy loading, collection updates and lifecycle timestamps when moving mapping code. Do not move database work outside its transaction accidentally.
- Choose session/CSRF or JWT from the clients, trust boundaries and logout requirements. JWT is not a mandatory upgrade. When JWT is required, prefer Spring Security's supported decoder/resource-server facilities and define issuer, audience, expiry, key handling and logout behavior explicitly.
- Inspect the installed Spring Boot line before adding starters; use compatible official documentation. Do not add frameworks solely to lengthen the technology list.

Read [technology choices and sources](references/technology-guide.md) for mapping, authentication or transaction decisions. After implementation, run the repository's appropriate checks and report what was actually executed, with any remaining runtime limits.
