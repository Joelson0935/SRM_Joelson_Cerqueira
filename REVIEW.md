# REVIEW.md — Code Review Reverso (Fase 2)

> Contexto: endpoint de liquidação "gerado por IA e mergeado às pressas numa sexta-feira".
> Revisão realizada como se fosse um PR em produção, ordenada por severidade de impacto.

---

## Código revisado

```typescript
// settlement.controller.ts
const BASE_RATE = 1.0; // taxa base mensal

app.post("/settlements", async (req: Request, res: Response) => {
  const { receivableId, currency } = req.body;

  const receivable = await db.queryOne(
    `SELECT * FROM receivables WHERE id = ${receivableId}`
  );

  const spread = receivable.type === "DUPLICATA" ? 1.5 : 2.5;
  const presentValue =
    receivable.face_value / Math.pow(1 + BASE_RATE + spread, receivable.term);

  let finalAmount = presentValue;
  if (currency === "USD") {
    const rate = await fxService.getLatestRate("USD");
    finalAmount = presentValue / rate;
  }

  try {
    await db.query(
      `INSERT INTO settlements (receivable_id, amount, currency)
       VALUES (${receivableId}, ${finalAmount.toFixed(2)}, '${currency}')`
    );
    await db.query(
      `UPDATE receivables SET status = 'SETTLED' WHERE id = ${receivableId}`
    );
  } catch (e) {
    // se falhar aqui, o insert já rodou, então segue o jogo
  }

  res.status(200).json({ ok: true, amount: finalAmount.toFixed(2) });
});
```

---

## Problemas em ordem de severidade

---

### 🔴 P1 — SQL Injection (Crítico — Segurança)

**Onde:** interpolação direta de `receivableId` e `currency` nas queries.

```typescript
`SELECT * FROM receivables WHERE id = ${receivableId}`
`VALUES (${receivableId}, ${finalAmount.toFixed(2)}, '${currency}')`
```

**Impacto em produção:**
Qualquer cliente que controle o body da requisição pode executar SQL arbitrário no banco de produção. Um payload como `receivableId = "1; DROP TABLE settlements--"` apaga toda a tabela de liquidações. Em sistemas financeiros, isso é eliminatório — é tanto uma brecha de destruição de dados quanto de extração de dados sensíveis (dados de cedentes, valores, histórico).

**Correção:**
Usar parametrização em todas as queries sem exceção:

```typescript
await db.query(
  "SELECT * FROM receivables WHERE id = $1",
  [receivableId]
);

await db.query(
  "INSERT INTO settlements (receivable_id, amount, currency) VALUES ($1, $2, $3)",
  [receivableId, finalAmount.toFixed(2), currency]
);
```

---

### 🔴 P2 — Ausência de transação (Crítico — Integridade ACID)

**Onde:** bloco `try/catch` com dois `db.query` independentes e o comentário `// segue o jogo`.

```typescript
try {
  await db.query(`INSERT INTO settlements ...`);
  await db.query(`UPDATE receivables SET status = 'SETTLED' ...`);
} catch (e) {
  // se falhar aqui, o insert já rodou, então segue o jogo
}
```

**Impacto em produção:**
Se o `INSERT` em `settlements` for bem-sucedido mas o `UPDATE` em `receivables` falhar (timeout, deadlock, falha de rede), o sistema fica em estado inconsistente permanente: o recebível continua com `status = PENDING`, mas a liquidação já existe no banco. O recebível pode ser liquidado uma segunda vez, gerando uma duplicata financeira real — exatamente o incidente descrito no Anexo B. O comentário `// segue o jogo` demonstra que o autor sabia do problema e decidiu ignorá-lo.

**Correção:**
Envolver ambas as operações numa transação atômica:

```typescript
await db.transaction(async (trx) => {
  await trx.query("INSERT INTO settlements ...", [...]);
  await trx.query("UPDATE receivables SET status = 'SETTLED' ...", [...]);
});
```

---

### 🔴 P3 — Ponto flutuante binário em cálculo financeiro (Crítico — Precisão)

**Onde:** uso de `Math.pow` e divisão nativa com `number` (IEEE 754 double).

```typescript
const presentValue =
  receivable.face_value / Math.pow(1 + BASE_RATE + spread, receivable.term);
```

**Impacto em produção:**
`number` em JavaScript usa IEEE 754 de 64 bits, que não representa `0.1` exatamente. Operações com spreads percentuais acumulam erro de arredondamento. Em alto volume de transações, esses erros se traduzem em divergências de centavos que comprometem a conciliação contábil — um problema grave em fundos regulados pela CVM. Além disso, `BASE_RATE = 1.0` é claramente errado: a taxa base de 1% a.m. deve ser `0.01`, não `1.0` (que representa 100% a.m.).

**Correção:**
Usar biblioteca de precisão decimal (ex: `decimal.js` ou `big.js`) e corrigir o valor da taxa base:

```typescript
import Decimal from "decimal.js";

const BASE_RATE = new Decimal("0.01"); // 1% a.m., não 100%
const spread = new Decimal(receivable.type === "DUPLICATA" ? "0.015" : "0.025");
const divisor = BASE_RATE.plus(spread).plus(1).pow(receivable.term);
const presentValue = new Decimal(receivable.face_value).div(divisor);
```

---

### 🔴 P4 — Ausência de idempotência (Crítico — Duplicata financeira)

**Onde:** o endpoint não possui nenhum mecanismo de idempotência.

**Impacto em produção:**
Qualquer retry de rede, timeout de cliente ou duplo clique do operador processa a mesma liquidação múltiplas vezes. Dado que a falha de atomicidade do P2 pode deixar o recebível como `PENDING` mesmo após um `INSERT` em `settlements`, o sistema pode tanto criar liquidações duplicadas quanto entrar em loop de retentativas. Em ambiente financeiro, isso representa pagamento duplo ao cedente.

**Correção:**
Exigir um header `Idempotency-Key` (UUID gerado pelo cliente) e verificar sua existência antes de processar:

```typescript
const idempotencyKey = req.headers["idempotency-key"];
const existing = await db.queryOne(
  "SELECT * FROM settlements WHERE idempotency_key = $1",
  [idempotencyKey]
);
if (existing) return res.status(200).json({ ok: true, amount: existing.amount });
```

---

### 🟠 P5 — NullPointerException silencioso (Alto — Disponibilidade)

**Onde:** acesso a `receivable.type`, `receivable.face_value` e `receivable.term` sem verificar se `receivable` é `null`.

```typescript
const receivable = await db.queryOne(`SELECT * FROM receivables WHERE id = ...`);
const spread = receivable.type === "DUPLICATA" ? 1.5 : 2.5; // explode se null
```

**Impacto em produção:**
Se `receivableId` não existir no banco, `receivable` será `null` e `receivable.type` lança `TypeError: Cannot read properties of null`. O erro não é capturado antes do bloco try/catch das queries, então a requisição morre com um 500 não tratado. Pior: dependendo do framework, pode nem retornar resposta ao cliente.

**Correção:**
Verificar e retornar 404 explícito:

```typescript
if (!receivable) {
  return res.status(404).json({ code: "RECEIVABLE_NOT_FOUND", message: `Receivable ${receivableId} not found` });
}
```

---

### 🟠 P6 — Exceção engolida silenciosamente (Alto — Observabilidade)

**Onde:** o `catch` captura o erro e ignora completamente.

```typescript
} catch (e) {
  // se falhar aqui, o insert já rodou, então segue o jogo
}
```

**Impacto em produção:**
Falhas de banco (constraint violation, deadlock, timeout) passam completamente despercebidas. O endpoint retorna `200 OK` mesmo quando nenhuma das operações completou. Do ponto de vista do operador, a liquidação parece ter funcionado. Do ponto de vista do banco, nada foi persistido. Não há nenhum log, métrica ou alerta que permita detectar o problema.

**Correção:**
Nunca engolir exceções em operações financeiras. Logar e propagar:

```typescript
} catch (e) {
  logger.error("Settlement failed", { receivableId, error: e });
  return res.status(500).json({ code: "SETTLEMENT_FAILED", message: "Internal error during settlement" });
}
```

---

### 🟠 P7 — Retorno 200 OK em toda situação (Alto — Semântica HTTP)

**Onde:** `res.status(200).json({ ok: true, ... })` para qualquer resultado.

**Impacto em produção:**
Clientes (frontend, sistemas de conciliação, load balancers) não conseguem distinguir sucesso de falha sem inspecionar o corpo da resposta. Retentativas automáticas baseadas em status HTTP nunca serão acionadas. O padrão correto para criação de recurso é `201 Created`.

**Correção:**
Usar status semânticos: `201 Created` para nova liquidação, `200 OK` para retentativa idempotente, `404` para recebível não encontrado, `409 Conflict` para recebível já liquidado.

---

### 🟡 P8 — Ausência de validação de input (Médio — Robustez)

**Onde:** `receivableId` e `currency` são usados diretamente do body sem nenhuma validação.

**Impacto em produção:**
`receivableId` pode ser `undefined`, string, objeto, ou número negativo. `currency` pode ser qualquer string (`"EUR"`, `"XYZ"`, `"' OR 1=1--"`). Sem validação, o sistema processa entrada malformada até o ponto de falha mais próximo, com erros difíceis de diagnosticar.

**Correção:**
Validar antes de processar:

```typescript
if (!receivableId || typeof receivableId !== "number") {
  return res.status(400).json({ code: "INVALID_INPUT", message: "receivableId must be a positive number" });
}
if (!["BRL", "USD"].includes(currency)) {
  return res.status(400).json({ code: "INVALID_INPUT", message: "currency must be BRL or USD" });
}
```

---

### 🟡 P9 — Taxa de câmbio sem tratamento de falha (Médio — Resiliência)

**Onde:** `fxService.getLatestRate("USD")` pode lançar exceção se o serviço estiver indisponível.

```typescript
const rate = await fxService.getLatestRate("USD");
finalAmount = presentValue / rate;
```

**Impacto em produção:**
Se o provedor de câmbio estiver fora do ar, toda liquidação em USD falha com 500 sem mensagem útil. Não há timeout configurado, circuit breaker, nem fallback. Uma indisponibilidade momentânea do serviço externo derruba o fluxo de liquidações inteiro.

**Correção:**
Tratar a ausência de taxa com erro específico e considerar timeout + circuit breaker para o serviço externo:

```typescript
const exchangeRate = await fxService.getLatestRate("USD");
if (!exchangeRate) {
  return res.status(503).json({ code: "EXCHANGE_RATE_UNAVAILABLE", message: "No exchange rate available for USD" });
}
```

---

### 🟡 P10 — Ausência de auditoria (Médio — Rastreabilidade)

**Onde:** o `INSERT` em `settlements` grava apenas `receivable_id`, `amount` e `currency`.

```sql
INSERT INTO settlements (receivable_id, amount, currency)
VALUES (...)
```

**Impacto em produção:**
Se o cálculo for contestado posteriormente (por auditoria regulatória ou pelo próprio cedente), não há como reconstruir qual taxa base, spread, taxa de câmbio ou prazo foram usados. O valor gravado é apenas o resultado final — sem os parâmetros que o geraram. Em fundo regulado pela CVM, ausência de trilha de auditoria é um risco regulatório direto.

**Correção:**
Gravar todos os parâmetros do cálculo: `base_rate_used`, `spread_used`, `term_used`, `exchange_rate_used`, `present_value_brl`, `settled_at`.

---

## Resumo executivo

| # | Problema | Severidade | Categoria |
|---|---|---|---|
| P1 | SQL Injection | 🔴 Crítico | Segurança |
| P2 | Ausência de transação ACID | 🔴 Crítico | Integridade |
| P3 | Ponto flutuante + taxa base errada | 🔴 Crítico | Precisão financeira |
| P4 | Ausência de idempotência | 🔴 Crítico | Duplicata financeira |
| P5 | NullPointerException sem tratamento | 🟠 Alto | Disponibilidade |
| P6 | Exceção engolida silenciosamente | 🟠 Alto | Observabilidade |
| P7 | 200 OK para tudo | 🟠 Alto | Semântica HTTP |
| P8 | Ausência de validação de input | 🟡 Médio | Robustez |
| P9 | Taxa de câmbio sem tratamento de falha | 🟡 Médio | Resiliência |
| P10 | Ausência de auditoria | 🟡 Médio | Rastreabilidade |

**Veredicto:** este código não deve ir a produção. Os problemas P1–P4 sozinhos são suficientes para causar um incidente financeiro grave: SQL injection pode destruir dados, ausência de transação gera liquidações parciais, ponto flutuante gera valores incorretos e ausência de idempotência gera pagamentos duplicados. O comentário `// segue o jogo` indica que o autor tinha consciência da falha de atomicidade e deliberadamente escolheu ignorá-la — o que torna o merge ainda mais preocupante do ponto de vista de processo.

---

*Revisão escrita como parte da Fase 2 do desafio técnico SRM Credit Engine.*
