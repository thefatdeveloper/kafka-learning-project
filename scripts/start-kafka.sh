#!/bin/bash

# Start Kafka Learning Project
echo "Starting Kafka and Zookeeper..."

# Navigate to project root
cd "$(dirname "$0")/.."

# Start Docker Compose
docker-compose up -d

echo ""
echo "Waiting for Kafka to be ready..."
sleep 10

echo ""
echo "Kafka cluster is starting up!"
echo "Kafka broker: localhost:9092"
echo "Zookeeper: localhost:2181"
echo ""
echo "To view logs, run: docker-compose logs -f"
echo "To stop Kafka, run: ./scripts/stop-kafka.sh"
