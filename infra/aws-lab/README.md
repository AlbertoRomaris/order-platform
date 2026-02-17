# AWS Lab (V2.5) – EC2 runtime + SQS

This folder documents how to run the **Order Platform** (API + Worker + Postgres) on a single EC2 instance, using **AWS SQS** as the event queue (real AWS, not LocalStack).

> Goal: portfolio-grade proof that the system works end-to-end on AWS with IAM Role (no access keys on the instance).

## What gets deployed
- **EC2** (Docker runtime)
- **SQS main queue** + **DLQ**
- **IAM Role** attached to EC2 (SQS permissions)
- **Security Group** allowing:
    - SSH (22) from your public IP
    - API (8080) from your public IP

## Prerequisites (local machine)
- Terraform
- AWS CLI configured (to run terraform and SQS checks)
- SSH key pair (`order-platform-lab.pem`) with correct permissions

## 1) Provision infrastructure (Terraform)
From:
`infra/aws-lab`

```bash
terraform fmt
terraform validate
terraform apply
```

After apply:

- Note the EC2 public IP
- Note the SQS queue URL output

You can retrieve outputs again at any time with:

    terraform output

Example output:

    order_events_queue_url = "https://sqs.eu-west-1/ACCOUNT_ID/order-platform-aws-lab-order-events"
    order_events_dlq_url   = "https://sqs.eu-west-1/ACCOUNT_ID/order-platform-aws-lab-order-events-dlq"

You can verify the EC2 public IP in:

- AWS Console → EC2 → Instances
- Or via Terraform:

  terraform state show aws_instance.ec2


## Connect to EC2

From your local machine:

    ssh -i ./order-platform-lab.pem ec2-user@<EC2_PUBLIC_IP>

Verify that the IAM Role is correctly attached:

    aws sts get-caller-identity

Expected result:
- The ARN should show an assumed role
- The role name should match the Terraform-created EC2 role

Verify that SQS access works:

    aws sqs list-queues --region eu-west-1

You should see:

- order-platform-aws-lab-order-events
- order-platform-aws-lab-order-events-dlq

This confirms:

- The IAM role is correctly attached
- The instance can access SQS
- No access keys are required on the EC2 instance  

## 2) Prepare the EC2 instance (Docker)

Connect to the instance:

```bash
ssh -i ./order-platform-lab.pem ec2-user@<EC2_PUBLIC_IP>
```

Update packages and ensure Docker is installed and running:
```bash
sudo dnf update -y
sudo dnf install docker -y
sudo systemctl enable docker
sudo systemctl start docker
```

Allow the ec2-user to use Docker without sudo:

```bash
sudo usermod -aG docker ec2-user
exit
```

Reconnect (group membership applies after reconnect):
```bash
ssh -i ./order-platform-lab.pem ec2-user@<EC2_PUBLIC_IP>
docker --version
```

## 3) Build images locally and transfer to EC2 (simple approach)

This lab uses a simple, no-registry approach: build locally, export as tar, and copy to EC2 with SCP.

From the repo root (local machine), build both images:

```bash
docker build -t order-platform-api ./services/api
docker build -t order-platform-worker ./services/worker
```

Export images to tar files:
```bash
docker save -o order-platform-api.tar order-platform-api
docker save -o order-platform-worker.tar order-platform-worker
```

Copy them to the EC2 instance:

```bash
scp -i ./order-platform-lab.pem ./order-platform-api.tar ec2-user@<EC2_PUBLIC_IP>:/home/ec2-user/
scp -i ./order-platform-lab.pem ./order-platform-worker.tar ec2-user@<EC2_PUBLIC_IP>:/home/ec2-user/
```

On the EC2 instance, load the images:

```bash
docker load -i /home/ec2-user/order-platform-api.tar
docker load -i /home/ec2-user/order-platform-worker.tar
docker images | grep order-platform
```

## 4) Run API + Worker + Postgres on EC2 (docker compose)

Create the compose file on the EC2 instance:

```bash
cat > docker-compose.aws-lab.yml <<'YAML'
services:
postgres:
image: postgres:16
container_name: order-platform-postgres
environment:
POSTGRES_DB: orderdb
POSTGRES_USER: orderuser
POSTGRES_PASSWORD: orderpass123
ports:
- "5433:5432"
volumes:
- order_platform_pgdata:/var/lib/postgresql/data
healthcheck:
test: ["CMD-SHELL", "pg_isready -U orderuser -d orderdb"]
interval: 5s
timeout: 3s
retries: 10

api:
image: order-platform-api:latest
container_name: order-platform-api
depends_on:
postgres:
condition: service_healthy
ports:
- "8080:8080"
environment:
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/orderdb
SPRING_DATASOURCE_USERNAME: orderuser
SPRING_DATASOURCE_PASSWORD: orderpass123
AWS_REGION: eu-west-1
AWS_SQS_QUEUEURL: https://sqs.eu-west-1.amazonaws.com/<ACCOUNT_ID>/order-platform-aws-lab-order-events

worker:
image: order-platform-worker:latest
container_name: order-platform-worker
depends_on:
postgres:
condition: service_healthy
ports:
- "8081:8081"
environment:
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/orderdb
SPRING_DATASOURCE_USERNAME: orderuser
SPRING_DATASOURCE_PASSWORD: orderpass123
AWS_REGION: eu-west-1
AWS_SQS_QUEUEURL: https://sqs.eu-west-1.amazonaws.com/<ACCOUNT_ID>/order-platform-aws-lab-order-events
ORDER_WORKER_FAILUREPROBABILITY: "0.0"
ORDER_WORKER_MAXRETRIES: "3"
ORDER_WORKER_RETRYDELAYMS: "1000"

volumes:
order_platform_pgdata:
YAML
```

Start the stack:

```bash
docker compose -f docker-compose.aws-lab.yml up -d
docker ps
```


Follow logs (optional):

```bash
docker logs -f order-platform-api
docker logs -f order-platform-worker
```

## 5) End-to-end verification

### 5.1 Create an order (from your local machine)

Replace <EC2_PUBLIC_IP>:

```bash
curl -X POST "http://<EC2_PUBLIC_IP>:8080/orders" -H "Content-Type: application/json" -d '{}'
```

You should receive an orderId.

### 5.2 Check SQS queue metrics (local machine)

Immediately after publishing you may see messages in-flight:

```bash
aws sqs get-queue-attributes \
--queue-url "https://sqs.eu-west-1.amazonaws.com/<ACCOUNT_ID>/order-platform-aws-lab-order-events" \
--attribute-names ApproximateNumberOfMessages ApproximateNumberOfMessagesNotVisible
```

Expected:
- ApproximateNumberOfMessagesNotVisible may become 1 briefly while the worker is processing.

### 5.3 Confirm worker processed and DB updated (on EC2)

Check worker logs:

```bash
docker logs --tail 200 order-platform-worker
```


Check the DB:

```bash
docker exec -it order-platform-postgres psql -U orderuser -d orderdb \
-c "select id, status, created_at, updated_at from orders order by created_at desc limit 5;"
```

Expected:
- Latest order status should be PROCESSED

### Notes / Troubleshooting

- EC2 instance too slow: t3.micro can be painfully slow for Docker + Java + Postgres. Consider using t3.small.
- SQS DNS errors (UnknownHostException) can appear if the instance is under heavy CPU contention or networking is unstable.
- If SSH stops responding, wait a bit and retry; CPU starvation can temporarily block responsiveness.

### Stop / cleanup

Stop containers on EC2:

```bash
docker compose -f docker-compose.aws-lab.yml down
```

Destroy AWS resources (local machine, from infra/aws-lab):

```bash
terraform destroy
```