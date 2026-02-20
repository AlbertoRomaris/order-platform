# V2.6 – Minimal Observability Layer

## Overview

Version V2.6 introduces a **minimal, production-aligned observability layer**.

The objective is not to build a full monitoring platform,  
but to make the system:

- debuggable
- measurable
- operationally transparent
- understandable during interviews and architectural discussions

This version adds:

- Spring Boot Actuator
- Prometheus-compatible metrics
- Business-level metrics (API & Worker)
- Transport-aware metrics (SQS / Outbox)
- Outbox backlog visibility
- Processing latency measurements

No dashboards, no alerts, no tracing systems are introduced in this version.  
Those belong to V3.

---

## Why Observability Matters Here

This project models a distributed, asynchronous architecture.

In such systems:

- Failures are not immediate
- Work happens out-of-band
- Retries happen later
- State evolves over time

Without observability, the system becomes a black box.

V2.6 ensures we can answer questions like:

- Are orders being created?
- Are events being published?
- Is the worker alive?
- Is backlog accumulating?
- Are retries happening?
- Are failures increasing?
- Is processing slow?

This is the minimum required to reason about system health.

---

# Actuator & Prometheus

Both API and Worker expose:

````bash
/actuator/health
/actuator/prometheus
````

Prometheus endpoint exposes metrics in text format compatible with Prometheus scraping.

This allows:

- Local inspection
- Manual verification
- Future integration with monitoring stacks

---

# API Observability

The API represents the synchronous command side.

Its metrics answer:

- How many orders are being created?
- How long does creation take?
- Are events being successfully published?
- Is the outbox accumulating?

---

## 1. Order Creation Latency

**`order_api_create_order_seconds`**


Type: `summary`

Measures the time required to:

- Persist the order
- Commit the transaction
- Publish (or enqueue) the event

Why this matters:

- Detect slow database operations
- Detect publish bottlenecks
- Understand request-level performance

---

## 2. Orders Created Counter


**`order_api_orders_total`**


Type: `counter`

Counts successfully created orders.

Why this matters:

- Throughput measurement
- Correlate with worker processing rate
- Validate end-to-end system flow

---

## 3. Event Publishing by Transport


`order_api_events_published_total{transport="sqs|outbox"}`
`order_api_event_publish_failed_total{transport="sqs|outbox"}`


Type: `counter`

Counts publish attempts grouped by transport.

Why this matters:

- Makes transport pluggability observable
- Distinguishes SQS vs Outbox mode
- Detects transport-level failures

This is especially useful when switching execution modes:

````bash
order.events.mode = outbox
order.events.mode = sqs
````


The business logic remains unchanged, but metrics reveal how events are delivered.

---

## 4. Outbox Backlog Gauge


`order_api_outbox_backlog`


Type: `gauge`

Counts outbox rows with status:
- `PENDING`
- `PROCESSING`

Why this matters:

- Detect backlog accumulation
- Reveal worker downtime
- Detect slow consumers

This metric reflects real database state.

If the worker is stopped and orders are created, this number increases.

---

## 5. Outbox Processing Gauge


`order_api_outbox_processing`


Type: `gauge`

Counts outbox rows currently in `PROCESSING`.

Why this matters:

- Detect stuck locks
- Detect crashed workers
- Reveal long-running processing

Together with backlog, this gives a full picture of asynchronous health.

---

# Worker Observability

The worker represents the asynchronous execution side.

Its metrics answer:

- Is it polling?
- Is it receiving messages?
- Is it processing successfully?
- Are retries happening?
- Are failures increasing?
- How long does processing take?

---

## 1. Poll Cycle Counter


`order_worker_poll_total`


Counts worker poll cycles (SQS or outbox).

Why this matters:

- Confirms worker liveness
- Reveals aggressive polling configuration
- Helps tune long polling

---

## 2. Messages Received


`order_worker_messages_received_total`


Counts events/messages fetched from transport.

Why this matters:

- Confirms actual consumption
- Detects silent failures
- Helps correlate with API throughput

---

## 3. Messages Processed


`order_worker_messages_processed_total`


Counts successful processing attempts.

Why this matters:

- Confirms state transitions
- Enables throughput comparison with API

---

## 4. Retry & Failure Metrics


`order_worker_messages_failed_total`
`order_worker_messages_retried_total`


Why this matters:

- Detect instability
- Detect increased failure probability
- Reveal misconfiguration

---

## 5. Business DLQ Metric


`order_worker_business_dlq_total`


Counts business-level DLQ entries.

Represents domain failure, not infrastructure failure.

---

## 6. SQS DLQ Metric


`order_worker_sqs_dlq_total`


Represents infrastructure-level DLQ (when using SQS).

Business DLQ and Infrastructure DLQ remain intentionally decoupled.

---

## 7. Processing Latency


`order_worker_processing_seconds`


Type: `summary`

Measures time spent processing a single order.

Why this matters:

- Detect slow domain logic
- Detect database latency
- Detect blocking I/O

---

# Architectural Principles Preserved

Observability was added without:

- Modifying domain logic
- Introducing cloud SDKs in core
- Polluting business use cases
- Breaking port/adapters separation

Metrics live in infrastructure/observability layers.

The core remains framework-agnostic.

---

# Operational Demo Scenarios

V2.6 enables practical demonstrations:

1. Stop the worker → create orders → backlog increases.
2. Start the worker → backlog drains.
3. Increase failureProbability → retries increase.
4. Force failures → DLQ counters increase.
5. Switch transport mode → metrics reflect transport change.

The system is no longer a black box.

---

# Why Not More?

V2.6 intentionally stops here.

We do NOT add:

- Distributed tracing
- Prometheus server
- Grafana dashboards
- Alert rules
- CloudWatch exporters
- Structured JSON logging
- OpenTelemetry

Those features require infrastructure-level decisions  
and belong to V3.

V2.6 provides:

- Local introspection
- Debuggability
- Interview-ready visibility
- Operational reasoning capability

That is the intended scope.

---

# Current Status

With V2.6, the platform now includes:

- Event-driven architecture
- Transactional outbox
- Separate worker service
- Dual DLQ design
- SQS cloud-ready transport
- Minimal but meaningful observability layer

The architecture is now:

- Cloud-ready
- Operationally visible
- Extensible toward production-grade monitoring

---

# Next Evolution (V3)

V3 will focus on:

- Prometheus server + Grafana dashboards
- AWS CloudWatch metrics
- Alerting rules
- Structured logging
- Possibly OpenTelemetry tracing
- Production-style deployment topology

V2.6 prepares the foundation.