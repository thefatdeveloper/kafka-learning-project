package com.srikar.kafka.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.srikar.kafka.model.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Kafka Producer that sends Order messages to the "orders" topic.
 * This producer demonstrates asynchronous message publishing with callbacks.
 *
 * @author Srikar
 * @version 1.0
 */
public class OrderProducer {

    private static final String TOPIC_NAME = "orders";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Creates and configures Kafka Producer properties.
     *
     * @return Properties object containing all Kafka producer configurations
     */
    private static Properties createProducerConfig() {
        Properties props = new Properties();

        // Bootstrap servers: Kafka broker address
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        // Key serializer: converts key to bytes (using String serializer)
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Value serializer: converts value to bytes (using String serializer for JSON)
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Acknowledgment mode: 'all' waits for all in-sync replicas to acknowledge
        // This provides the strongest durability guarantee
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        // Retries: number of times to retry failed sends
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        return props;
    }

    /**
     * Sends an Order to the Kafka "orders" topic asynchronously.
     * Uses the orderId as the partition key to ensure ordering for the same order.
     *
     * @param producer the KafkaProducer instance
     * @param order the Order object to send
     */
    private static void sendOrder(KafkaProducer<String, String> producer, Order order) {
        try {
            // Serialize Order object to JSON string
            String orderJson = objectMapper.writeValueAsString(order);

            // Create ProducerRecord with topic, key (orderId), and value (JSON)
            // Using orderId as key ensures all updates to same order go to same partition
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    TOPIC_NAME,
                    order.getOrderId(),
                    orderJson
            );

            System.out.println("Sending order: " + order);

            // Send asynchronously with callback
            producer.send(record, new Callback() {
                @Override
                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    if (exception == null) {
                        // Success
                        System.out.println("✓ Sent order " + order.getOrderId() +
                                " to partition " + metadata.partition() +
                                " with offset " + metadata.offset());
                    } else {
                        // Failure
                        System.err.println("✗ Failed to send order " + order.getOrderId());
                        exception.printStackTrace();
                    }
                }
            });

        } catch (JsonProcessingException e) {
            System.err.println("Error serializing order to JSON: " + order.getOrderId());
            e.printStackTrace();
        }
    }

    /**
     * Generates a list of 5 sample Order objects with realistic data.
     *
     * @return List of 5 mock Order objects
     */
    private static List<Order> generateSampleOrders() {
        List<Order> orders = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        orders.add(new Order(
                "ORD-001",
                "CUST-101",
                "Laptop",
                1,
                999.99,
                "NEW",
                currentTime
        ));

        orders.add(new Order(
                "ORD-002",
                "CUST-102",
                "Monitor",
                2,
                349.99,
                "NEW",
                currentTime + 1000
        ));

        orders.add(new Order(
                "ORD-003",
                "CUST-103",
                "Keyboard",
                3,
                79.99,
                "NEW",
                currentTime + 2000
        ));

        orders.add(new Order(
                "ORD-004",
                "CUST-101",
                "Mouse",
                1,
                29.99,
                "NEW",
                currentTime + 3000
        ));

        orders.add(new Order(
                "ORD-005",
                "CUST-104",
                "USB Cable",
                5,
                9.99,
                "NEW",
                currentTime + 4000
        ));

        return orders;
    }

    /**
     * Main method that orchestrates the producer flow:
     * 1. Creates Kafka producer
     * 2. Generates sample orders
     * 3. Sends all orders asynchronously
     * 4. Flushes and closes the producer
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("Starting OrderProducer...");
        System.out.println("==========================================");

        long startTime = System.currentTimeMillis();

        // Create Kafka producer with configured properties
        KafkaProducer<String, String> producer = new KafkaProducer<>(createProducerConfig());

        try {
            // Generate sample orders
            List<Order> sampleOrders = generateSampleOrders();

            // Send each order asynchronously
            for (Order order : sampleOrders) {
                sendOrder(producer, order);
            }

            // Flush ensures all messages are sent before continuing
            producer.flush();
            System.out.println("==========================================");
            System.out.println("All " + sampleOrders.size() + " orders sent successfully!");

            // Wait 2 seconds to allow callbacks to complete
            TimeUnit.SECONDS.sleep(2);

        } catch (InterruptedException e) {
            System.err.println("Producer interrupted during sleep");
            e.printStackTrace();
        } finally {
            // Always close the producer to release resources
            producer.close();
            System.out.println("Producer flushed and closed.");
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Total execution time: " + (endTime - startTime) + " ms");
        System.out.println("==========================================");
    }
}

/*
 * ===== MANUAL TESTING GUIDE =====
 *
 * Step 1: Compile the project
 * Command: mvn clean compile
 * Expected: BUILD SUCCESS
 *
 * Step 2: Run OrderProducer to send 5 orders
 * Command: mvn exec:java -Dexec.mainClass="com.srikar.kafka.producer.OrderProducer"
 * Expected: See "Starting OrderProducer..." and 5 success messages with partition/offset info
 *
 * Step 3: Verify messages in "orders" topic using Kafka Console Consumer
 * Command: docker exec kafka kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic orders --from-beginning
 * Expected: See all 5 orders in JSON format (you may need to scroll to see all)
 * To exit: Press Ctrl+C
 *
 * Step 4: Check topic metadata (optional)
 * Command: docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic orders
 * Expected: See topic info with 3 partitions, confirms orders topic exists
 *
 * Step 5: Check consumer groups (optional - for Phase 3)
 * Command: docker exec kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
 * Expected: (empty initially, populated after consumers are created in Phase 3)
 */
