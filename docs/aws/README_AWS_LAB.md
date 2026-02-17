# AWS Lab – Minimal Cloud Deployment (V2.5)

## Overview

The AWS Lab extends Version 2 into a **real cloud runtime environment**.

The purpose of this lab is not to introduce complex AWS services,
but to validate that the architecture designed in V2 works correctly
when deployed on actual cloud infrastructure.

This deployment includes:

- A single EC2 instance
- Dockerized API and Worker services
- PostgreSQL running in-container
- AWS SQS as the external event transport
- Infrastructure provisioned via Terraform
- IAM Role-based authentication (no access keys on the instance)

The goal is to demonstrate:

- Cloud portability
- Infrastructure as Code
- Separation between business logic and infrastructure
- Secure credential handling
- End-to-end asynchronous processing in AWS

---

## Why This Lab Exists

V2 introduced a cloud-ready architecture:

- Transactional outbox
- Separate worker service
- SQS-compatible transport
- Runtime-configurable execution modes

However, until this lab, everything still ran locally.

This lab proves that:

- The system runs unmodified on AWS
- IAM Roles replace static credentials
- SQS behaves as expected under real network conditions
- Dockerized services can be deployed independently of the host
- Infrastructure is reproducible via Terraform

The focus remains architecture-first, infrastructure-second.

---

## Architectural Scope

This lab intentionally uses a minimal cloud setup:

```
Internet
|
v
EC2 (Docker runtime)
├── API container (Spring Boot)
├── Worker container (Spring Boot)
└── PostgreSQL container
|
v
AWS SQS (managed)
|
v
SQS DLQ
```

Important:

- The database remains the source of truth.
- SQS replaces the database outbox polling transport.
- Business logic remains unchanged.
- IAM Role provides credentials automatically.
- No AWS SDK logic exists inside the domain layer.

---

## What Gets Provisioned (Terraform)

The `infra/aws-lab` module provisions:

- EC2 instance (t3.micro / t3.small)
- Security Group (SSH + API access restricted to your IP)
- SQS main queue
- SQS Dead Letter Queue
- Redrive policy configuration
- IAM Role attached to EC2
- IAM Instance Profile

Infrastructure is fully declarative and reproducible.

---

## Security Model

This lab deliberately avoids:

- Access keys in environment variables
- Hardcoded credentials
- Local AWS profiles inside containers

Instead:

- The EC2 instance receives an IAM Role
- The AWS SDK automatically retrieves temporary credentials
- No secrets are stored inside the application

Verification:

```bash
aws sts get-caller-identity
```

Expected result:

- An assumed-role ARN
- No static credentials

This mirrors production-grade AWS patterns.

---

## Runtime Behavior (SQS Mode)

In AWS Lab, the worker runs with:

```bash
worker.mode = sqs-consumer
```

Behavioral implications:

- API publishes directly to SQS
- Worker polls SQS via long polling
- Messages are deleted only after successful processing
- Visibility timeout governs retry timing
- After maxReceiveCount, SQS moves the message to its DLQ

Business-level retries remain intact:

- The domain still enforces maxRetries
- Failed orders are still persisted in the database DLQ
- Infrastructure DLQ remains independent

This preserves architectural purity.

---


## Dual DLQ in AWS Context

Two independent failure layers exist:

### 1) Business DLQ (Database)

Represents:

- Domain-level failure
- Exhausted business retries
- Manual reprocessing eligibility

### 2) Infrastructure DLQ (SQS)

Represents:

- Transport-level failure
- Consumer crashes
- Poisoned messages
- Worker downtime

These layers are intentionally decoupled.

Manual reprocessing only affects the business DLQ.
SQS DLQ requires infrastructure-level intervention.

This mirrors real-world distributed systems.

---

## Operational Validation

End-to-end validation includes:

1. Creating an order via EC2 public IP
2. Observing SQS metrics
3. Observing worker logs
4. Verifying DB state transition
5. Optionally forcing failure to validate DLQ behavior

The architecture behaves identically to local execution,
with SQS acting purely as a transport layer.

---


## Performance Considerations

``t3.micro`` can experience:

- CPU throttling
- Network latency
- Slow container startup
- Temporary DNS resolution errors under load

This is expected in burstable instances.

For smoother operation, ``t3.small`` is recommended.

This lab intentionally remains low-cost and minimal.

---

## Current Status

AWS Lab successfully demonstrates:

- Infrastructure as Code (Terraform)
- EC2-based runtime
- IAM Role-based authentication
- SQS integration
- Worker separation
- End-to-end asynchronous processing
- Real AWS DLQ behavior

This completes the transition from:

- V1 → In-memory async
- V2 → Database-backed outbox
- V2.5 → Cloud transport with SQS

---

### Next Evolution

Future improvements may include:

- RDS PostgreSQL instead of containerized DB
- ECS instead of single EC2
- Application Load Balancer
- Horizontal worker scaling
- CloudWatch metrics and alarms
- OpenTelemetry tracing
- Structured JSON logs

The architecture is already prepared for these steps.