#!/bin/bash

# Stop Kafka Learning Project
echo "Stopping Kafka and Zookeeper..."

# Navigate to project root
cd "$(dirname "$0")/.."

# Stop Docker Compose
docker-compose down

echo ""
echo "Kafka cluster stopped successfully!"
