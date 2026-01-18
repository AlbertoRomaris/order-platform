

## Overview

This project is a **production-oriented learning project** focused on backend architecture and asynchronous processing.

It implements an **event-driven order processing platform** with:

- synchronous REST API
- asynchronous background worker
- explicit order state machine
- retries with backoff
- persistent Dead Letter Queue (DLQ)
- manual reprocessing of failed orders

The goal is not to build a CRUD application, but to **learn and demonstrate how to design resilient backend systems**.

---

## Core Principles

- Architecture over infrastructure
- Explicit state transitions
- Database as source of truth
- Asynchronous processing
- Failure handling as a first-class concern
- Business logic independent of cloud providers

---

## High-Level Architecture

The system follows an event-driven architecture with clear separation between
synchronous and asynchronous responsibilities.

```
Client
|
| POST /orders
v
API (Spring Boot)
|
| persist Order (PENDING)
v
PostgreSQL (source of truth)
|
| OrderCreated event
v
In-memory Queue
|
v
Order Worker (asynchronous)
```

The API is responsible only for accepting requests and persisting state.
All business processing happens asynchronously in the worker.

---

## Order Lifecycle

Orders evolve through an explicit state machine enforced at the domain level.

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

### State meanings

- **PENDING**  
  Order accepted and persisted, waiting for processing.

- **PROCESSING**  
  Order is currently being handled by the worker.

- **PROCESSED**  
  Order completed successfully.

- **FAILED**  
  Terminal failure after exceeding maximum retries.

- **DLQ (Dead Letter Queue)**  
  Persistent record of failed orders for operational inspection and recovery.

---

## Asynchronous Processing

Order processing never happens inside the API request thread.

The workflow is:

1. API receives request and creates a `PENDING` order
2. An `OrderCreated` event is published
3. The worker consumes the event asynchronously
4. The worker transitions the order through its states

This design provides:

- decoupling
- resilience
- scalability
- clear operational visibility

---

## Failure Handling & Retries

The worker includes built-in failure simulation and retry logic.

- Failures are simulated via configurable probability
- Each failure increments `retryCount`
- Orders are retried until `maxRetries` is reached
- When retries are exhausted:
    - Order is marked `FAILED`
    - A DLQ entry is persisted

Retry and DLQ behavior is fully configurable via application properties.

---

## Dead Letter Queue (DLQ)

The DLQ is implemented as a persistent database table.

### DLQ responsibilities

- Store terminally failed orders
- Preserve failure reason and retry count
- Enable operational inspection
- Allow manual reprocessing

---

## Manual Reprocessing

Failed orders can be manually reprocessed via API:

``POST /dlq/{orderId}/reprocess``

This operation:

1. Deletes the DLQ entry
2. Resets the order state to `PENDING`
3. Re-enqueues the event for processing

This models real-world operational recovery workflows.

# API Endpoints

All endpoints are intentionally minimal to keep focus on architecture.

### Orders

`POST /orders`

`GET /orders/{id}`

### Dead Letter Queue

`GET /dlq`

`POST /dlq/{orderId}/reprocess`

---

## Configuration

Worker behavior is fully configurable without code changes:

```yaml
order:
  worker:
    maxRetries: 3
    failureProbability: 0.3
    processingDelayMs: 500
    retryDelayMs: 1000
````

This allows testing different failure scenarios locally.

---


## Project Structure

````
services/api
├── domain          # Entities, state machine, business rules
├── application     # Use cases and orchestration
├── infrastructure  # Persistence, messaging, web adapters
````

The worker currently runs in the same process as the API.
In future versions it will be extracted into an independent service.

## Current Status

### Version 1 complete

Implemented features:

- Event-driven order processing

- Explicit state machine

- Asynchronous worker

- Retry mechanism

- Persistent DLQ

- Manual reprocessing endpoint

### Next Versions

**V2 - Cloud Lab**

- External queue (AWS SQS)

- Separate worker service

- Dockerized deployment

- Infrastructure as Code (Terraform)

**V3 - Production-like**

- ECS + ALB

- RDS PostgreSQL

- Autoscaling workers

- Metrics, alerts, observability
---

This project is intentionally simple in scope, but strict in design,
favoring correctness, clarity, and operational realism over feature count.

