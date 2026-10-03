# SRM Credit Engine

Plataforma de **cessão de crédito multimoedas** para operações de FIDC. O sistema
precifica recebíveis (duplicatas e cheques pré-datados), simula o valor presente
em tempo real, liquida títulos com garantias de idempotência e auditoria, e
mantém um extrato analítico de todas as liquidações — incluindo conversão
cross-currency BRL → USD com taxa de câmbio gravada imutavelmente.

O projeto é dividido em dois módulos:

| Módulo | Pasta | Descrição |
|--------|-------|-----------|
| **Backend** | [`credit-engine/`](credit-engine) | API REST em Java 21 + Spring Boot. Motor de precificação, liquidação ACID, câmbio e extrato. |
| **Frontend** | [`frontend/`](frontend) | SPA em React + TypeScript + Redux Toolkit. Painel do operador, grid de transações e cadastro de câmbio. |

---

## Sumário

- [Visão geral do domínio](#visão-geral-do-domínio)
- [Arquitetura](#arquitetura)
- [Stack e versões](#stack-e-versões)
- [Pré-requisitos](#pré-requisitos)
- [Como rodar](#como-rodar)
  - [Opção A — Docker Compose (recomendada)](#opção-a--docker-compose-recomendada)
  - [Opção B — Backend e banco locais](#opção-b--backend-e-banco-locais)
  - [Frontend em modo de desenvolvimento](#frontend-em-modo-de-desenvolvimento)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Endpoints da API](#endpoints-da-api)
- [Fórmula de precificação e golden cases](#fórmula-de-precificação-e-golden-cases)
- [Testes](#testes)
- [Comandos úteis](#comandos-úteis)
- [Documentação complementar](#documentação-complementar)

---

## Visão geral do domínio

Um **recebível** (`Receivable`) é um título a vencer, com valor de face, prazo em
meses e tipo. Cada tipo tem um *spread* mensal próprio:

| Tipo | Spread a.m. |
|------|-------------|
| Duplicata Mercantil (`DUPLICATA_MERCANTIL`) | 1,5% |
| Cheque Pré-datado (`CHEQUE_PRE_DATADO`) | 2,5% |

O **motor de precificação** calcula o valor presente (VP) do título. Ao
**liquidar** um recebível, o sistema grava um registro imutável (`Settlement`)
com todos os parâmetros do cálculo para auditoria. Liquidações em **USD** usam a
taxa de câmbio vigente no momento da operação (`ExchangeRate`), também gravada no
registro.

Garantias centrais:

- **Precisão numérica:** todo valor monetário usa `BigDecimal` (nunca `float`/`double`), com arredondamento `HALF_EVEN` aplicado uma única vez ao resultado final.
- **Idempotência:** liquidações exigem um header `Idempotency-Key`; retentativas com a mesma chave retornam o resultado original sem duplicar.
- **ACID:** o registro da liquidação e a mudança de status do recebível ocorrem na mesma transação.
- **Auditoria:** cada liquidação registra taxa base, spread, prazo, câmbio e timestamp usados.

---

## Arquitetura

```
┌──────────────────────────┐        /api/*         ┌───────────────────────────┐
│  Frontend (React SPA)    │  ───────────────────► │  Backend (Spring Boot)    │
│  Vite dev server :5173   │   proxy em dev        │  :8080                    │
│  Redux Toolkit + RTKQ    │                       │  Controllers → Services   │
└──────────────────────────┘                       │  → Pricing (Strategy)     │
                                                    │  → Repositories (JPA)     │
                                                    └────────────┬──────────────┘
                                                                 │ JDBC
                                                                 ▼
                                                    ┌───────────────────────────┐
                                                    │  PostgreSQL  :5432         │
                                                    │  schema via Flyway (V1)    │
                                                    └───────────────────────────┘
```

**Backend** — arquitetura em camadas (`controller` → `service` → `repository`),
com o motor de precificação isolado em `pricing/` usando o padrão **Strategy**
(um `PricingStrategy` por tipo de recebível, resolvido por um factory via
injeção do Spring). Schema versionado por **Flyway** (`ddl-auto: validate`).

**Frontend** — SPA com **Redux Toolkit** e **RTK Query** para o estado de
servidor (cache, invalidação por tags, hooks tipados). Endpoints injetados por
feature (`features/*/*Api.ts`), componentes de UI reutilizáveis com **CSS
Modules** e três telas roteadas por `react-router-dom`.

---

## Stack e versões

### Backend (`credit-engine/`)

| Tecnologia | Versão | Papel |
|------------|--------|-------|
| Java (JDK) | **21** (LTS) | Linguagem |
| Spring Boot | **4.1.1** | Framework de aplicação |
| Spring Web / Data JPA / Validation | (gerenciado pelo Spring Boot) | REST, ORM, Bean Validation |
| Hibernate | (via Spring Data JPA) | ORM |
| PostgreSQL | **16** (imagem `postgres:16-alpine`) | Banco de dados |
| Flyway | (gerenciado pelo Spring Boot) + `flyway-database-postgresql` | Migrations versionadas |
| SpringDoc OpenAPI | **2.8.9** | Documentação Swagger UI |
| Lombok | **1.18.36** | Redução de boilerplate |
| Maven | via `mvnw` wrapper | Build |

### Frontend (`frontend/`)

| Tecnologia | Versão | Papel |
|------------|--------|-------|
| Node.js | **24.x** (testado em 24.21.0) | Runtime de build/dev |
| npm | **11.x** (testado em 11.19.0) | Gerenciador de pacotes |
| React | **19.x** | Biblioteca de UI |
| TypeScript | **6.x** | Tipagem estática |
| Vite | **8.x** | Dev server e bundler |
| Redux Toolkit (com RTK Query) | **2.13** | Estado global e chamadas à API |
| React Redux | **9.3** | Integração React ↔ Redux |
| React Router DOM | **7.x** | Roteamento SPA |
| React Hook Form | **7.x** | Formulários |
| uuid | **14.x** | Geração de `Idempotency-Key` |

> As versões do frontend seguem os ranges do `package.json`; os números exatos
> ficam no `package-lock.json`.

---

## Pré-requisitos

Para rodar **tudo via Docker** (opção recomendada), você precisa apenas de:

- **Docker** e **Docker Compose**

Para rodar **localmente** (sem Docker), você precisa de:

- **JDK 21** (o backend usa o wrapper `mvnw`, então o Maven não precisa estar instalado)
- **PostgreSQL 16** rodando (ou via Docker só para o banco)
- **Node.js 24+** e **npm 11+** (para o frontend)

---

## Como rodar

### Opção A — Docker Compose (recomendada)

Sobe o PostgreSQL e o backend já integrados, com healthcheck no banco.

```bash
# 1. Na raiz do projeto, crie o .env a partir do exemplo
cp .env.example .env        # (no Windows PowerShell: Copy-Item .env.example .env)

# 2. Suba os containers
docker compose up --build
```

- Backend disponível em **http://localhost:8080**
- Swagger UI em **http://localhost:8080/swagger-ui**
- PostgreSQL em **localhost:5432**

Para parar:

```bash
docker compose down          # mantém os dados (volume)
docker compose down -v       # remove também o volume do banco
```

> O `docker-compose.yml` sobe apenas banco + backend. O frontend roda em modo de
> desenvolvimento (veja abaixo) e fala com o backend via proxy.

### Opção B — Backend e banco locais

Suba só o banco via Docker (ou use um PostgreSQL já instalado):

```bash
docker compose up -d db
```

Então rode o backend com o wrapper Maven:

```bash
cd credit-engine

# Linux/macOS
./mvnw spring-boot:run

# Windows
.\mvnw.cmd spring-boot:run
```

O backend aplica as migrations do Flyway automaticamente na subida.

### Frontend em modo de desenvolvimento

Em outro terminal:

```bash
cd frontend
npm install            # apenas na primeira vez
npm run dev
```

- Frontend em **http://localhost:5173**
- As chamadas a `/api/*` são encaminhadas para o backend (`localhost:8080`) pelo
  **proxy do Vite** configurado em `vite.config.ts` — não há problema de CORS em dev.

> **Importante:** o proxy só é lido na inicialização do Vite. Se o backend subir
> depois, ou se você alterar o `vite.config.ts`, **reinicie o `npm run dev`**.

---

## Variáveis de ambiente

Definidas no `.env` da raiz (veja `.env.example`), consumidas pelo
`docker-compose.yml`:

| Variável | Default | Descrição |
|----------|---------|-----------|
| `DB_NAME` | `srm_credit_engine` | Nome do banco PostgreSQL |
| `DB_USERNAME` | `postgres` | Usuário do banco |
| `DB_PASSWORD` | `troque_esta_senha` | Senha do banco (troque em qualquer ambiente real) |
| `BASE_RATE` | `0.01` | Taxa base mensal do fundo (1,00% a.m.) |

O arquivo `.env` **nunca** deve ser commitado — já está no `.gitignore`.

---

## Endpoints da API

Base: `/api`. Documentação interativa completa em **`/swagger-ui`**.

### Recebíveis

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/receivables` | Cadastra um recebível |
| `GET` | `/api/receivables` | Lista recebíveis (paginação server-side) |
| `GET` | `/api/receivables/{id}` | Busca um recebível por ID |

### Simulação

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/simulate` | Calcula o VP em tempo real (não persiste nada) |

### Liquidação e extrato

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/settlements` | Liquida um recebível. **Requer header `Idempotency-Key`**. Retorna `201` para nova liquidação, `200` para retentativa idempotente. |
| `GET` | `/api/settlements` | Extrato com filtros `from`, `to`, `cedente`, `currency` + paginação (`page`, `size`, `sort`) |
| `GET` | `/api/settlements/{id}` | Detalhe de uma liquidação |

### Câmbio

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/exchange-rates` | Cadastra uma taxa de câmbio (par `USD/BRL`) |
| `GET` | `/api/exchange-rates/latest/{pair}` | Taxa vigente para o par (ex: `USD/BRL`) |

**Exemplo — liquidar um recebível (idempotente):**

```bash
curl -X POST http://localhost:8080/api/settlements \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 3f1c8e2a-9b4d-4a7e-8c21-0d6f5b2a1e34" \
  -d '{ "receivableId": 1, "currency": "BRL" }'
```

Erros seguem um corpo JSON padronizado (`ApiError`) com `code`, `message`,
`timestamp` e, em validações, `details`. Códigos HTTP são semânticos
(`201`, `200`, `400`, `404`, `409`, `422`, `500`).

---

## Fórmula de precificação e golden cases

```
Valor Presente = Valor de Face / (1 + Taxa Base + Spread) ^ Prazo
Deságio        = Valor de Face − Valor Presente
```

- Prazo em meses inteiros; taxa base e spread em decimal (ex: 1,5% → `0.015`).
- Cálculos intermediários com `MathContext(20, HALF_EVEN)`; arredondamento para 2 casas aplicado **uma única vez** no resultado final.
- Para USD: o VP em BRL (já arredondado) é dividido pela taxa de câmbio e arredondado novamente (`HALF_EVEN`, 2 casas).

Casos de aceite validados ao centavo nos testes:

| Caso | Entrada | Resultado |
|------|---------|-----------|
| **C1** | Duplicata, R$ 100.000,00, 3 meses, BRL | **R$ 92.859,94** |
| **C2** | Cheque, R$ 25.000,00, 2 meses, BRL | **R$ 23.337,77** |
| **C3** | Duplicata, R$ 100.000,00, 3 meses, USD (câmbio 5,4321) | **US$ 17.094,67** |

---

## Testes

### Backend

```bash
cd credit-engine

# Linux/macOS
./mvnw test

# Windows
.\mvnw.cmd test
```

Cobre o motor de precificação (golden cases + casos de borda) e o serviço de
liquidação (idempotência, 404/409, conversão cross-currency C3) com JUnit 5 +
Mockito.

### Frontend

Verificação de tipos + build de produção:

```bash
cd frontend
npm run build        # tsc -b && vite build
npm run lint         # ESLint
```

---

## Comandos úteis

| Objetivo | Comando |
|----------|---------|
| Subir tudo (banco + backend) | `docker compose up --build` |
| Subir só o banco | `docker compose up -d db` |
| Rodar backend local | `cd credit-engine && ./mvnw spring-boot:run` |
| Testes do backend | `cd credit-engine && ./mvnw test` |
| Empacotar o backend (JAR) | `cd credit-engine && ./mvnw package` |
| Rodar frontend (dev) | `cd frontend && npm run dev` |
| Build do frontend | `cd frontend && npm run build` |
| Preview do build | `cd frontend && npm run preview` |
| Lint do frontend | `cd frontend && npm run lint` |

> No Windows, use `.\mvnw.cmd` no lugar de `./mvnw`.

---

## Documentação complementar

| Documento | Conteúdo |
|-----------|----------|
| [`SPEC.md`](SPEC.md) | Premissas, ambiguidades, decisões de precisão numérica e critérios de aceite |
| [`REVIEW.md`](REVIEW.md) | Code review reverso de um endpoint problemático, ordenado por severidade |
| [`DECISIONS.md`](DECISIONS.md) | O que foi cortado/simplificado e por quê; decisões de design |
| [`AI_USAGE.md`](AI_USAGE.md) | Engenharia da colaboração com IA: onde errou, o que não foi delegado |
