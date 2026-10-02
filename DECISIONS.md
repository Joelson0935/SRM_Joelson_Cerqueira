# DECISIONS.md — Decisões, Cortes e Simplificações

> Este documento registra o que foi deliberadamente cortado ou simplificado, e por quê. Priorização é critério de avaliação — um corte bem justificado vale mais que uma feature mal feita.

---

## O que foi implementado

O escopo entregue cobre integralmente o nível **Pleno** conforme o enunciado:

- Motor de precificação com Strategy pattern, golden cases ao centavo
- Entidades JPA com ciclo de vida controlado, migration Flyway
- Endpoint de liquidação com idempotência (header `Idempotency-Key`), ACID e auditoria completa
- Currency Engine com vigência temporal de taxas
- Extrato com filtros combinados e paginação server-side
- Simulação em tempo real (pré-liquidação)
- Tratamento de erros global com HTTP semântico
- Docker + Docker Compose com healthcheck
- Testes unitários das strategies e do serviço de liquidação (15 testes)
- SPEC.md, REVIEW.md, AI_USAGE.md

---

## O que foi cortado e por quê

### 1. Testes de integração com banco real

**O que seria:** testes com `@SpringBootTest` + banco PostgreSQL real (ou Testcontainers) validando o fluxo end-to-end de liquidação, incluindo constraints de unicidade e comportamento do Flyway.

**Por que foi cortado:** Testcontainers exige Docker disponível no ambiente de CI e adiciona complexidade de setup que ultrapassaria o esforço alvo de 8–16h para o nível pleno. Os testes unitários com Mockito cobrem toda a lógica de negócio incluindo os golden cases. A integração com banco é validada manualmente via `./mvnw spring-boot:run` e Swagger UI.

**Risco:** falhas de integração específicas do PostgreSQL (ex: comportamento de `NUMERIC` vs `DECIMAL` em edge cases) não são cobertas automaticamente.

---

### 2. Autenticação e autorização

**O que seria:** Spring Security com JWT ou session-based auth, perfis de usuário (operador, auditor), proteção dos endpoints.

**Por que foi cortado:** o enunciado não especifica requisitos de autenticação para o MVP, e a pergunta sobre perfis de usuário foi explicitamente listada como "pergunta que faríamos ao negócio" no `SPEC.md`. Adicionar auth sem requisitos claros gera complexidade sem valor verificável na defesa.

**Risco:** em produção real, todos os endpoints precisariam de autenticação. Este é o item de maior risco de segurança do MVP.

---

### 3. Observabilidade (logs estruturados + métricas)

**O que seria:** logs estruturados em JSON com `logback` + MDC (correlation ID por request), métricas de negócio via Micrometer (liquidações/min, latência do motor de precificação).

**Por que foi cortado:** é requisito do nível Sênior, não Pleno. O `GlobalExceptionHandler` já usa `slf4j` para logar erros com contexto adequado. Adicionar Micrometer + Prometheus para o nível pleno seria over-engineering do escopo.

---

### 4. Optimistic locking na liquidação

**O que seria:** `@Version` na entidade `Receivable` + teste demonstrando conflito de duas liquidações simultâneas sendo tratado.

**Por que foi cortado:** é requisito do nível Sênior ("Concorrência"). Para o nível Pleno, a combinação de `idempotency_key` único + `uq_settlements_receivable_id` no banco garante que liquidações duplicadas são rejeitadas pelo PostgreSQL mesmo em race conditions, sem necessidade de optimistic locking explícito. A proteção existe — a camada de locking otimista seria defesa em profundidade adicional.

---

### 5. CI/CD (GitHub Actions)

**O que seria:** pipeline rodando `./mvnw test` + linter em cada push/PR.

**Por que foi cortado:** é requisito do nível Sênior. Para o nível Pleno, o foco foi na qualidade do código e nos testes locais. O pipeline pode ser adicionado com um arquivo `.github/workflows/ci.yml` simples após a entrega.

---

### 6. Diagrama C4

**O que seria:** diagramas C4 nível 1 (contexto) e nível 2 (containers) documentando a arquitetura.

**Por que foi cortado:** é requisito do nível Sênior. A arquitetura do sistema é suficientemente descrita pelo README e pelo `SPEC.md` para o nível Pleno.

---

## Decisões de design que merecem destaque

### Por que Flyway em vez de `ddl-auto: create`?

`ddl-auto: create` ou `update` é conveniente em desenvolvimento mas perigoso em qualquer ambiente compartilhado — o Hibernate pode dropar tabelas ou aplicar migrations inconsistentes. Flyway garante migrations versionadas, auditáveis e repetíveis. Em ambiente financeiro, rastreabilidade de mudanças no schema é obrigatória.

### Por que `NUMERIC(19,6)` e não `DECIMAL(19,2)`?

Valores monetários são armazenados com 6 casas decimais para preservar precisão em cálculos intermediários que possam ocorrer diretamente no banco (ex: relatórios SQL). O arredondamento para 2 casas é responsabilidade da camada de aplicação — gravado apenas no resultado final conforme o `SPEC.md`. `DECIMAL` e `NUMERIC` são equivalentes no PostgreSQL; usamos `NUMERIC` por convenção.

### Por que Strategy pattern para o motor de precificação?

O enunciado pede explicitamente o padrão Strategy, e faz sentido: cada tipo de recebível tem um spread próprio. Adicionar um novo tipo (`NOTA_PROMISSORIA`, por exemplo) exige apenas criar um novo `@Component` que implementa `PricingStrategy` — sem alterar código existente (Open/Closed Principle). O `PricingStrategyFactory` usa injeção automática do Spring para descobrir todas as strategies disponíveis.

### Por que a idempotency key fica no header e não no body?

Seguindo o padrão de APIs financeiras (Stripe, Adyen, PagSeguro): a idempotency key é um mecanismo de transporte da requisição, não um dado de negócio. Colocá-la no header separa a semântica do negócio (o que liquidar) da semântica de transporte (como garantir que só liquide uma vez). Isso também facilita o tratamento no `GlobalExceptionHandler` via `MissingRequestHeaderException`.
