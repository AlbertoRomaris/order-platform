# V2 – Cloud-ready Architecture (Outbox + Separate Worker)

## Overview

Version 2 evolves the system from a **single-process, in-memory event model**
to a **cloud-ready, production-style architecture**.

The focus of this version is **architecture**, not cloud infrastructure.

V2 introduces:

- Transactional Outbox pattern
- Separate asynchronous worker service
- Database-backed event flow
- Explicit ownership of responsibilities between API and Worker
- Failure handling prepared for distributed systems

No external queue or cloud provider is required yet.
The system still runs locally using PostgreSQL as the source of truth.

---

## Why V2 Exists

V1 proved the **event-driven model and failure handling logic**.

However, V1 still had important limitations:

- Worker ran inside the API process
- In-memory queue was not durable
- Events could be lost on crash
- Architecture was not cloud-portable

V2 solves these problems by introducing **clear boundaries** and
**persistent asynchronous workflows**.

This version is intentionally designed so that **replacing the database-backed
outbox with SQS/Kafka later is trivial**.

---

## Core Architectural Principles

- Database as the source of truth
- No in-memory queues for critical workflows
- Explicit separation between:
    - command handling (API)
    - event publishing (outbox)
    - event consumption (worker)
- Failure handling and retries as first-class concerns
- Architecture independent of cloud provider

---

## High-Level Architecture (V2)

```
Client
|
| POST /orders
v
API (Spring Boot)
|
| 1. Persist Order (PENDING)
| 2. Persist OutboxEvent (OrderCreated)
v
PostgreSQL (source of truth)
|
| Outbox polling
v
Worker Service
|
| Process order
| Retry / Fail
v
PostgreSQL
```

Key difference vs V1:
- **No direct event dispatch**
- **No in-memory queue**
- Everything flows through the database

---

## Transactional Outbox Pattern

### What is stored in the Outbox

Each domain event is persisted as a row:

- event id
- aggregate id (order id)
- event type
- payload
- status (`PENDING`, `PROCESSING`, `PROCESSED`, `FAILED`)
- attempts
- next attempt timestamp
- locking metadata

The outbox table guarantees:

- durability
- ordering
- retryability
- crash safety

---

## Observability & Traceability (Correlation ID)

V2 introduces **end-to-end traceability** using a Correlation ID, designed
specifically for asynchronous, event-driven systems.

The goal is to follow a **single business intent** across:
- HTTP requests
- Transactional outbox
- Asynchronous worker processing
- Retries and DLQ handling

This is critical in distributed systems where execution is decoupled
in time and process.

---

### Correlation ID lifecycle

1. **API**
  - Each incoming HTTP request accepts an optional `X-Correlation-Id` header.
  - If not provided, the API generates one automatically.
  - The Correlation ID is stored in MDC and appears in all API logs.

2. **Outbox**
  - The Correlation ID is persisted inside the outbox event payload.
  - This guarantees durability and allows correlation even after restarts.

   Example payload:
   ```json
   {
     "orderId": "...",
     "correlationId": "test-777"
   }
```

3. **Worker**
- The worker extracts the Correlation ID from the event payload.
- It injects it into MDC during event processing.
- All logs related to processing, retries, and failures include the same Correlation ID.
  
Example logs:
   ```json
   {
  correlationId=test-777 Order ... RETRY
  correlationId=test-777 Order ... PROCESSED
   }
```

This allows reconstructing the full lifecycle of an order across
asynchronous boundaries.

### Design notes

- Correlation IDs are transport-agnostic (DB outbox, SQS, Kafka).
- They survive crashes, retries, and restarts.
- This design is compatible with cloud-native observability stacks (e.g. CloudWatch, OpenTelemetry).

---

## Responsibility Split

### API Service

The API **never processes events**.

Its responsibilities are limited to:

- Validate input
- Persist domain state
- Persist outbox events
- Expose read APIs
- Expose DLQ and reprocess endpoints

The API **does not care if or when the worker runs**.

---

### Worker Service

The worker is a **separate Spring Boot application**.

Its responsibilities:

- Poll the outbox
- Claim events using database locking
- Process orders
- Handle retries with backoff
- Mark events as processed or failed
- Write to DLQ when retries are exhausted

The worker can be:
- restarted
- duplicated
- scaled
  without affecting correctness.

---

## Outbox Polling & Locking

The worker uses:

- `FOR UPDATE SKIP LOCKED`
- explicit `locked_by` and `locked_at`

This ensures:

- no double processing
- safe parallel workers
- no race conditions

Example lifecycle of an outbox event:

```
PENDING
|
|
v
PROCESSING (locked by worker-X)
|
+--> PROCESSED
|
+--> PENDING (retry with delay)
|
+--> FAILED
```

---

## Order Lifecycle (unchanged, but now distributed)

```
PENDING
     |
     v
  PROCESSING
|           |
v           v
PROCESSED FAILED
            |
            v
           DLQ

```

Difference vs V1:
- State transitions are now driven by **outbox consumption**
- Not by in-process event handlers

---

## Failure Handling & Retries

Retry behavior is fully deterministic and persisted.

- Each failure increments `attempts`
- `next_attempt_at` controls backoff
- Worker ignores events not ready yet
- After max retries:
    - Order → `FAILED`
    - Outbox → `FAILED`
    - DLQ entry persisted

No retry logic lives in memory.

---

## Dead Letter Queue (DLQ)

DLQ is a **persistent table**.

Each DLQ entry stores:

- order id
- failure reason
- retry count
- failure timestamp

DLQ entries survive restarts and crashes.

---

## Manual Reprocessing (V2 semantics)

Reprocessing is explicit and controlled.

`POST /dlq/{orderId}/reprocess`

This operation:

1. Deletes the DLQ entry
2. Resets the order state to `PENDING`
3. Creates a **new outbox event**
4. Leaves processing entirely to the worker

Important:
- API does NOT process the order
- API only emits intent
- Worker remains the only executor

---

## Configuration

Worker behavior is fully configurable:

```yaml
worker:
  outbox:
    poll:
      delay-ms: 1000

order:
  worker:
    maxRetries: 3
    retryDelayMs: 1000
    failureProbability: 0.3
```

## Project Structure (V2)

The project is organized by **responsibility**, not by framework or deployment.

The `core` module remains completely framework-agnostic and is shared
by both the API and the Worker.

```
order-platform/
├── services/
│ ├── core/
│ │ ├── domain/ # Domain entities and state machine
│ │ ├── application/
│ │ │ ├── model/ # Plain models (e.g. OutboxEvent, DLQ entry)
│ │ │ ├── port/ # Interfaces (repositories, publishers)
│ │ │ └── usecase/ # Pure business use cases
│ │
│ ├── api/
│ │ ├── application/ # Transactional wrappers & orchestration
│ │ ├── infrastructure/
│ │ │ ├── persistence/ # JPA repositories (orders, DLQ)
│ │ │ ├── outbox/ # JDBC outbox publisher (V2)
│ │ │ ├── messaging/ # In-memory publisher (V1 only)
│ │ │ └── web/ # REST controllers
│ │
│ └── worker/
│ ├── job/ # Scheduled outbox polling jobs
│ └── infrastructure/
│ └── persistence/
│ └── outbox/ # JDBC outbox consumer & state transitions
│
├── docker/ # Local Docker setup
├── docs/ # Architecture documentation
└── infra/ # Infrastructure as Code (future versions)

```

### Key Structural Decisions

- `core` contains **all business logic** and has **no Spring, JPA, or cloud dependencies**
- `api` and `worker` depend on `core`, but **never on each other**
- Infrastructure concerns are isolated behind **ports & adapters**
- V1 and V2 coexist via configuration, not code duplication
