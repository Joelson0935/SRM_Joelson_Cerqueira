# SPEC.md — SRM Credit Engine

## Visão Geral

Este documento registra as premissas adotadas, ambiguidades identificadas, decisões de precisão numérica e critérios de aceite para o **SRM Credit Engine** — plataforma de cessão de crédito multimoedas para operações de FIDC.

---

## 1. Ambiguidades Identificadas e Premissas Adotadas

### 1.1 Unidade do Prazo

**Ambiguidade:** O enunciado menciona "prazo" sem especificar se é em dias, meses ou anos.

**Premissa adotada:** O prazo é expresso em **meses inteiros** (`Integer`). A fórmula de juros compostos opera com base mensal, portanto o expoente da fórmula é o número de meses.

**Justificativa:** Os golden cases do enunciado (3 meses, 2 meses) validam essa escolha e os spreads são expressos em `% a.m.`, reforçando a base mensal.

---

### 1.2 Valor da Taxa Base

**Ambiguidade:** O enunciado não define explicitamente onde a taxa base é armazenada nem se é configurável por operação.

**Premissa adotada:** A taxa base é **1,00% a.m.** (0,01 em decimal), configurável via propriedade de aplicação (`srm.pricing.base-rate=0.01`). Ela é única e global — não varia por tipo de recebível ou por operação. Alterações na taxa base não retroagem liquidações já realizadas.

**Justificativa:** Taxas base são definidas pela política do fundo, não pelo ativo individualmente. A separação por configuração permite ajuste sem recompilação.

---

### 1.3 Taxa de Câmbio na Liquidação

**Ambiguidade:** Qual taxa de câmbio vale no momento da liquidação — a da data da operação, a mais recente disponível, ou a da data de vencimento do título?

**Premissa adotada:** Utiliza-se a **taxa de câmbio vigente no momento em que a liquidação é processada** (taxa mais recente cadastrada no sistema com `effectiveAt <= now()`). Essa taxa é **gravada imutavelmente** no registro de auditoria da liquidação, garantindo rastreabilidade independente de atualizações futuras.

**Perguntas que faríamos ao negócio antes de ir a produção:**
- A mesa de operações quer travar a taxa no momento da simulação (pré-liquidação) ou no momento da confirmação?
- Existe um SLA de atualização de taxa (ex.: taxa atualizada a cada 15 minutos via integração com provedor)?
- Liquidações cross-currency precisam de aprovação de dois operadores (4-eyes principle)?

---

### 1.4 Política de Arredondamento

**Ambiguidade:** O enunciado não especifica onde o arredondamento ocorre durante o cálculo.

**Premissa adotada:** **Half-even (Banker's Rounding), 2 casas decimais, somente no resultado final.** Todos os cálculos intermediários são realizados com precisão total (`BigDecimal` com escala mínima de 10 casas) e o arredondamento é aplicado **uma única vez** ao valor presente final, antes de qualquer conversão cambial.

**No caso cross-currency:** o valor presente em BRL é arredondado primeiro (2 casas, half-even), e então convertido para USD. O resultado em USD também é arredondado (2 casas, half-even) como etapa final separada.

**Justificativa:** Arredondamentos intermediários acumulam erro — em operações financeiras de alto volume, esse acúmulo é inaceitável. O half-even é o padrão IEEE 754 e o mais justo estatisticamente para grandes volumes de transações.

---

### 1.5 Idempotência na Liquidação

**Ambiguidade:** O enunciado pede idempotência mas não especifica o mecanismo.

**Premissa adotada:** A idempotência é garantida por dois mecanismos combinados:

1. **Chave de idempotência (`idempotency-key`) no header da requisição:** o cliente envia um UUID único por tentativa de liquidação. O servidor armazena esse key e, se a mesma requisição chegar novamente, retorna o resultado original sem reprocessar.
2. **Constraint de unicidade no banco:** o campo `status` do recebível só pode ser `SETTLED` uma vez — uma tentativa de liquidar um recebível já liquidado retorna `409 Conflict`, independente da idempotency key.

---

### 1.6 Imutabilidade dos Registros de Liquidação

**Premissa adotada:** Liquidações confirmadas são **imutáveis**. Não existe endpoint de `PUT`/`PATCH`/`DELETE` para liquidações. Eventuais correções são tratadas como estorno (nova operação de crédito) — fora do escopo deste MVP.

---

### 1.7 Tipos de Recebível

**Premissa adotada:** O sistema suporta exatamente dois tipos no MVP:

| Tipo | Enum | Spread |
|------|------|--------|
| Duplicata Mercantil | `DUPLICATA_MERCANTIL` | 1,5% a.m. |
| Cheque Pré-datado | `CHEQUE_PRE_DATADO` | 2,5% a.m. |

Novos tipos podem ser adicionados implementando a interface `PricingStrategy` sem alterar o código existente (Open/Closed Principle).

---

## 2. Decisões de Precisão Numérica

### 2.1 Tipo de Dado no Banco (PostgreSQL)

| Campo | Tipo PostgreSQL | Justificativa |
|-------|----------------|---------------|
| Valores monetários (face_value, present_value, amount) | `NUMERIC(19, 6)` | Precisão decimal exata; 6 casas para cálculos intermediários no banco; sem ponto flutuante binário |
| Taxa de câmbio | `NUMERIC(19, 8)` | Taxas cambiais exigem mais casas decimais para precisão |
| Taxa base / spread | `NUMERIC(10, 6)` | Percentuais com precisão suficiente |

**`float` e `double` são proibidos para qualquer valor monetário ou taxa** — ponto flutuante binário não representa `0.1` exatamente e acumula erro em operações financeiras.

### 2.2 Tipo de Dado na Aplicação (Java)

- Todos os valores monetários e taxas usam `java.math.BigDecimal`.
- `RoundingMode.HALF_EVEN` é o único modo de arredondamento permitido.
- Valores recebidos via API como `String` ou `Number` são convertidos para `BigDecimal` antes de qualquer operação.
- Respostas da API serializam valores monetários como `String` no JSON para evitar perda de precisão em clientes JavaScript (que usam `double` internamente).

### 2.3 Momento do Arredondamento

```
VP intermediário = face_value / (1 + base_rate + spread)^prazo  [sem arredondamento]
VP final (BRL)   = ARREDONDAR(VP intermediário, 2, HALF_EVEN)
VP final (USD)   = ARREDONDAR(VP final BRL / taxa_cambio, 2, HALF_EVEN)
```

---

## 3. Fórmula de Precificação

```
Valor Presente = Valor de Face / (1 + Taxa Base + Spread) ^ Prazo
```

Onde:
- `Valor de Face`: valor nominal do recebível em BRL
- `Taxa Base`: 1,00% a.m. = `0.01`
- `Spread`: percentual a.m. específico do tipo (Duplicata: `0.015`, Cheque: `0.025`)
- `Prazo`: número inteiro de meses

**Deságio** = Valor de Face − Valor Presente

---

## 4. Perguntas que Faríamos ao Negócio

Estas são as perguntas que, num projeto real, precisariam de resposta antes do início do desenvolvimento:

1. **Taxa base:** A taxa de 1,00% a.m. é revisada periodicamente? Com qual frequência e por qual processo?
2. **Câmbio:** A taxa de câmbio deve ser obtida de um provedor externo (ex.: Banco Central, Bloomberg) ou é sempre inserida manualmente pela mesa? Qual a tolerância para taxa "velha" (ex.: taxa com mais de 1h recusa a liquidação)?
3. **Prazo fracionado:** O sistema precisa suportar prazos em dias (ex.: 45 dias = 1,5 meses)? Se sim, qual a regra de conversão?
4. **Liquidação parcial:** Um recebível pode ser liquidado parcialmente (ex.: 60% do valor de face)?
5. **Múltiplas moedas:** Além de BRL e USD, há planos para EUR, GBP ou outras moedas no curto prazo?
6. **Conciliação:** O sistema precisa integrar com o core bancário/FIDC para conciliação automática das liquidações?
7. **Permissões:** Existem perfis de usuário distintos (operador, aprovador, auditor) ou todos têm acesso irrestrito?
8. **Reprocessamento:** Em caso de falha técnica, quem autoriza o reprocessamento de uma liquidação cancelada?

---

## 5. Critérios de Aceite

### 5.1 Corretude (não negociável)

- [ ] Os três golden cases do enunciado devem ser reproduzidos ao centavo:
  - C1: Duplicata, R$ 100.000,00, 3 meses, BRL → **R$ 92.859,94**
  - C2: Cheque, R$ 25.000,00, 2 meses, BRL → **R$ 23.337,77**
  - C3: Duplicata, R$ 100.000,00, 3 meses, USD (câmbio 5,4321) → **US$ 17.094,67**
- [ ] Nenhum valor monetário usa `float`/`double` em nenhuma camada.

### 5.2 Integridade e Segurança

- [ ] Uma liquidação não pode ficar em estado intermediário (INSERT sem UPDATE ou vice-versa) — transação ACID obrigatória.
- [ ] A mesma requisição de liquidação repetida (mesmo `idempotency-key`) retorna o mesmo resultado sem criar novo registro.
- [ ] Um recebível já liquidado (`status = SETTLED`) não pode ser liquidado novamente — retorna `409 Conflict`.
- [ ] Todos os inputs são validados antes de chegar à camada de negócio (Bean Validation + handler global).

### 5.3 Auditabilidade

- [ ] Cada liquidação registra: valor de face, valor presente, deságio, taxa base usada, spread usado, taxa de câmbio usada (se aplicável), timestamp, moeda e identificador do recebível.
- [ ] Registros de liquidação não possuem endpoint de modificação ou exclusão.

### 5.4 Usabilidade da API

- [ ] Todos os endpoints retornam códigos HTTP semânticos (200, 201, 400, 404, 409, 422, 500).
- [ ] Erros retornam corpo JSON estruturado com `code`, `message` e `timestamp`.
- [ ] API documentada via OpenAPI/Swagger acessível em `/swagger-ui`.

### 5.5 Desempenho (MVP)

- [ ] Endpoint de precificação (simulação) responde em menos de 200ms em ambiente local.
- [ ] Endpoint de extrato com paginação server-side — nunca retorna toda a tabela em memória.

---

## 6. Stack e Justificativa

| Camada | Tecnologia | Justificativa |
|--------|-----------|---------------|
| Backend | Java 21 + Spring Boot 3.x | Tipagem forte, ecossistema maduro para financeiro, suporte LTS |
| Persistência | Spring Data JPA + Hibernate | Mapeamento ORM robusto; consultas analíticas via JPQL/nativo |
| Banco de dados | PostgreSQL | ACID nativo, `NUMERIC` exato, amplamente usado em fintechs |
| Validação | Bean Validation (Jakarta) | Padrão do ecossistema Spring, declarativo e testável |
| Documentação | SpringDoc OpenAPI 3 | Geração automática do contrato da API |
| Frontend | React + Redux Toolkit | Separação clara de estado global; ecossistema maduro |
| Containerização | Docker + Docker Compose | Reprodutibilidade de ambiente |

---

*Documento vivo — premissas podem ser revisadas mediante alinhamento com o negócio, com registro de alteração via commit.*
