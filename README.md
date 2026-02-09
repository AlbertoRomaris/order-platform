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

#### 🟡 V2 – Cloud-ready Architecture (Outbox + Separate Worker)
Architecturally complete.

- Transactional Outbox pattern
- Separate worker service
- Database-backed asynchronous processing
- Durable retries and failure handling
- Manual reprocessing via new outbox events
- No cloud provider dependency

📄 Documentation:
- [V2 Architecture & Design](docs/README_V2.md)

---

### Planned Deployments

#### 🔜 AWS Lab – Minimal Cloud Deployment
Planned.

- Single EC2 instance
- Dockerized API and Worker
- AWS SQS replacing database outbox polling
- Infrastructure as Code (Terraform)
- Low-cost, learning-oriented setup

📄 Documentation:
- `docs/aws/README_AWS_LAB.md` (coming soon)

---

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