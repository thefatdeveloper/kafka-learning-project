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

/**
 * Kafka Consumer that consumes Order messages from the "orders" topic.
 * This consumer demonstrates message consumption in a consumer group for load balancing.
 * Multiple instances of this consumer can run simultaneously to distribute the workload.
 *
 * @author Srikar
 * @version 1.0
 */
public class QueueConsumer {

    private static final String TOPIC_NAME = "orders";
    private static final String CONSUMER_ID = "Consumer-1";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Creates and configures Kafka Consumer properties.
     *
     * @return Properties object containing all Kafka consumer configurations
     */
    private static Properties createConsumerConfig() {
        Properties props = new Properties();

        // Bootstrap servers: Kafka broker address
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        // Consumer group ID: Identifies which consumer group this consumer belongs to
        // All consumers with the same group.id share the workload (load balancing)
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-processing-group");

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

            // Print consumption details with consumer ID
            System.out.println("==========================================");
            System.out.println("[" + CONSUMER_ID + "] Consumed Order:");
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
            System.err.println("[" + CONSUMER_ID + "] Error processing order from partition " +
                    record.partition() + " offset " + record.offset());
            e.printStackTrace();
        }
    }

    /**
     * Main method that orchestrates the consumer flow:
     * 1. Creates Kafka consumer
     * 2. Subscribes to the "orders" topic
     * 3. Polls for messages in an infinite loop
     * 4. Processes each consumed message
     * 5. Gracefully shuts down on interrupt
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("Starting " + CONSUMER_ID + "...");
        System.out.println("Consumer Group: order-processing-group");
        System.out.println("Topic: " + TOPIC_NAME);
        System.out.println("==========================================");

        // Create Kafka consumer with configured properties
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(createConsumerConfig());

        // Subscribe to the "orders" topic
        consumer.subscribe(Collections.singletonList(TOPIC_NAME));

        // Add shutdown hook for graceful shutdown
        // This ensures the consumer is properly closed when the application is terminated (Ctrl+C)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n==========================================");
            System.out.println("[" + CONSUMER_ID + "] Shutting down gracefully...");
            consumer.close();
            System.out.println("[" + CONSUMER_ID + "] Consumer closed.");
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
            System.err.println("[" + CONSUMER_ID + "] Error in consumer loop");
            e.printStackTrace();
        } finally {
            // Close consumer to release resources
            consumer.close();
            System.out.println("[" + CONSUMER_ID + "] Consumer closed.");
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
 * Step 2: Run QueueConsumer (Terminal 1)
 * Command: mvn exec:java -Dexec.mainClass="com.srikar.kafka.consumer.QueueConsumer"
 * Expected: Consumer starts and processes all existing messages from "orders" topic
 *           You should see all 5 orders being consumed with partition, offset, and order details
 *
 * Step 3: Send more orders while consumer is running (Terminal 2)
 * Command: mvn exec:java -Dexec.mainClass="com.srikar.kafka.producer.OrderProducer"
 * Expected: Terminal 1 (consumer) should immediately show the new orders being consumed
 *
 * Step 4: Run multiple consumers (Terminal 3 - modify CONSUMER_ID to "Consumer-2")
 * Command: mvn exec:java -Dexec.mainClass="com.srikar.kafka.consumer.QueueConsumer"
 * Expected: Partitions are rebalanced between Consumer-1 and Consumer-2
 *           Each consumer processes messages from different partitions (load balancing)
 *
 * Step 5: Check consumer group status
 * Command: docker exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group order-processing-group
 * Expected: Shows all active consumers, partition assignments, current offset, and lag
 *
 * Step 6: Stop consumer (in any terminal running consumer)
 * Command: Press Ctrl+C
 * Expected: Graceful shutdown message appears, consumer closes cleanly
 *
 * ===== TESTING CONSUMER GROUPS (LOAD BALANCING) =====
 *
 * To test multiple consumers with different IDs:
 * 1. Change CONSUMER_ID constant to "Consumer-2", "Consumer-3", etc.
 * 2. Recompile: mvn clean compile
 * 3. Run in separate terminals
 * 4. Send messages and observe how they're distributed across consumers
 * 5. Each consumer will process messages from different partitions
 */
