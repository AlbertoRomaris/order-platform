# AWS V3 – Operational Runbook

This runbook documents how to:

- Provision infrastructure
- Verify deployment
- Validate end-to-end functionality
- Safely destroy resources to avoid AWS charges

---

## 1. Prerequisites

- AWS CLI configured:

```bash
aws sts get-caller-identity
```

- Terraform installed
- AWS region configured (default: eu-west-1)
- Located in project root

---

## 2. Infrastructure Provisioning

### 2.1 Bootstrap (Terraform Backend)

Creates:
- S3 remote state bucket
- DynamoDB lock table

```bash
cd infra/aws-v3/bootstrap
terraform init
terraform apply
```

Verification:

```bash
terraform output
aws s3 ls | grep tfstate
aws dynamodb list-tables
```

---

### 2.2 Stack (Main Infrastructure)

Creates:
- VPC
- ECS Cluster
- API Service
- Worker Service
- ALB
- RDS
- SQS
- IAM Roles
- Monitoring
- Autoscaling

```bash
cd ../stack
terraform init -backend-config=backend.hcl
terraform apply
```

Verify outputs:

```bash
terraform output
```

---

## 3. Infrastructure Verification

### 3.1 ECS Services

```bash
aws ecs list-clusters
aws ecs list-services --cluster <CLUSTER_NAME>
aws ecs describe-services --cluster <CLUSTER_NAME> --services <API_SERVICE> <WORKER_SERVICE>
```

Expected:
- runningCount == desiredCount
- No failing deployments
- No repeated STOPPED tasks

---

### 3.2 ALB Health Check

```bash
curl -i http://<ALB_DNS>/actuator/health
```

Expected:

```json
HTTP 200
{"status":"UP"}
```

---

### 3.3 CloudWatch Logs

List log groups:

```bash
aws logs describe-log-groups | grep order-platform
```

Inspect latest worker logs:

```bash
aws logs describe-log-streams \
  --log-group-name "/ecs/<NAME_PREFIX>/worker" \
  --order-by LastEventTime \
  --descending
```

---

## 4. End-to-End Functional Validation

### 4.1 Create Order

```bash
curl -X POST http://<ALB_DNS>/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"test","amount":123}'
```

Expected behavior:

1. Order stored in RDS
2. Event published to SQS
3. Worker consumes message
4. Order state transitions
5. Message deleted from SQS

---

### 4.2 SQS Backlog Check

```bash
aws sqs get-queue-attributes \
  --queue-url "<QUEUE_URL>" \
  --attribute-names ApproximateNumberOfMessagesVisible ApproximateNumberOfMessagesNotVisible
```

Backlog should decrease after worker processing.

---

## 5. Deployment Verification (CI/CD)

After pushing to master:

### 5.1 Verify ECR Images

```bash
aws ecr list-images --repository-name <API_REPO>
aws ecr list-images --repository-name <WORKER_REPO>
```

Expected:
- Image tagged with commit SHA
- Stable deployment tag present

---

### 5.2 Verify ECS Rollout

```bash
aws ecs describe-services \
  --cluster <CLUSTER_NAME> \
  --services <API_SERVICE> <WORKER_SERVICE>
```

Expected:
- rolloutState = COMPLETED
- runningCount == desiredCount

---

## 6. Safe Destruction (Cost Control)

### 6.1 Destroy Stack First

```bash
cd infra/aws-v3/stack
terraform destroy
```

Notes:
- force_delete = true enabled for ECR
- RDS configured with skip_final_snapshot = true

---

### 6.2 Destroy Bootstrap

```bash
cd ../bootstrap
terraform destroy
```

If S3 bucket destruction fails due to versioning,
delete object versions before retrying.

---

## 7. Final Cost-Safety Checklist

Verify no resources remain:

```bash
aws ecs list-clusters
aws rds describe-db-instances
aws sqs list-queues
aws elbv2 describe-load-balancers
aws ecr describe-repositories
aws logs describe-log-groups
```

Also check manually:
- RDS snapshots
- Elastic IPs
- NAT Gateways (if ever used)

---

## Operational Guarantees

This runbook ensures:
- Reproducible provisioning
- Deterministic teardown
- No orphaned AWS resources
- Controlled cost exposure
- Verifiable production-like runtime