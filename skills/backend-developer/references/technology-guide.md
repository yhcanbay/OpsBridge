# Java / Spring decisions

Research checked: 2026-11-04. Verify current versions when making a new dependency decision.

| Concern | Default reasoning | Reconsider when |
|---|---|---|
| Packages | Explicit layers when the user requests them; domain organization also exists | Existing repository or module boundaries favor a different layout |
| API data | Validated request DTO, response DTO, explicit mapper | A genuinely separate read model needs a projection |
| Mapper | Manual, named mapper for a small contract | Many mappings make MapStruct's generated, compile-time checked mapping useful |
| Transactions | Service owns the use case; map lazy relationships while the transaction is open | Asynchronous work requires a separately designed boundary |
| Authentication | Browser session with CSRF for a same-origin admin panel | Independent API clients or an identity provider require bearer tokens |
| JWT | Spring Security resource-server support for validation | Token issuance/refresh requires a separate explicit design |

These are engineering choices, not claims that a framework mandates one architecture. Do not create an interface for every service without a substitution boundary. Do not use Lombok on entities merely to suppress readable code; generated equality and serialization need particular care.

Primary sources:

- [Spring Boot code structure](https://docs.spring.io/spring-boot/4.0/reference/using/structuring-your-code.html): root application package and supported organization examples; no universally required package layout.
- [Spring Data JPA transactions](https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html): service-level transaction boundaries.
- [MapStruct stable guide](https://mapstruct.org/documentation/stable/reference/html/): generated mapping, Spring component model and unmapped target checks.
- [Spring Security CSRF](https://docs.spring.io/spring-security/reference/7.0/servlet/exploits/csrf.html): browser mutation requests and token renewal after authentication/logout.
- [Spring Security JWT](https://docs.spring.io/spring-security/reference/7.0/servlet/oauth2/resource-server/jwt.html): bearer token validation and issuer configuration.
