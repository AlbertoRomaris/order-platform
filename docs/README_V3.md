# V3 – Production-Like Cloud Architecture on AWS

## Executive Summary

Version 3 transforms the Event-Driven Order Platform from a cloud-ready distributed system (V2) into a production-like, fully managed cloud architecture running on AWS.

No new business capabilities are introduced in this version.

Instead, V3 validates that:

- The domain architecture survives real cloud constraints.
- Infrastructure can be provisioned reproducibly via Terraform.
- The system scales horizontally under load.
- Failures are observable and actionable.
- Security boundaries are enforced through IAM and network isolation.
- Deployments are automated and traceable.
- Operational behavior is measurable and predictable.

V2 proved architectural correctness.
V3 proves operational readiness.

---

# Architectural Continuity

V3 does not alter the core architecture introduced in V2.

The following principles remain intact:

- Database as the source of truth.
- Ports & Adapters (Hexagonal Architecture).
- Framework-agnostic core module.
- No AWS SDK dependencies in the domain layer.
- Explicit state machine for order lifecycle.
- Deterministic retry semantics.
- Dual DLQ separation (business vs infrastructure).
- End-to-end Correlation ID propagation.

The only runtime transport change is:

```
worker.mode = sqs-consumer
```

This changes how events are delivered — not how business logic behaves.

The core domain remains unaware of:

- ECS
- SQS
- RDS
- CloudWatch
- IAM

Cloud infrastructure is treated strictly as an adapter.

---

# System Architecture Overview

## High-Level Topology

```
Internet
    ↓
Application Load Balancer (public subnets)
    ↓
ECS Fargate – API Service
ECS Fargate – Worker Service
    ↓
Amazon RDS PostgreSQL (private subnets)
    ↓
Amazon SQS (Main Queue)
    ↓
SQS Dead Letter Queue
```

---

## Responsibility Boundaries

| Component | Responsibility |
|------------|----------------|
| ALB | Public HTTP entry, TLS termination, health checks |
| ECS API | Command handling, persistence, event publication |
| ECS Worker | Asynchronous order processing |
| RDS | Persistent state and transactional integrity |
| SQS | Decoupled event transport and retry mechanism |
| SQS DLQ | Infrastructure failure isolation |
| Database DLQ | Business-level failure persistence |

Each layer has a single clear responsibility.

---

# Infrastructure as Code Strategy

All infrastructure is provisioned via Terraform under:

```
infra/aws-v3
```

The infrastructure is intentionally separated into two layers.

---

## Bootstrap Layer

Purpose: enable safe Terraform collaboration.

Resources:

- S3 bucket for remote Terraform state.
- Versioning enabled.
- Server-side encryption enabled.
- Public access blocked.
- DynamoDB table for state locking.

### Design Rationale

Remote state with locking ensures:

- No concurrent state corruption.
- Deterministic apply operations.
- State version recovery.
- No reliance on local state files.

Without this layer, infrastructure changes would not be production-safe.

---

## Stack Layer

The stack provisions runtime infrastructure:

- VPC and networking
- ECS cluster and services
- RDS database
- SQS queues
- IAM roles and policies
- CloudWatch alarms and metrics
- SNS notifications
- Autoscaling policies

Infrastructure is declarative, version-controlled, and reproducible.

---

# Networking & Security Architecture

## VPC Design

- Single VPC.
- Public subnets for ALB.
- Private subnets for RDS.
- ECS tasks may run in public subnets (cost-aware tradeoff).

### Cost Tradeoff: No NAT Gateway (Development Environment)

NAT Gateway is intentionally omitted to reduce recurring costs.

Implications:

- ECS tasks may require public IP.
- Outbound internet access is allowed.
- Security enforced via Security Groups.

This is acceptable for development environments.
A production environment would typically include:

- NAT Gateway
- Private ECS tasks
- Stricter egress control

---

## Security Group Model

Traffic flow:

Internet → ALB → ECS → RDS

Rules enforced:

- ALB allows inbound HTTP/HTTPS from internet.
- ECS API allows inbound only from ALB.
- ECS Worker has no public ingress.
- RDS allows inbound only from ECS Security Groups.
- No public DB access.

Security is enforced at network boundaries.

---

# Compute Layer

## ECS Fargate

Chosen over EC2 because:

- No instance management.
- No patching responsibility.
- Horizontal scaling built-in.
- Clear separation of infrastructure and workload.

Tradeoff:

- Slightly higher cost than EC2.
- Less granular tuning.

Appropriate for production-like managed architecture.

---

## API Service

Characteristics:

- Stateless.
- Health check endpoint exposed.
- Publishes events to SQS.
- Uses IAM Task Role for SendMessage permission.

The API never processes business events.

---

## Worker Service

Characteristics:

- Stateless.
- Consumes SQS via long polling.
- Horizontally scalable.
- Deletes message only after successful processing.
- Uses IAM Task Role for Receive/Delete permission.

Designed to be duplicated safely.

---

# Data Layer

## Amazon RDS PostgreSQL

Chosen over containerized database because:

- Managed backups.
- Automated patching.
- Durable storage.
- Storage scaling.
- Operational reliability.

RDS runs in private subnets.
Not publicly accessible.

Flyway migrations run at application startup.

Database remains the single source of truth.

---

# Messaging Architecture

## SQS Main Queue

Selected because:

- Fully managed.
- Scales automatically.
- Built-in retry via visibility timeout.
- Decouples producer and consumer.
- No broker maintenance required.

Standard queue chosen over FIFO:

- Higher throughput.
- Order ordering not required for this domain.
- Lower operational constraints.

---

## SQS DLQ

Configured with:

- maxReceiveCount.
- Redrive policy.

Used for infrastructure-level failure isolation.

Messages in DLQ require operational inspection.

---

# Dual DLQ Model

Two distinct failure domains are preserved.

## Infrastructure DLQ (SQS)

Handles:

- Worker crashes.
- Visibility timeout expirations.
- Unexpected runtime failures.

AWS-managed.

---

## Business DLQ (Database)

Handles:

- Retry exhaustion.
- Domain-specific failure classification.

Domain-managed.

Separation ensures:

- No cloud leakage into core.
- Clear operational ownership.
- Clean architectural boundaries.

---

# Runtime Behavior

## API Flow

1. Receive request.
2. Validate input.
3. Persist order (PENDING).
4. Publish event to SQS.
5. Return response.

API does not process orders.

---

## Worker Flow

1. Poll SQS.
2. Deserialize event.
3. Inject Correlation ID.
4. Execute use case.
5. Transition order state.
6. Delete SQS message.

On failure:

- Message reappears.
- Retry managed by SQS.
- After threshold → DLQ.

Worker is idempotent and stateless.

---

# Observability Architecture

Observability spans multiple layers:

- Logs
- Metrics
- Alarms
- Events
- Dashboard

---

# Logging Strategy

- Structured logs.
- Correlation ID preserved.
- Separate Log Groups per service.
- Retention: 7 days (cost-aware decision).

Tradeoff:

- Lower retention reduces cost.
- Not suitable for forensic-grade audit.

---

# Application Metrics

Derived via Log Metric Filters:

- ApiErrorCount
- WorkerFailedCount

Used to detect:

- Broken deployments.
- Business failure spikes.
- Unexpected runtime exceptions.

---

# Infrastructure Metrics

## ALB

- HTTPCode_ELB_5XX_Count
- HTTPCode_Target_5XX_Count
- TargetResponseTime p95
- RequestCount

p95 chosen over average to surface tail latency.

---

## SQS

- ApproximateNumberOfMessagesVisible
- ApproximateNumberOfMessagesNotVisible

Used for:

- Backlog monitoring.
- Scaling triggers.
- Throughput validation.

---

## RDS

- CPUUtilization
- FreeStorageSpace

Used to detect:

- Saturation.
- Resource exhaustion.

---

# Autoscaling Model

Worker scaling driven by backlog.

Scale-out when backlog increases.
Scale-in when backlog remains empty.

Step scaling selected over target tracking for explicit threshold control.

Min capacity: 1  
Max capacity: 5

Tradeoff:

- Manual threshold tuning required.
- More explicit control than target tracking.

---

# Event-Based Health Monitoring

EventBridge monitors:

- ECS Task STOPPED
- ECS Deployment failures
- Service instability

Metrics alone cannot capture crash loops reliably.

Event-driven monitoring closes that gap.

---

# Failure Detection Matrix

| Failure | Detection |
|----------|----------|
| Worker crash | ECS STOPPED event |
| Worker stuck | Sustained backlog alarm |
| DLQ growth | DLQ alarm |
| Business retry exhaustion | WorkerFailedCount |
| API broken deploy | ALB 5xx |
| DB overload | RDS CPU alarm |
| Disk exhaustion | FreeStorage alarm |

This provides multi-layer detection coverage.

---

# Deployment Automation

## Authentication Model

- OIDC provider configured.
- IAM role scoped to repository and branch.
- No static AWS credentials stored.
- Short-lived STS credentials.

Reduces blast radius and secret management risk.

---

## Build Strategy

- Monorepo-aware multi-stage Docker builds.
- Maven reactor.
- Images tagged with commit SHA.
- Deterministic artifacts.

Ensures traceability and rollback capability.

---

## Deployment Strategy

- Push to master triggers pipeline.
- Image pushed to ECR.
- ECS force-new-deployment.
- Rolling replacement.
- Health checks gate deployment success.

Deployment validated only when:

- rolloutState = COMPLETED
- runningCount == desiredCount

---

# Security Model

Security enforced at:

## Identity Layer

- Separate task roles.
- Least privilege SQS permissions.
- Restricted OIDC trust policy.

## Network Layer

- RDS private.
- No public DB.
- Controlled ingress via ALB.
- No worker public exposure.

## Secret Management

- No secrets committed.
- Runtime configuration externalized.
- IAM over static credentials.

---

# Known Limitations

The system intentionally does not include:

- Multi-region failover.
- Blue/green deployment.
- Canary releases.
- WAF integration.
- Secrets rotation automation.
- Distributed tracing.
- Kubernetes orchestration.

These are conscious exclusions to preserve clarity and scope.

---

# Risk Considerations

- Single-region deployment.
- No cross-AZ redundancy beyond AWS defaults.
- Log retention limited.
- Manual DLQ inspection required.
- No rate limiting at ALB.

Risks are understood and acceptable for current scope.

---

# Operational Characteristics Achieved

The system demonstrates:

- Managed compute.
- Managed database.
- Managed messaging.
- Horizontal scaling.
- Deterministic infrastructure provisioning.
- Secure CI/CD authentication.
- Failure detection coverage.
- Workload-driven scaling.
- Clean separation of domain and cloud concerns.

---

# Final State

After V3, the Event-Driven Order Platform:

- Runs on fully managed AWS services.
- Preserves clean hexagonal architecture.
- Separates business and infrastructure failure domains.
- Scales horizontally with workload.
- Detects failures across layers.
- Is reproducible via Terraform.
- Deploys automatically via CI/CD.

V2 validated architecture.

V3 validates cloud operability.