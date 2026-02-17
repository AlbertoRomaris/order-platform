output "order_events_queue_url" {
  value = aws_sqs_queue.main.url
}

output "order_events_queue_arn" {
  value = aws_sqs_queue.main.arn
}

output "order_events_dlq_url" {
  value = aws_sqs_queue.dlq.url
}

output "order_events_dlq_arn" {
  value = aws_sqs_queue.dlq.arn
}

output "ec2_public_ip" {
  value = aws_instance.ec2.public_ip
}
