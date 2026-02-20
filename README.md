## Project Status

The project evolves through **explicit architectural versions**, each one
introducing new concerns while keeping the business logic stable.

### Current Versions

#### ✅ V1 – In-process Event-driven Architecture
Completed.

- In-memory event queue
- Asynchronous worker inside API process
- Explicit order state machine
- Retries with backoff
- Persistent Dead Letter Queue (DLQ)
- Manual reprocessing endpoint

📄 Documentation:
- [V1 Architecture & Design](docs/README_V1.md)

---

#### ✅ V2 – Cloud-ready Architecture (Outbox + Separate Worker)
Architecturally complete.

V2 introduces a **production-grade, cloud-ready architecture** while still
running fully locally.  
The focus of this version is **correct distributed-systems design**, not
infrastructure complexity.

**Key characteristics:**

- Transactional Outbox pattern (database as source of truth)
- Clear separation between API (command side) and Worker (execution side)
- Database-backed asynchronous processing
- Durable retries with backoff and crash safety
- Persistent Dead Letter Queue (DLQ)
- Explicit manual reprocessing semantics (emit intent, never execute directly)
- End-to-end correlation ID propagation across async boundaries
- Stale lock recovery for crashed workers
- **Pluggable event transport**
    - Database outbox polling
    - AWS SQS (via LocalStack, cloud-compatible)

**Important design note:**

V2 is **cloud-ready but cloud-agnostic**:
- No cloud provider is required
- No infrastructure assumptions leak into the core
- Switching from DB outbox to SQS is a configuration change, not a rewrite

This version intentionally proves that the **same business logic**
can run correctly with:
- in-process events (V1)
- database-backed outbox (V2)
- external queues (SQS)

📄 Documentation:
- [V2 Architecture & Design](docs/README_V2.md)

---

#### ✅ V2.5 – AWS Lab – Minimal Cloud Deployment
Completed.

V2.5 validates that the cloud-ready architecture introduced in V2
runs correctly on real AWS infrastructure without modifying
the business logic.

This lab focuses on **runtime validation in AWS**, not on introducing
new architectural patterns.

**Infrastructure scope:**

- Single EC2 instance (Docker runtime)
- Dockerized API and Worker services
- PostgreSQL running in-container
- AWS SQS replacing database outbox polling
- SQS Dead Letter Queue (infrastructure-level DLQ)
- IAM Role attached to EC2 (no static access keys)
- Infrastructure provisioned via Terraform
- Security Group restricted to a specific public IP

**Architectural significance:**

- Confirms full separation between business logic and infrastructure
- Demonstrates IAM Role-based authentication (production-grade pattern)
- Proves transport pluggability (DB outbox → SQS) without code changes
- Validates retry semantics and DLQ behavior in real cloud conditions
- Maintains database as source of truth

This version completes the transition from:
- V1 → In-process async
- V2 → Database-backed outbox
- V2.5 → External cloud transport (SQS on AWS)

📄 Documentation:
- [AWS Lab – Minimal Cloud Deployment](docs/aws/README_AWS_LAB.md)
- [Terraform Infrastructure (infra/aws-lab)](infra/aws-lab/README.md)

---

#### ✅ V2.6 – Minimal Observability Layer
Completed.

V2.6 introduces a **minimal, production-aligned observability layer**
focused on making the system measurable, debuggable, and operationally visible
without altering the core business logic.

This version does not introduce external monitoring systems,
but prepares the architecture for production-grade observability in V3.

**Key characteristics:**

- Spring Boot Actuator enabled in both API and Worker
- Prometheus-compatible metrics endpoint (`/actuator/prometheus`)
- Business-level metrics (orders created, events processed, retries, failures)
- Transport-aware metrics (SQS vs Outbox)
- Outbox backlog visibility via database-backed gauges
- Worker processing latency measurements
- Clear separation between business logic and observability layer

**Architectural significance:**

- Observability added without polluting the core domain
- Infrastructure concerns remain in adapters/infrastructure layer
- Transport pluggability is now observable via metrics
- Enables operational reasoning (backlog growth, retry storms, worker liveness)
- Establishes the foundation for production monitoring in V3

📄 Documentation:
- [V2.6 – Minimal Observability Layer](docs/observability/README_OBSERVABILITY.md)

---

### Planned Deployments

#### 🔜 V3 – Production-like Cloud Architecture
Planned.

- ECS / Fargate
- Application Load Balancer (ALB)
- RDS PostgreSQL
- Auto-scaling workers
- Metrics, logs, and alerts
- CI/CD basics

📄 Documentation:
- `docs/prod/README_V3.md` (coming soon)