package com.srikar.kafka.model;

/**
 * POJO model representing an Order in the Real-Time Order Processing System.
 * This class is designed to be JSON serialization-friendly for Kafka message passing.
 *
 * @author Srikar
 * @version 1.0
 */
public class Order {

    /**
     * Unique identifier for the order
     */
    private String orderId;

    /**
     * Customer who placed the order
     */
    private String customerId;

    /**
     * Name of the product ordered
     */
    private String productName;

    /**
     * Quantity of the product ordered
     */
    private int quantity;

    /**
     * Price per unit of the product
     */
    private double price;

    /**
     * Current status of the order (NEW, PROCESSING, COMPLETED)
     */
    private String status;

    /**
     * Timestamp when the order was created (epoch time in milliseconds)
     */
    private long timestamp;

    /**
     * No-argument constructor required for JSON deserialization.
     */
    public Order() {
    }

    /**
     * All-arguments constructor to create a complete Order object.
     *
     * @param orderId unique identifier for the order
     * @param customerId customer who placed the order
     * @param productName name of the product ordered
     * @param quantity quantity of the product ordered
     * @param price price per unit of the product
     * @param status current status of the order
     * @param timestamp timestamp when the order was created
     */
    public Order(String orderId, String customerId, String productName, int quantity,
                 double price, String status, long timestamp) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
        this.timestamp = timestamp;
    }

    /**
     * Gets the order ID.
     *
     * @return the unique identifier for the order
     */
    public String getOrderId() {
        return orderId;
    }

    /**
     * Sets the order ID.
     *
     * @param orderId the unique identifier for the order
     */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /**
     * Gets the customer ID.
     *
     * @return the customer who placed the order
     */
    public String getCustomerId() {
        return customerId;
    }

    /**
     * Sets the customer ID.
     *
     * @param customerId the customer who placed the order
     */
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    /**
     * Gets the product name.
     *
     * @return the name of the product ordered
     */
    public String getProductName() {
        return productName;
    }

    /**
     * Sets the product name.
     *
     * @param productName the name of the product ordered
     */
    public void setProductName(String productName) {
        this.productName = productName;
    }

    /**
     * Gets the quantity ordered.
     *
     * @return the quantity of the product ordered
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Sets the quantity ordered.
     *
     * @param quantity the quantity of the product ordered
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * Gets the price per unit.
     *
     * @return the price per unit of the product
     */
    public double getPrice() {
        return price;
    }

    /**
     * Sets the price per unit.
     *
     * @param price the price per unit of the product
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * Gets the order status.
     *
     * @return the current status of the order
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the order status.
     *
     * @param status the current status of the order
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the timestamp when the order was created.
     *
     * @return the timestamp in epoch milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp when the order was created.
     *
     * @param timestamp the timestamp in epoch milliseconds
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns a string representation of the Order object.
     *
     * @return a string containing all order fields
     */
    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
