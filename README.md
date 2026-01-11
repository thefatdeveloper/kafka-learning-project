# 🚀 Real-Time Order Processing System

A production-ready Apache Kafka project demonstrating real-time order processing with producers, consumers, and stream processing. Built to showcase enterprise-level Kafka expertise.

## 📋 Project Overview

This project implements a **real-time e-commerce order processing pipeline** using Apache Kafka. It demonstrates key Kafka concepts including partitioned producers, consumer groups, broadcast consumers, and stateful stream processing with aggregations.

### Technologies Used

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Core programming language |
| Apache Kafka | 3.6.1 | Distributed streaming platform |
| Kafka Streams | 3.6.1 | Real-time stream processing |
| Maven | 3.x | Build and dependency management |
| Docker Compose | - | Kafka infrastructure orchestration |
| Jackson | 2.16.1 | JSON serialization/deserialization |

### Use Case

E-commerce platform processing order events in real-time:
- **Producers** publish order events to Kafka
- **Queue Consumers** process orders with load balancing across instances
- **Topic Consumers** broadcast all orders to multiple independent systems
- **Streams Processor** calculates running totals per customer

---

## 🏗️ Architecture Diagram

```
┌─────────────────┐
│ OrderProducer   │ (Sends 5 sample orders)
└────────┬────────┘
         │ JSON Orders
         ▼
┌─────────────────────────────────────────────────┐
│           Kafka Broker (localhost:9092)         │
│                                                 │
│  Topic: "orders" (3 partitions, RF=1)          │
│  ├─ Partition 0: [ORD-004, ORD-005]           │
│  ├─ Partition 1: [ORD-001, ORD-003]           │
│  └─ Partition 2: [ORD-002]                    │
└─────────┬────────────────────┬──────────────────┘
          │                    │
          │                    │
    ┌─────▼─────┐         ┌────▼────┐
    │ Consumer  │         │ Consumer│ (Same group: "order-processing-group")
    │    #1     │◄────────┤    #2   │ → LOAD BALANCING
    └───────────┘         └─────────┘    (Partitions distributed)
          │
          │
    ┌─────▼─────┐         ┌──────────┐
    │  Topic    │         │  Topic   │ (Different groups: "broadcast-group-UUID")
    │Consumer #1│         │Consumer#2│ → BROADCAST MODE
    └───────────┘         └──────────┘    (All get ALL messages)
          │
          │
    ┌─────▼──────────────────────────────────────┐
    │      OrderStatsStream Processor            │
    │  (Aggregates: Customer → Running Total)    │
    └─────┬──────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────┐
│  Topic: "order-stats" (1 partition, RF=1)      │
│  Output: CUST-101 → $1029.98                   │
│          CUST-102 → $699.98                    │
└─────────────────────────────────────────────────┘
```

---

## 💡 Key Concepts Demonstrated

### 1. **Producer with Partitioning by Key**
Messages are distributed across partitions based on the orderId key, ensuring all events for the same order go to the same partition for ordering guarantees.

### 2. **Consumer Groups - Queue Mode (Load Balancing)**
Multiple consumers with the **same** `group.id` share partitions. Each message is consumed by only ONE consumer in the group, distributing workload across instances.

### 3. **Consumer Groups - Topic Mode (Broadcast/Pub-Sub)**
Multiple consumers with **different** `group.id` values each receive ALL messages from ALL partitions. Enables independent processing pipelines like logging, analytics, and auditing.

### 4. **Kafka Streams - Real-Time Aggregation**
Stateful stream processing that maintains running totals per customer using KStream → KGroupedStream → KTable pattern with fault-tolerant state stores.

### 5. **Async Production with Callbacks**
Non-blocking message publishing with acknowledgment callbacks showing partition assignment and offset confirmation.

### 6. **JSON Serialization with Jackson**
Custom serialization/deserialization of POJO models to JSON for human-readable message format.

---

## 📁 Project Structure

```
kafka-learning-project/
├── docker-compose.yml                 # Kafka infrastructure setup
├── pom.xml                           # Maven dependencies
├── README.md                         # This file
├── scripts/                          # Helper scripts
└── src/main/java/com/srikar/kafka/
    ├── model/
    │   └── Order.java                # 📦 POJO: Order entity (7 fields)
    ├── producer/
    │   └── OrderProducer.java        # 📤 Producer: Sends 5 orders asynchronously
    ├── consumer/
    │   ├── QueueConsumer.java        # 📥 Consumer: Load balancing (group.id shared)
    │   ├── QueueConsumer2.java       # 📥 Consumer: Second instance for testing
    │   └── TopicConsumer.java        # 📢 Consumer: Broadcast (group.id unique)
    └── streams/
        └── OrderStatsStream.java     # 🌊 Streams: Customer total aggregation
```

### Class Descriptions

| Class | Type | Description |
|-------|------|-------------|
| **Order.java** | Model | POJO with orderId, customerId, productName, quantity, price, status, timestamp |
| **OrderProducer.java** | Producer | Sends 5 sample orders to "orders" topic with async callbacks and partition key |
| **QueueConsumer.java** | Consumer | group.id=`order-processing-group` - Load balances with other instances |
| **QueueConsumer2.java** | Consumer | Same group as QueueConsumer - Demonstrates partition sharing |
| **TopicConsumer.java** | Consumer | group.id=`broadcast-group-<UUID>` - Receives ALL messages (pub-sub) |
| **OrderStatsStream.java** | Streams | Aggregates orders by customerId, maintains running total, outputs to "order-stats" |

---

## ⚙️ Prerequisites

- ☕ **Java 21** - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/)
- 📦 **Maven 3.x** - Install via `brew install maven` (macOS) or [download](https://maven.apache.org/download.cgi)
- 🐳 **Docker & Docker Compose** - [Install Docker Desktop](https://www.docker.com/products/docker-desktop/)
- 🔧 **Git** - For cloning the repository

---

## 🚀 How to Run

### Step 1: Clone the Repository

```bash
git clone https://github.com/thefatdeveloper/kafka-learning-project.git
cd kafka-learning-project
```

### Step 2: Start Kafka Infrastructure

```bash
# Start Zookeeper, Kafka broker, and topic initialization
docker-compose up -d

# Wait 15 seconds for Kafka to be ready
sleep 15

# Verify containers are running
docker-compose ps
```

### Step 3: Create Kafka Topics (if not auto-created)

```bash
# Create "orders" topic with 3 partitions
docker exec kafka kafka-topics --create --if-not-exists \
  --bootstrap-server localhost:9092 \
  --topic orders --partitions 3 --replication-factor 1

# Create "order-stats" topic with 1 partition
docker exec kafka kafka-topics --create --if-not-exists \
  --bootstrap-server localhost:9092 \
  --topic order-stats --partitions 1 --replication-factor 1

# Verify topics
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

### Step 4: Compile the Project

```bash
mvn clean compile
```

### Step 5: Run the Producer

**Terminal 1:**
```bash
mvn exec:java -Dexec.mainClass="com.srikar.kafka.producer.OrderProducer"
```

**Expected Output:**
```
==========================================
Starting OrderProducer...
==========================================
Sending order: Order{orderId='ORD-001', customerId='CUST-101', productName='Laptop', ...}
✓ Sent order ORD-001 to partition 1 with offset 0
✓ Sent order ORD-002 to partition 2 with offset 0
✓ Sent order ORD-003 to partition 1 with offset 1
✓ Sent order ORD-004 to partition 0 with offset 0
✓ Sent order ORD-005 to partition 0 with offset 1
==========================================
All 5 orders sent successfully!
Producer flushed and closed.
Total execution time: 2207 ms
==========================================
```

### Step 6: Run Queue Consumer (Load Balancing)

**Terminal 2:**
```bash
mvn exec:java -Dexec.mainClass="com.srikar.kafka.consumer.QueueConsumer"
```

**Terminal 3 (Optional - Second Consumer):**
```bash
mvn exec:java -Dexec.mainClass="com.srikar.kafka.consumer.QueueConsumer2"
```

**Expected Output (Consumer 1 - processes ~2-3 orders):**
```
==========================================
Starting Consumer-1...
Consumer Group: order-processing-group
Topic: orders
==========================================
[Consumer-1] Consumed Order:
  Partition: 1
  Offset: 0
  Key: ORD-001
  Order Details:
    - Order ID: ORD-001
    - Customer ID: CUST-101
    - Product: Laptop
    - Quantity: 1
    - Price: $999.99
    - Status: NEW
    - Timestamp: 1767449110761
==========================================
```

### Step 7: Run Topic Consumer (Broadcast Mode)

**Terminal 4:**
```bash
mvn exec:java -Dexec.mainClass="com.srikar.kafka.consumer.TopicConsumer"
```

**Expected Output (receives ALL 5 orders):**
```
==========================================
BROADCAST MODE - This consumer receives ALL messages
==========================================
Starting TopicConsumer...
Consumer Group: broadcast-group-a1b2c3d4-e5f6-7890-abcd-ef1234567890
Topic: orders
Mode: BROADCAST (unique group.id)
==========================================
[BROADCAST] Consumed Order:
  Consumer Group: broadcast-group-a1b2c3d4-e5f6-7890-abcd-ef1234567890
  Partition: 0
  Offset: 0
  Key: ORD-004
  ...
```

### Step 8: Run Kafka Streams Processor

**Terminal 5:**
```bash
mvn exec:java -Dexec.mainClass="com.srikar.kafka.streams.OrderStatsStream"
```

**Expected Output:**
```
==========================================
Starting OrderStatsStream...
Application ID: order-stats-app
Input Topic: orders
Output Topic: order-stats
==========================================
Processing order ORD-001 for customer CUST-101, amount: $999.99
Customer CUST-101 new total: $999.99
Processing order ORD-004 for customer CUST-101, amount: $29.99
Customer CUST-101 new total: $1029.98
Processing order ORD-002 for customer CUST-102, amount: $699.98
Customer CUST-102 new total: $699.98
...
OrderStatsStream is running...
Press Ctrl+C to stop.
==========================================
```

### Step 9: Verify Stream Processor Output

**Terminal 6:**
```bash
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-stats \
  --from-beginning \
  --property print.key=true \
  --property key.separator=:
```

**Expected Output:**
```
CUST-101:999.99
CUST-102:699.98
CUST-103:239.97
CUST-101:1029.98
CUST-104:49.95
```

### Step 10: Shutdown

```bash
# Stop each Java application with Ctrl+C in each terminal

# Stop Kafka infrastructure
docker-compose down

# To remove all data (start fresh next time)
docker-compose down -v
```

---

## 📊 Sample Output

### Producer Output
```
==========================================
Starting OrderProducer...
==========================================
Sending order: Order{orderId='ORD-001', customerId='CUST-101', productName='Laptop', quantity=1, price=999.99, status='NEW', timestamp=1767449110761}
Sending order: Order{orderId='ORD-002', customerId='CUST-102', productName='Monitor', quantity=2, price=349.99, status='NEW', timestamp=1767449111761}
Sending order: Order{orderId='ORD-003', customerId='CUST-103', productName='Keyboard', quantity=3, price=79.99, status='NEW', timestamp=1767449112761}
Sending order: Order{orderId='ORD-004', customerId='CUST-101', productName='Mouse', quantity=1, price=29.99, status='NEW', timestamp=1767449113761}
Sending order: Order{orderId='ORD-005', customerId='CUST-104', productName='USB Cable', quantity=5, price=9.99, status='NEW', timestamp=1767449114761}
✓ Sent order ORD-004 to partition 0 with offset 0
✓ Sent order ORD-005 to partition 0 with offset 1
✓ Sent order ORD-001 to partition 1 with offset 0
✓ Sent order ORD-003 to partition 1 with offset 1
✓ Sent order ORD-002 to partition 2 with offset 0
==========================================
All 5 orders sent successfully!
Total execution time: 2207 ms
==========================================
```

### Consumer Output (Queue Mode - Load Balanced)
```
==========================================
[Consumer-1] Consumed Order:
  Partition: 1
  Offset: 0
  Key: ORD-001
  Order Details:
    - Order ID: ORD-001
    - Customer ID: CUST-101
    - Product: Laptop
    - Quantity: 1
    - Price: $999.99
    - Status: NEW
==========================================
```

### Streams Processor Output (Aggregation)
```
Processing order ORD-001 for customer CUST-101, amount: $999.99
Customer CUST-101 new total: $999.99
Processing order ORD-004 for customer CUST-101, amount: $29.99
Customer CUST-101 new total: $1029.98
Processing order ORD-002 for customer CUST-102, amount: $699.98
Customer CUST-102 new total: $699.98
Processing order ORD-003 for customer CUST-103, amount: $239.97
Customer CUST-103 new total: $239.97
Processing order ORD-005 for customer CUST-104, amount: $49.95
Customer CUST-104 new total: $49.95
```

---

## 🎓 Key Learnings

### 1. **Partition Keys Ensure Message Ordering**
Using `orderId` as the partition key guarantees that all messages for the same order are sent to the same partition and consumed in order. This is critical for maintaining event ordering in distributed systems.

### 2. **Consumer Group ID Determines Queue vs Topic Behavior**
- **Same group.id** = Load balancing (queue pattern) - Each message consumed once across the group
- **Unique group.id** = Broadcast (pub-sub pattern) - Each consumer receives all messages
This single configuration change enables vastly different consumption patterns.

### 3. **Kafka Streams Simplifies Stateful Processing**
Without Kafka Streams, maintaining aggregated state requires manual state management with external databases. Streams API provides built-in state stores with automatic fault tolerance and changelog backup.

### 4. **Asynchronous Production Maximizes Throughput**
Callbacks enable fire-and-forget message sending with confirmation handling, allowing the producer to continue sending while waiting for acknowledgments from the broker.

### 5. **Serdes Are Critical for Type Safety**
Proper serialization/deserialization configuration prevents runtime errors and ensures data integrity across the pipeline. Using JSON with Jackson provides human-readable messages for debugging.

### 6. **Consumer Rebalancing Enables Elastic Scaling**
Adding or removing consumer instances automatically triggers partition rebalancing, allowing horizontal scaling without application downtime or manual intervention.

### 7. **Offset Management Ensures Exactly-Once Semantics**
Auto-commit with proper configuration prevents message loss and duplicate processing. Understanding offset behavior is crucial for reliable message processing.

---

## 🧪 Testing & Verification Commands

### View Consumer Groups
```bash
docker exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --list
```

### Describe Consumer Group (Check Lag)
```bash
docker exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group order-processing-group
```

### View Topic Details
```bash
docker exec kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe --topic orders
```

### Consume Messages from Beginning
```bash
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic orders \
  --from-beginning \
  --max-messages 5
```

---

## 🛠️ Troubleshooting

### Issue: "UNKNOWN_TOPIC_OR_PARTITION" error
**Solution:** Create topics manually using the commands in Step 3.

### Issue: Producer/Consumer can't connect to Kafka
**Solution:**
1. Verify Kafka is running: `docker-compose ps`
2. Check Kafka logs: `docker-compose logs kafka`
3. Restart Kafka: `docker-compose restart kafka`

### Issue: Consumers not receiving messages
**Solution:**
1. Check consumer group offsets
2. Verify topic has messages: `kafka-console-consumer --from-beginning`
3. Ensure auto.offset.reset is set to "earliest"

---

## 🔮 Future Enhancements

- [ ] Add Schema Registry for Avro serialization
- [ ] Implement error handling with Dead Letter Queue (DLQ)
- [ ] Add monitoring with Prometheus & Grafana
- [ ] Implement KSQL for SQL-like stream processing
- [ ] Add integration tests with Testcontainers
- [ ] Implement transactional producers for exactly-once semantics
- [ ] Add security (SSL/SASL authentication)

---

## 👨‍💻 Author

**Srikar Ganugapati**

- 🔗 LinkedIn: [www.linkedin.com/in/srikargvs17](https://www.linkedin.com/in/srikargvs17)
- 💻 GitHub: [https://github.com/thefatdeveloper](https://github.com/thefatdeveloper)
- 📧 Email: srikar.ganugapati@outlook.com

---

## 📄 License

This project is licensed under the **MIT License** - see below for details:

---

## ⭐ Acknowledgments

- Apache Kafka documentation and community
- Confluent Platform tutorials and guides
- Jackson JSON processor library

---

<div align="center">

**If you found this project helpful, please consider giving it a ⭐ on GitHub!**

Made with ❤️ and ☕ by Srikar Ganugapati

</div>
