package com.srikar.kafka.streams;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srikar.kafka.model.Order;

import java.util.Properties;

/**
 * Kafka Streams application that processes orders and calculates running totals per customer.
 * This demonstrates real-time stream processing and stateful aggregations.
 *
 * KAFKA STREAMS CONCEPTS:
 *
 * 1. KStream vs KTable:
 *    - KStream: Represents an unbounded stream of records (like a log or event stream)
 *      Each record is independent. Think: "INSERT-only table" or "event log"
 *      Example: Stream of order events - each order is a separate event
 *
 *    - KTable: Represents a changelog stream (like a database table)
 *      Latest record for each key is the current value. Think: "UPDATE-able table"
 *      Example: Current total per customer - updates as new orders arrive
 *
 * 2. Serdes (Serializer/Deserializer):
 *    Serdes define how to convert objects to/from bytes for storage and transmission
 *    - Serdes.String(): For String keys/values
 *    - Serdes.Long(): For Long keys/values
 *    - Custom Serdes can be created for complex objects
 *
 * 3. GroupBy and KGroupedStream:
 *    - groupBy(): Groups records by a key, creates KGroupedStream
 *    - KGroupedStream: Intermediate step before aggregation
 *    - Enables operations like aggregate(), count(), reduce()
 *
 * 4. Aggregate and State:
 *    - aggregate(): Performs stateful computation across grouped records
 *    - Maintains running state (like running total) in a state store
 *    - State is fault-tolerant and automatically recovered
 *
 * @author Srikar
 * @version 1.0
 */
public class OrderStatsStream {

    private static final String INPUT_TOPIC = "orders";
    private static final String OUTPUT_TOPIC = "order-stats";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Creates and configures Kafka Streams properties.
     *
     * @return Properties object containing all Kafka Streams configurations
     */
    private static Properties createStreamsConfig() {
        Properties props = new Properties();

        // Application ID: Unique identifier for this Kafka Streams application
        // Also used as the consumer group ID
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "order-stats-app");

        // Bootstrap servers: Kafka broker address
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        // Default key serde: Used for serializing/deserializing keys
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        // Default value serde: Used for serializing/deserializing values
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        return props;
    }

    /**
     * Parses JSON order string to Order object.
     * Returns null and logs warning if parsing fails (bad record is skipped).
     *
     * @param orderJson JSON string representation of an order
     * @return Order object or null if parsing fails
     */
    private static Order parseOrder(String orderJson) {
        try {
            return objectMapper.readValue(orderJson, Order.class);
        } catch (Exception e) {
            System.err.println("WARNING: Failed to parse order JSON, skipping record: " + orderJson);
            System.err.println("Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Calculates the total amount for an order (quantity * price).
     *
     * @param order the Order object
     * @return total amount as a double
     */
    private static double calculateOrderTotal(Order order) {
        return order.getQuantity() * order.getPrice();
    }

    /**
     * Builds the Kafka Streams topology for order statistics processing.
     *
     * Stream Processing Flow:
     * 1. Read from "orders" topic as KStream (unbounded stream of order events)
     * 2. Parse JSON to Order object (skip bad records)
     * 3. Calculate order total (quantity * price)
     * 4. Group by customerId (creates KGroupedStream)
     * 5. Aggregate to calculate running total per customer (creates KTable)
     * 6. Convert KTable to KStream for output
     * 7. Write to "order-stats" topic
     *
     * @return StreamsBuilder configured with the processing topology
     */
    private static StreamsBuilder buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        // Step 1: Create KStream from "orders" topic
        // KStream<String, String>: key=orderId (from producer), value=JSON order
        KStream<String, String> ordersStream = builder.stream(INPUT_TOPIC);

        // Step 2-5: Process orders and aggregate by customer
        KTable<String, String> customerTotals = ordersStream
                // Filter and map: Parse JSON, calculate total, re-key by customerId
                .mapValues(orderJson -> {
                    Order order = parseOrder(orderJson);
                    if (order == null) {
                        return null; // Skip bad records
                    }

                    double total = calculateOrderTotal(order);
                    System.out.println("Processing order " + order.getOrderId() +
                            " for customer " + order.getCustomerId() +
                            ", amount: $" + String.format("%.2f", total));

                    // Return order with total for next step
                    return order.getCustomerId() + ":" + total;
                })
                .filter((key, value) -> value != null) // Remove null values (bad records)

                // Group by customerId
                // selectKey() changes the key from orderId to customerId
                .selectKey((key, value) -> value.split(":")[0])

                // groupByKey() creates KGroupedStream<customerId, "customerId:total">
                .groupByKey()

                // Aggregate: Calculate running total per customer
                // aggregate(initializer, adder, materialized)
                // - initializer: Starting value (0.0 for new customers)
                // - adder: Function to add new value to running total
                // - Materialized: Specifies serdes for state store
                .aggregate(
                        () -> "0.0", // Initial value for new customers
                        (customerId, newValue, runningTotal) -> {
                            // Extract the order total from "customerId:total" format
                            double orderTotal = Double.parseDouble(newValue.split(":")[1]);
                            double currentTotal = Double.parseDouble(runningTotal);
                            double newTotal = currentTotal + orderTotal;

                            System.out.println("Customer " + customerId +
                                    " new total: $" + String.format("%.2f", newTotal));

                            return String.format("%.2f", newTotal);
                        },
                        Materialized.with(Serdes.String(), Serdes.String())
                );

        // Step 6-7: Convert KTable to KStream and write to output topic
        // KTable.toStream() converts changelog stream back to event stream
        customerTotals.toStream().to(OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.String()));

        return builder;
    }

    /**
     * Main method that orchestrates the Kafka Streams application:
     * 1. Creates streams configuration
     * 2. Builds the processing topology
     * 3. Starts the streams application
     * 4. Adds shutdown hook for graceful close
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("Starting OrderStatsStream...");
        System.out.println("Application ID: order-stats-app");
        System.out.println("Input Topic: " + INPUT_TOPIC);
        System.out.println("Output Topic: " + OUTPUT_TOPIC);
        System.out.println("==========================================");

        // Create streams configuration
        Properties props = createStreamsConfig();

        // Build topology
        StreamsBuilder builder = buildTopology();

        // Create KafkaStreams instance
        KafkaStreams streams = new KafkaStreams(builder.build(), props);

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n==========================================");
            System.out.println("Shutting down OrderStatsStream gracefully...");
            streams.close();
            System.out.println("OrderStatsStream closed.");
            System.out.println("==========================================");
        }));

        // Start the streams application
        streams.start();

        System.out.println("OrderStatsStream is running...");
        System.out.println("Press Ctrl+C to stop.");
        System.out.println("==========================================");
    }
}

/*
 * ===== MANUAL TESTING GUIDE =====
 *
 * Prerequisites: Ensure Kafka is running and both topics exist
 *
 * Step 1: Verify topics exist
 * Command: docker exec kafka kafka-topics --list --bootstrap-server localhost:9092
 * Expected: See "orders" and "order-stats" topics
 *
 * Step 2: Compile the project
 * Command: mvn clean compile
 * Expected: BUILD SUCCESS
 *
 * Step 3: Start OrderStatsStream (Terminal 1)
 * Command: mvn exec:java -Dexec.mainClass="com.srikar.kafka.streams.OrderStatsStream"
 * Expected: "OrderStatsStream is running..." message
 *           Processing messages appear for existing orders in the topic
 *
 * Step 4: Monitor order-stats topic (Terminal 2)
 * Command: docker exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic order-stats --from-beginning --property print.key=true --property key.separator=:
 * Expected: See customer IDs with their running totals
 *           Format: CUST-101:999.99
 *
 * Step 5: Send new orders (Terminal 3)
 * Command: mvn exec:java -Dexec.mainClass="com.srikar.kafka.producer.OrderProducer"
 * Expected: Terminal 1 shows processing messages
 *           Terminal 2 shows updated customer totals
 *
 * Example Output in Terminal 1:
 * Processing order ORD-001 for customer CUST-101, amount: $999.99
 * Customer CUST-101 new total: $999.99
 * Processing order ORD-004 for customer CUST-101, amount: $29.99
 * Customer CUST-101 new total: $1029.98
 *
 * Example Output in Terminal 2:
 * CUST-101:999.99
 * CUST-102:699.98
 * CUST-103:239.97
 * CUST-101:1029.98
 * CUST-104:49.95
 *
 * Step 6: Test with multiple producers
 * Command: Run OrderProducer multiple times in Terminal 3
 * Expected: Running totals increase with each batch of orders
 *           Same customer's orders are aggregated correctly
 *
 * Step 7: Check Kafka Streams application state
 * Command: docker exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group order-stats-app
 * Expected: Shows consumer group for the streams app with partition assignments
 *
 * Step 8: Stop the stream processor
 * Command: Press Ctrl+C in Terminal 1
 * Expected: Graceful shutdown message appears
 *
 * ===== UNDERSTANDING THE OUTPUT =====
 *
 * The stream processor maintains running totals per customer:
 * - First order from CUST-101: Total = $999.99
 * - Second order from CUST-101: Total = $999.99 + $29.99 = $1029.98
 * - Third order from CUST-101: Total = $1029.98 + (new order amount)
 *
 * This demonstrates stateful stream processing where:
 * - State is maintained in a local state store
 * - State is automatically backed up to Kafka
 * - State survives application restarts
 *
 * ===== TESTING ERROR HANDLING =====
 *
 * To test bad JSON handling:
 * 1. Send invalid JSON to orders topic:
 *    Command: echo "invalid-json" | docker exec -i kafka kafka-console-producer --bootstrap-server localhost:9092 --topic orders
 * 2. Expected: Warning message in Terminal 1, record is skipped
 * 3. Stream processor continues running normally
 */
