# V3 – Production-Like Cloud Architecture

## Overview

Version 3 evolves the platform from a validated cloud runtime (V2.5)
into a **production-like, cloud-native architecture on AWS**.

This version does not introduce new business features.

Instead, it demonstrates:

- Cloud-native deployment patterns
- Managed infrastructure services
- Secure runtime configuration
- Horizontal scalability
- Infrastructure as Code (Terraform)
- Operational readiness

The core domain remains unchanged and framework-agnostic.

---

# Objectives of V3

V3 exists to demonstrate production-level architectural thinking.

Specifically, V3 proves that:

- The same clean core can run on fully managed cloud infrastructure
- Infrastructure concerns are isolated from business logic
- The system can scale horizontally
- Runtime behavior is observable and operable
- Deployment is reproducible via Infrastructure as Code
- Cloud security best practices are applied

This is not about adding complexity.
It is about demonstrating architectural maturity.

---

# Architectural Philosophy

The principles from V1 and V2 still apply:

- Database is the source of truth
- Core is framework-agnostic
- No cloud SDK logic in domain
- Ports & Adapters architecture
- Explicit state machine
- Retry + DLQ semantics
- Correlation ID end-to-end
- Observability without polluting domain

V3 extends those principles into a real cloud environment.

---

# High-Level Architecture (V3)

```
Internet
|
v
Application Load Balancer (public subnets)
|
v
ECS Fargate (API Service - private subnets)
ECS Fargate (Worker Service - private subnets)
|
v
Amazon RDS PostgreSQL (private subnets)
|
v
Amazon SQS (Main Queue)
|
v
SQS Dead Letter Queue
```

---

# Key Differences vs V2.5

| V2.5 | V3 |
|------|----|
| Single EC2 | Fully managed ECS Fargate |
| Postgres container | Managed RDS PostgreSQL |
| No load balancer | ALB with health checks |
| Manual scaling | Auto-scaling workers |
| Minimal observability | CloudWatch dashboards & alarms |
| Manual deployment | CI/CD pipeline |

---

# Core Runtime Mode in V3

In V3, the primary transport mode is:

`worker.mode = sqs-consumer`


Why:

- Horizontally scalable
- Managed retry semantics
- Real cloud-native transport
- Aligns with AWS production patterns

The database outbox mode remains available for local execution,
but is not the primary runtime in V3.

---

# V3 Phases

V3 is implemented incrementally.

Each phase has a clear Definition of Done.

---

## V3.0 – Infrastructure Foundation

### Scope

- New Terraform stack (`infra/aws-v3`)
- Remote Terraform backend (S3 + DynamoDB lock)
- ECR repositories (API + Worker)
- ECS Cluster
- CloudWatch Log Groups
- Naming and tagging strategy

### Why

Establish reproducible, production-grade infrastructure management.

### Definition of Done

- `terraform apply` creates:
    - ECR repos
    - ECS cluster
    - Log groups
- Remote backend configured
- No state stored locally

---

## V3.1 – API Service on ECS + ALB

### Scope

- VPC (public + private subnets)
- Application Load Balancer
- Target group
- ECS Fargate service (API)
- Health checks (`/actuator/health`)
- Security Groups (least privilege)
- Logs flowing to CloudWatch

### Why

Demonstrates production-standard API deployment.

### Definition of Done

- Public ALB URL returns `200 OK`
- ECS service stable
- Logs visible in CloudWatch

---

## V3.2 – RDS PostgreSQL

### Scope

- RDS PostgreSQL (private subnets)
- DB Subnet Group
- Security Group (ECS → RDS only)
- Secrets in SSM Parameter Store or Secrets Manager
- Flyway migrations executed at startup

### Why

Replaces containerized database with managed service.

### Definition of Done

- API connects to RDS
- Migrations applied
- RDS not publicly accessible

---

## V3.3 – Worker on ECS + SQS End-to-End

### Scope

- SQS main queue
- SQS DLQ with redrive policy
- ECS Worker service (Fargate)
- IAM Task Roles:
    - API: SendMessage
    - Worker: Receive/Delete
- End-to-end processing pipeline

### Why

Demonstrates event-driven architecture at cloud scale.

### Definition of Done

- Create order via ALB
- Event appears in SQS
- Worker processes event
- Order state transitions in RDS
- DLQ configured correctly

---

## V3.4 – Monitoring & Alerting

### Scope

- CloudWatch metrics (infra + application)
- Log-based custom metrics
- CloudWatch alarms (operational & scaling)
- SNS email notifications
- EventBridge alerts for ECS worker failures
- Autoscaling observability tied to SQS backlog

---

### Why

Production-grade systems require:

- Early detection of failures
- Backlog awareness
- Worker health visibility
- Infrastructure degradation alerts
- Elastic scaling tied to workload

This version introduces real operational observability, not just logs.

---

## 1️⃣ Application Metrics (Log-Based Custom Metrics)

**Namespace:** `order-platform-dev-v3/App`

### 1.1 API Error Count

- Source: CloudWatch Log Metric Filter
- Pattern: `" ERROR "`
- Metric Name: `ApiErrorCount`

#### Alarm: `api-error-spike`

| Property | Value |
|----------|--------|
| Statistic | Sum |
| Period | 60s |
| Threshold | > 5 |
| Action | SNS alert |

**Purpose**

Detects sudden spikes of API errors (5+ errors per minute).

Used to detect:

- Broken deployment
- Database failures
- Runtime exceptions
- Dependency outages

---

### 1.2 Worker Failed Count

- Source: Log Metric Filter
- Pattern: `FAILED`
- Metric Name: `WorkerFailedCount`

#### Alarm: `worker-failed`

| Property | Value |
|----------|--------|
| Threshold | > 0 |
| Period | 60s |
| Action | SNS |

**Purpose**

Detects business-level failures:

- Terminal failures
- DLQ-bound failures
- SQS delete failures

---

## 2️⃣ SQS Metrics

- Namespace: `AWS/SQS`
- Metric: `ApproximateNumberOfMessagesVisible`

### 2.1 Autoscaling – Scale Out

Alarm: `scaleout-sqs-backlog-high`

| Property | Value |
|----------|--------|
| Threshold | > 10 |
| Evaluation | 2 periods |
| Period | 60s |
| Action | Scale worker +1 |

When backlog grows above 10 → worker scales out.

---

### 2.2 Autoscaling – Scale In

Alarm: `scalein-sqs-backlog-empty`

| Property | Value |
|----------|--------|
| Threshold | ≤ 0 |
| Evaluation | 5 periods |
| Period | 60s |
| Action | Scale worker -1 |

When backlog remains empty → worker scales in.

⚠️ This alarm being in `ALARM` when backlog = 0 is expected behavior.

---

### 2.3 Backlog High Sustained (Operational Alarm)

Alarm: `queue-backlog-high-sustained`

Purpose:

Detects sustained backlog even after scaling.

Identifies:

- Worker misconfiguration
- Throughput bottlenecks
- SQS processing issues

Action: SNS notification.

---

### 2.4 DLQ Non Empty

Alarm: `dlq-nonempty`

- Threshold: > 0

Triggers when:

- Messages exceed `maxReceiveCount`
- Worker cannot process successfully

This is a critical operational alert.

---

## 3️⃣ ECS Worker Health (Event-Based Monitoring)

Instead of relying on `RunningTaskCount`, worker lifecycle is monitored using EventBridge.

### 3.1 ECS Task STOPPED

Triggers when:

- Worker task crashes
- Task OOM
- Deployment failure
- Manual stop
- Runtime crash

Event Type:

- `ECS Task State Change`
- `lastStatus = STOPPED`

Action: SNS notification.

---

### 3.2 ECS Service / Deployment Events

Triggers when:

- Service cannot place tasks
- Deployment fails
- Rollback occurs
- Service instability detected

Event Types:

- `ECS Service Action`
- `ECS Deployment State Change`

Action: SNS notification.

---

## 4️⃣ ALB Metrics

Namespace: `AWS/ApplicationELB`

### 4.1 ALB 5xx

Alarm: `alb-5xx`

Detects:

- API container failures
- Target crashes
- Upstream application errors

---

### 4.2 Target Group 5xx

Alarm: `tg-5xx`

Detects:

- Bad responses from API container
- Application runtime failures

---

### 4.3 Latency p95

Alarm: `alb-latency-p95`

Detects:

- Performance degradation
- Slow database responses
- Resource exhaustion

---

## 5️⃣ RDS Metrics

Namespace: `AWS/RDS`

### 5.1 CPU High

Alarm: `rds-cpu-high`

Detects:

- Database overload
- Query inefficiency
- Capacity saturation

---

### 5.2 Free Storage Low

Alarm: `rds-free-storage-low`

Detects:

- Impending disk exhaustion
- Risk of database outage

---

## 6️⃣ SNS Alerting

All operational alarms and EventBridge rules publish to:

`order-platform-dev-v3-alerts`

Subscription:

- Email (manually confirmed)

---

## 7️⃣ Autoscaling Configuration

ECS Service: `order-platform-dev-v3-worker-svc`

| Property | Value |
|----------|--------|
| Min Capacity | 1 |
| Max Capacity | 5 |
| Adjustment Type | Step Scaling |
| Cooldown | 30–120s |

Scaling is driven by SQS backlog.

---

## 8️⃣ Failure Coverage Matrix

| Failure Scenario | Detection Mechanism |
|------------------|--------------------|
| Worker crash | EventBridge STOPPED event |
| Worker deployment failure | ECS Service / Deployment event |
| Worker stuck (not processing) | Backlog sustained alarm |
| Business processing failure | WorkerFailedCount |
| Messages dead-lettered | DLQ alarm |
| API runtime errors | ApiErrorCount |
| API broken deploy | ALB 5xx |
| Database overload | RDS CPU |
| Database storage risk | FreeStorage alarm |
| Throughput increase | Autoscaling triggered |

---

## 9️⃣ Observability Level Achieved

The system now includes:

- Reactive alerting
- Proactive degradation detection
- Elastic scaling tied to workload
- Infrastructure + application failure visibility
- Operational maturity patterns

After V3.4 + Monitoring:

- Core architecture stable
- Infra production-like
- Async workflow resilient
- Elastic worker scaling
- Real alerting
- Real failure detection

This system now resembles a small production backend.

## ️ 🔟 CloudWatch Dashboard (Operational Visualization)

### Overview

A production-grade CloudWatch Dashboard is provisioned via Terraform:

`order-platform-dev-v3-dashboard`

The dashboard provides real-time visualization of:

- Queue backlog health
- Worker capacity vs desired state
- API error signals
- Infrastructure resource pressure
- Traffic patterns
- Throughput trends

It complements alerting by enabling:

- Root cause investigation
- Capacity analysis
- Scaling validation
- Failure correlation across layers

---

## 10.1 SQS Monitoring

### SQS Backlog (Visible / InFlight)

**Namespace:** `AWS/SQS`

Metrics:

- `ApproximateNumberOfMessagesVisible`
- `ApproximateNumberOfMessagesNotVisible`

Purpose:

- Detect processing bottlenecks
- Observe worker throughput vs inflow
- Validate autoscaling behavior

Interpretation:

| Signal | Meaning |
|--------|----------|
| Visible increasing | Worker under-provisioned |
| NotVisible high | Messages being actively processed |
| Visible sustained | Scaling misconfigured or worker stuck |

---

### DLQ Messages (Visible)

Metric: `ApproximateNumberOfMessagesVisible`

Purpose:

- Immediate visibility of terminal failures
- Validate retry strategy behavior
- Confirm DLQ alarm triggering

DLQ > 0 is considered a critical operational signal.

---

## 10.2 Load Balancer Observability

### HTTP 5XX (ALB + TargetGroup)

**Namespace:** `AWS/ApplicationELB`

Metrics:

- `HTTPCode_ELB_5XX_Count`
- `HTTPCode_Target_5XX_Count`

Purpose:

- Detect broken deployments
- Identify container-level failures
- Distinguish infra-level vs application-level errors

---

### ALB Latency p95 (TargetResponseTime)

Statistic: `p95`

Purpose:

- Performance degradation detection
- Database slowness correlation
- Resource pressure identification

p95 is used instead of average to surface tail latency.

---

### ALB RequestCount

Metric: `RequestCount`

Purpose:

- Traffic pattern analysis
- Correlate load with scaling events
- Detect abnormal traffic spikes

---

## 10.3 RDS Observability

### RDS CPUUtilization

**Namespace:** `AWS/RDS`

Purpose:

- Detect overload conditions
- Identify inefficient queries
- Capacity planning signal

---

### RDS FreeStorageSpace

Purpose:

- Prevent disk exhaustion
- Detect abnormal storage growth

Disk exhaustion is considered a critical failure scenario.

---

## 10.4 ECS Worker Capacity Monitoring

### Desired vs Running Tasks

**Namespace:** `ECS/ContainerInsights`

Metrics:

- `DesiredTaskCount`
- `RunningTaskCount`

Purpose:

- Validate autoscaling behavior
- Detect failed deployments
- Identify placement issues

Interpretation:

| Scenario | Meaning |
|----------|----------|
| Desired > Running | ECS placement issue or container crash |
| Desired increasing | Autoscaling triggered |
| Running unstable | Worker crash loop |

This provides operational visibility beyond EventBridge alerts.

---

## 10.5 Application-Level Metrics

**Namespace:** `order-platform-dev-v3/App`

### WorkerActivity

Custom metric derived from application logs.

Purpose:

- Confirm worker is actively processing messages
- Validate throughput
- Correlate processing with SQS backlog

This metric complements SQS backlog signals.

Future improvements may include:

- ApiErrorCount
- WorkerFailedCount
- OrdersProcessed
- OrdersFailed

---

## 10.6 SQS Throughput

### NumberOfMessagesSent

Purpose:

- Monitor event production rate
- Correlate traffic with processing capacity
- Validate end-to-end flow

---

## Dashboard Design Principles

The dashboard follows production monitoring principles:

- Infrastructure and application signals combined
- Backlog + Throughput + Capacity triad
- Tail latency preferred over averages
- Visual confirmation of scaling behavior
- Clear separation of layers (ALB / ECS / RDS / SQS / App)

---

## Observability Model

The system now includes:

- Real-time visualization (Dashboard)
- Reactive alerting (CloudWatch Alarms)
- Event-driven health detection (EventBridge)
- Elastic scaling observability
- Failure correlation capability

This ensures both:

- Immediate alerting
- Investigative depth

---

## Operational Maturity

With the dashboard in place, operators can:

- Identify root cause in minutes
- Confirm autoscaling effectiveness
- Detect saturation early
- Correlate failures across components
- Validate resilience mechanisms

The platform now includes production-grade operational observability.




## V3.6 – CI/CD Pipeline

### Scope

- GitHub Actions workflow
- Docker build
- Push to ECR
- ECS rolling update
- Preferably OIDC-based AWS authentication

### Why

Completes the production lifecycle.

### Definition of Done

- Push to main → deploy new version
- ECS service updates successfully

---

# Security Model in V3

- No static AWS credentials
- IAM Task Roles for ECS services
- Security groups with least privilege
- RDS in private subnets
- No direct database exposure
- No hardcoded secrets

---

# Cost Considerations

To avoid unnecessary AWS charges:

- Proper resource tagging
- Minimal instance sizes
- Controlled NAT usage
- Log retention limits
- Explicit resource destruction when not needed

V3 is designed to remain within AWS free-tier or credit budgets.

---

# Operational Capabilities Achieved in V3

With V3, the platform demonstrates:

- Managed compute (ECS Fargate)
- Managed database (RDS)
- Managed messaging (SQS)
- Horizontal scaling
- Infrastructure as Code
- Secure credential management
- Health checks & load balancing
- Cloud-native observability
- Deployment automation

This elevates the project from
“cloud-ready architecture”
to
“production-like cloud system”.

---

# What V3 Intentionally Does NOT Add

- No distributed tracing
- No OpenTelemetry
- No Kafka
- No Kubernetes
- No multi-region deployment
- No multi-account strategy

These belong to future evolutions.

V3 focuses on correctness, clarity, and architectural discipline.

---

# Final State After V3

After completing V3, the Event-Driven Order Platform:

- Runs on fully managed AWS services
- Preserves clean hexagonal architecture
- Demonstrates distributed system design
- Is horizontally scalable
- Is operationally visible
- Is reproducible via Terraform
- Is deployable via CI/CD