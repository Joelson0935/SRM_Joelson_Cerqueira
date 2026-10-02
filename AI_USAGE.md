# AI_USAGE.md — Engenharia da Colaboração com IA

> Este documento não é um log de sessão. É um registro das decisões sobre **como** a IA foi usada, onde ela errou, e o que foi deliberadamente mantido sob controle humano.

---

## 1. Specs e prompts estratégicos

A IA foi usada como parceira de implementação, não como geradora de código autônomo. O fluxo adotado foi:

**Aprovação antes de escrever:** cada passo foi apresentado como plano antes de ser implementado. A IA só escrevia código após aprovação explícita do escopo. Isso evitou retrabalho e manteve controle sobre a direção arquitetural.

**Scaffolding dirigido:** para a estrutura de pacotes (`config`, `domain`, `pricing`, `service`, `controller`, `dto`, `exception`, `repository`), a IA recebeu o contexto do desafio e as premissas do `SPEC.md` antes de sugerir qualquer estrutura. O resultado foi revisado e ajustado antes de ser aceito.

**Motor de precificação:** o prompt incluiu explicitamente as restrições de precisão (`BigDecimal`, `MathContext(20)`, `HALF_EVEN` apenas no resultado final, sem `float`/`double`) e os golden cases como critério de aceite. A IA gerou a implementação e os testes; os golden cases foram rodados para validar ao centavo antes de avançar.

**REVIEW.md:** a IA recebeu o código do Anexo A e foi instruída a ordenar os problemas por severidade de impacto em produção — não por facilidade de detecção. O resultado foi revisado para garantir que a priorização fazia sentido do ponto de vista de negócio financeiro, não apenas técnico.

---

## 2. Casos concretos em que a IA errou

### Caso 1 — Package name com underscore
O Spring Initializr gerou o pacote como `com.srm.credit_engine` (com underscore). A IA não detectou o problema de imediato — sugeriu criar arquivos dentro do pacote gerado. O processo de detecção foi visual: ao abrir o projeto no IDE, a estrutura de pastas evidenciou o underscore. A correção foi manual (renomear a pasta) e a IA acompanhou o ajuste.

**Lição:** a IA assume que o ambiente está correto. Validação do scaffolding gerado por ferramentas externas precisa ser feita pelo desenvolvedor.

### Caso 2 — Dependências de teste inválidas no pom.xml
O Spring Initializr 4.x gerou entradas como `spring-boot-starter-data-jpa-test`, `spring-boot-starter-webmvc-test` — artefatos que não existem no Maven. A IA não questionou essas dependências ao primeiro contato com o `pom.xml`; identificou o problema apenas quando solicitada a revisar o arquivo para corrigir outros pontos. O build teria falhado em tempo de download se não tivesse sido corrigido.

**Lição:** não confiar cegamente em arquivos gerados por ferramentas externas, mesmo oficiais. Revisar o `pom.xml` gerado antes de prosseguir é parte do processo.

### Caso 3 — Import ausente após edição parcial de arquivo
Ao adicionar o método `findByFilters` no `SettlementService`, a IA fez a edição via `str_replace` e adicionou os imports de `Page` e `Pageable` — mas o IDE reorganizou os imports e a edição não encontrou o trecho esperado, deixando os imports fora. O erro só apareceu no `./mvnw compile`. O processo de detecção foi o output de erro do compilador (`cannot find symbol: class Pageable`).

**Lição:** edições parciais em arquivos com imports gerenciados pelo IDE exigem verificação de compilação após cada alteração. O golden path é compilar após cada passo, não só ao final.

---

## 3. O que foi deliberadamente não delegado à IA

### Decisões de precisão numérica
A escolha de `BigDecimal` com `MathContext(20)` e `RoundingMode.HALF_EVEN` aplicado apenas ao resultado final foi tomada antes de qualquer geração de código, baseada no conhecimento do domínio financeiro e nas premissas do `SPEC.md`. A IA implementou a decisão — não a tomou. Se delegada, a IA tenderia a usar `double` ou aplicar arredondamento em etapas intermediárias, como demonstrado pelo código do Anexo A (`Math.pow` com `number`).

### Priorização do REVIEW.md
A ordenação dos problemas do Anexo A por severidade de impacto de negócio (SQL Injection > ausência de transação > ponto flutuante > idempotência) foi uma decisão de julgamento financeiro. A IA lista problemas com facilidade; priorizar pelo que causa pagamento duplo, destruição de dados ou risco regulatório exige entendimento do domínio.

### Premissas do SPEC.md sobre câmbio
A decisão de usar a taxa vigente no momento da liquidação (não a da data de vencimento) e gravá-la imutavelmente no registro foi tomada com base em raciocínio de auditoria financeira. É uma decisão que tem implicações regulatórias e contábeis — não deve ser delegada a uma IA sem validação explícita com o negócio.

### Validação dos golden cases
Os três golden cases foram rodados como testes automatizados e verificados ao centavo antes de avançar para o próximo passo. A IA gera os testes; a decisão de não avançar enquanto os valores não batem ao centavo é humana.
