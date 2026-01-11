package com.srikar.kafka.consumer;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srikar.kafka.model.Order;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

/**
 * Kafka Consumer that demonstrates BROADCAST behavior using unique consumer group IDs.
 *
 * BROADCAST MODE EXPLANATION:
 * Unlike QueueConsumer which uses a shared group.id for load balancing, this consumer
 * uses a UNIQUE group.id (generated with UUID) for each instance. This means:
 *
 * - Each TopicConsumer instance belongs to its own separate consumer group
 * - Kafka treats each consumer group independently
 * - Every consumer group receives ALL messages from ALL partitions
 * - This creates a "broadcast" or "pub-sub" pattern where every consumer gets every message
 *
 * Use Cases for Broadcast Mode:
 * - Logging/Monitoring: Multiple systems need to see all events
 * - Auditing: Separate audit trail that sees everything
 * - Analytics: Different analytics pipelines process the same data independently
 * - Multi-region replication: Each region gets all messages
 *
 * @author Srikar
 * @version 1.0
 */
public class TopicConsumer {

    private static final String TOPIC_NAME = "orders";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Unique consumer group ID - ensures this consumer receives ALL messages
    // Each instance gets a different UUID, so they don't share partitions
    private static final String CONSUMER_GROUP_ID = "broadcast-group-" + UUID.randomUUID();

    /**
     * Creates and configures Kafka Consumer properties with a UNIQUE group.id.
     * The unique group.id is the key to broadcast behavior.
     *
     * @return Properties object containing all Kafka consumer configurations
     */
    private static Properties createConsumerConfig() {
        Properties props = new Properties();

        // Bootstrap servers: Kafka broker address
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        // Consumer group ID: Using a UNIQUE group.id (with UUID)
        // WHY UNIQUE? Because each consumer group receives ALL messages independently
        // If multiple consumers have different group.ids, they all get ALL messages (broadcast)
        // If multiple consumers share the same group.id, they share messages (load balancing)
        props.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_ID);

        // Key deserializer: converts bytes back to key type (String)
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // Value deserializer: converts bytes back to value type (String - we'll parse JSON manually)
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // Auto offset reset: what to do when there is no initial offset or offset is out of range
        // "earliest" - start from the beginning of the topic
        // "latest" - start from the end (only new messages)
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Enable auto commit: automatically commit offsets periodically
        // When true, offsets are committed automatically in the background
        // When false, you must manually commit offsets using commitSync() or commitAsync()
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        // Auto commit interval: how often to commit offsets (default 5000ms)
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");

        return props;
    }

    /**
     * Processes a single consumed order record.
     * Deserializes JSON to Order object and prints detailed information.
     *
     * @param record the ConsumerRecord containing the order message
     */
    private static void processOrder(ConsumerRecord<String, String> record) {
        try {
            // Deserialize JSON string back to Order object
            Order order = objectMapper.readValue(record.value(), Order.class);

            // Print consumption details with group ID
            System.out.println("==========================================");
            System.out.println("[BROADCAST] Consumed Order:");
            System.out.println("  Consumer Group: " + CONSUMER_GROUP_ID);
            System.out.println("  Partition: " + record.partition());
            System.out.println("  Offset: " + record.offset());
            System.out.println("  Key: " + record.key());
            System.out.println("  Order Details:");
            System.out.println("    - Order ID: " + order.getOrderId());
            System.out.println("    - Customer ID: " + order.getCustomerId());
            System.out.println("    - Product: " + order.getProductName());
            System.out.println("    - Quantity: " + order.getQuantity());
            System.out.println("    - Price: $" + order.getPrice());
            System.out.println("    - Status: " + order.getStatus());
            System.out.println("    - Timestamp: " + order.getTimestamp());
            System.out.println("==========================================");

        } catch (Exception e) {
            System.err.println("[BROADCAST] Error processing order from partition " +
                    record.partition() + " offset " + record.offset());
            e.printStackTrace();
        }
    }

    /**
     * Main method that orchestrates the consumer flow:
     * 1. Creates Kafka consumer with unique group.id
     * 2. Subscribes to the "orders" topic
     * 3. Polls for messages in an infinite loop
     * 4. Processes each consumed message
     * 5. Gracefully shuts down on interrupt
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("BROADCAST MODE - This consumer receives ALL messages");
        System.out.println("==========================================");
        System.out.println("Starting TopicConsumer...");
        System.out.println("Consumer Group: " + CONSUMER_GROUP_ID);
        System.out.println("Topic: " + TOPIC_NAME);
        System.out.println("Mode: BROADCAST (unique group.id)");
        System.out.println("==========================================");

        // Create Kafka consumer with configured properties
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(createConsumerConfig());

        // Subscribe to the "orders" topic
        consumer.subscribe(Collections.singletonList(TOPIC_NAME));

        // Add shutdown hook for graceful shutdown
        // This ensures the consumer is properly closed when the application is terminated (Ctrl+C)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n==========================================");
            System.out.println("[BROADCAST] Shutting down gracefully...");
            consumer.close();
            System.out.println("[BROADCAST] Consumer closed.");
            System.out.println("==========================================");
        }));

        try {
            // Infinite polling loop
            // The consumer continuously polls Kafka for new messages
            while (true) {
                // Poll for new records with 1 second timeout
                // If no messages are available, poll() returns empty after timeout
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));

                // Process each consumed record
                for (ConsumerRecord<String, String> record : records) {
                    processOrder(record);
                }

                // Small pause to make output more readable
                if (!records.isEmpty()) {
                    Thread.sleep(100);
                }
            }

        } catch (Exception e) {
            System.err.println("[BROADCAST] Error in consumer loop");
            e.printStackTrace();
        } finally {
            // Close consumer to release resources
            consumer.close();
            System.out.println("[BROADCAST] Consumer closed.");
        }
    }
}

/*
 * ===== MANUAL TESTING GUIDE =====
 *
 * Prerequisites: Ensure Kafka is running and "orders" topic exists with messages
 *
 * Step 1: Compile the project
 * Command: mvn clean compile
 * Expected: BUILD SUCCESS
 *
 * Step 2: Run TopicConsumer (Terminal 1)
 * Command: mvn exec:java -Dexec.mainClass="com.srikar.kafka.consumer.TopicConsumer"
 * Expected: Consumer starts in BROADCAST MODE and processes all existing messages
 *           Note the unique consumer group ID displayed
 *
 * Step 3: Run ANOTHER TopicConsumer (Terminal 2)
 * Command: mvn exec:java -Dexec.mainClass="com.srikar.kafka.consumer.TopicConsumer"
 * Expected: This consumer ALSO processes ALL messages (not load balanced)
 *           Both consumers receive the same messages (broadcast behavior)
 *           Each has a different unique group ID
 *
 * Step 4: Send new orders (Terminal 3)
 * Command: mvn exec:java -Dexec.mainClass="com.srikar.kafka.producer.OrderProducer"
 * Expected: BOTH TopicConsumer instances receive ALL 5 orders
 *           This demonstrates broadcast/pub-sub pattern
 *
 * Step 5: Compare with QueueConsumer behavior (Terminal 4)
 * Command: mvn exec:java -Dexec.mainClass="com.srikar.kafka.consumer.QueueConsumer"
 * Expected: QueueConsumer shares partitions with other QueueConsumers (load balancing)
 *           But TopicConsumers still get ALL messages independently
 *
 * Step 6: Check all consumer groups
 * Command: docker exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list
 * Expected: See "order-processing-group" (for QueueConsumers)
 *           AND multiple "broadcast-group-<UUID>" groups (one per TopicConsumer)
 *
 * Step 7: Check specific broadcast consumer group
 * Command: docker exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group broadcast-group-<UUID>
 * Expected: Shows single consumer with ALL 3 partitions assigned (no sharing)
 *
 * ===== KEY DIFFERENCES: QUEUE vs BROADCAST =====
 *
 * QueueConsumer (Load Balancing):
 * - Shared group.id: "order-processing-group"
 * - Multiple consumers SHARE partitions
 * - Each message consumed by ONE consumer in the group
 * - Use case: Distribute workload across multiple workers
 *
 * TopicConsumer (Broadcast):
 * - Unique group.id: "broadcast-group-<UUID>"
 * - Each consumer gets ALL partitions
 * - Each message consumed by ALL consumers
 * - Use case: Multiple independent systems need all messages
 *
 * ===== PRACTICAL EXAMPLE =====
 *
 * Run simultaneously:
 * - Terminal 1: QueueConsumer (order-processing-group)
 * - Terminal 2: QueueConsumer (order-processing-group)
 * - Terminal 3: TopicConsumer (broadcast-group-xxx)
 * - Terminal 4: TopicConsumer (broadcast-group-yyy)
 * - Terminal 5: Send 10 orders
 *
 * Result:
 * - QueueConsumer instances share: Each gets ~5 orders (load balanced)
 * - TopicConsumer instances each get: All 10 orders (broadcast)
 */
