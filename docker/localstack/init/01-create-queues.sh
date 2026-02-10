#!/bin/sh
set -eu

echo "[localstack-init] (re)creating SQS queues..."

ENDPOINT_URL="http://localhost:4566"
REGION="${AWS_DEFAULT_REGION:-eu-west-1}"
ACCOUNT_ID="000000000000"

DLQ_NAME="order-events-dlq"
QUEUE_NAME="order-events"

# Helper: always call aws against local endpoint explicitly
awsls() {
  awslocal --endpoint-url="$ENDPOINT_URL" "$@"
}

DLQ_URL="$ENDPOINT_URL/$ACCOUNT_ID/$DLQ_NAME"
QUEUE_URL="$ENDPOINT_URL/$ACCOUNT_ID/$QUEUE_NAME"

delete_if_exists() {
  NAME="$1"
  URL="$2"
  if awsls sqs get-queue-url --queue-name "$NAME" >/dev/null 2>&1; then
    echo "[localstack-init] Deleting existing queue: $NAME"
    awsls sqs delete-queue --queue-url "$URL" >/dev/null 2>&1 || true
  fi
}

# Hard reset for reproducibility
delete_if_exists "$QUEUE_NAME" "$QUEUE_URL"
delete_if_exists "$DLQ_NAME" "$DLQ_URL"

echo "[localstack-init] Creating DLQ: $DLQ_NAME"
awsls sqs create-queue --queue-name "$DLQ_NAME" >/dev/null

DLQ_ARN="$(awsls sqs get-queue-attributes --queue-url "$DLQ_URL" --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)"
echo "[localstack-init] DLQ ARN: $DLQ_ARN"

echo "[localstack-init] Creating main queue: $QUEUE_NAME"
awsls sqs create-queue --queue-name "$QUEUE_NAME" >/dev/null

# Apply attributes using cli-input-json (most robust with awscli v1)
INPUT_JSON="/tmp/set-attrs.json"
cat > "$INPUT_JSON" <<EOF
{
  "QueueUrl": "$QUEUE_URL",
  "Attributes": {
    "VisibilityTimeout": "30",
    "RedrivePolicy": "{\"deadLetterTargetArn\":\"$DLQ_ARN\",\"maxReceiveCount\":\"3\"}"
  }
}
EOF

awsls sqs set-queue-attributes --cli-input-json "file://$INPUT_JSON" >/dev/null


# Verify
RP_CHECK="$(awsls sqs get-queue-attributes --queue-url "$QUEUE_URL" --attribute-names RedrivePolicy --query 'Attributes.RedrivePolicy' --output text || true)"
if [ -z "$RP_CHECK" ] || [ "$RP_CHECK" = "None" ]; then
  echo "[localstack-init] ERROR: RedrivePolicy missing after set-queue-attributes"
  awsls sqs get-queue-attributes --queue-url "$QUEUE_URL" --attribute-names All || true
  exit 1
fi

echo "[localstack-init] OK: RedrivePolicy applied"
awsls sqs list-queues
