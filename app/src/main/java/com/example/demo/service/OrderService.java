package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.model.Product;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final ProductService productService;
    private final ObjectMapper objectMapper;
    private final Tracer tracer = GlobalOpenTelemetry.getTracer("OrderService");
    // Thread pool to simulate async production behavior
    private final ExecutorService workerPool = Executors.newFixedThreadPool(4);

    {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    public OrderService(ProductService productService) {
        this.productService = productService;
    }

    public List<Order> getAllOrders() {
        Span span = tracer.spanBuilder("OrderService.getAllOrders").startSpan();
        try (Scope scope = span.makeCurrent()) {
            validateRequest();
            List<Order> orderList = new ArrayList<>(orders.values());
            List<Order> enriched = enrichOrders(orderList);
            auditAccess("getAllOrders", orderList.size());
            return enriched;
        } finally {
            span.end();
        }
    }

    public Optional<Order> getOrderById(Long id) {
        Span span = tracer.spanBuilder("OrderService.getOrderById").setAttribute("order.id", id).startSpan();
        try (Scope scope = span.makeCurrent()) {
            validateRequest();
            Optional<Order> order = Optional.ofNullable(orders.get(id));
            order.ifPresent(o -> {
                enrichOrderDetails(o);
                calculateShipping(o);
                auditAccess("getOrderById", 1);
            });
            return order;
        } finally {
            span.end();
        }
    }

    public List<Order> getOrdersByUserId(Long userId) {
        Span span = tracer.spanBuilder("OrderService.getOrdersByUserId").setAttribute("user.id", userId).startSpan();
        try (Scope scope = span.makeCurrent()) {
            validateRequest();
            List<Order> userOrders = orders.values().stream()
                    .filter(order -> order.getUserId().equals(userId))
                    .collect(Collectors.toList());
            List<Order> enriched = enrichOrders(userOrders);
            auditAccess("getOrdersByUserId", userOrders.size());
            return enriched;
        } finally {
            span.end();
        }
    }

    public Order createOrder(Long userId, List<Long> productIds) {
        Span span = tracer.spanBuilder("OrderService.createOrder")
                .setAttribute("user.id", userId)
                .setAttribute("product.count", productIds.size())
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            validateRequest();
            validateOrderData(userId, productIds);

            List<Product> products = fetchProducts(productIds);
            validateProductAvailability(products);

            BigDecimal subtotal = calculateSubtotal(products);
            BigDecimal tax = calculateTax(subtotal);
            BigDecimal shipping = estimateShipping(products);
            BigDecimal total = subtotal.add(tax).add(shipping);

            Order order = buildOrder(userId, productIds, total);

            processPayment(order);
            reserveInventory(products);

            String serialized = serializeOrder(order);
            Order stored = deserializeOrder(serialized);
            orders.put(stored.getId(), stored);

            sendConfirmation(stored);

            auditAccess("createOrder", 1);
            return stored;
        } finally {
            span.end();
        }
    }

    public Optional<Order> updateOrderStatus(Long id, String status) {
        Span span = tracer.spanBuilder("OrderService.updateOrderStatus")
                .setAttribute("order.id", id)
                .setAttribute("order.status", status)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            validateRequest();
            Order order = orders.get(id);
            if (order != null) {
                validateStatusTransition(order.getStatus(), status);
                order.setStatus(status);
                String serialized = serializeOrder(order);
                Order updated = deserializeOrder(serialized);
                orders.put(id, updated);
                notifyStatusChange(updated);
                auditAccess("updateOrderStatus", 1);
                return Optional.of(updated);
            }
            return Optional.empty();
        } finally {
            span.end();
        }
    }

    public boolean cancelOrder(Long id) {
        Span span = tracer.spanBuilder("OrderService.cancelOrder").setAttribute("order.id", id).startSpan();
        try (Scope scope = span.makeCurrent()) {
            validateRequest();
            Order order = orders.get(id);
            if (order != null && "PENDING".equals(order.getStatus())) {
                order.setStatus("CANCELLED");
                refundPayment(order);
                releaseInventory(order);
                auditAccess("cancelOrder", 1);
                return true;
            }
            return false;
        } finally {
            span.end();
        }
    }

    private void validateRequest() {
        Span span = tracer.spanBuilder("OrderService.validateRequest").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(2_000_000, 5_000_000);
        } finally {
            span.end();
        }
    }

    private void validateOrderData(Long userId, List<Long> productIds) {
        Span span = tracer.spanBuilder("OrderService.validateOrderData").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(3_000_000, 7_000_000);
        } finally {
            span.end();
        }
    }

    private List<Product> fetchProducts(List<Long> productIds) {
        Span span = tracer.spanBuilder("OrderService.fetchProducts").startSpan();
        try (Scope scope = span.makeCurrent()) {
            List<Product> products = new ArrayList<>();
            for (Long productId : productIds) {
                productService.getProductById(productId).ifPresent(products::add);
            }
            return products;
        } finally {
            span.end();
        }
    }

    private void validateProductAvailability(List<Product> products) {
        Span span = tracer.spanBuilder("OrderService.validateProductAvailability").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(2_500_000, 6_000_000);
        } finally {
            span.end();
        }
    }

    private BigDecimal calculateSubtotal(List<Product> products) {
        Span span = tracer.spanBuilder("OrderService.calculateSubtotal").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(2_000_000, 5_000_000);
            return products.stream()
                    .map(Product::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } finally {
            span.end();
        }
    }

    private BigDecimal calculateTax(BigDecimal subtotal) {
        Span span = tracer.spanBuilder("OrderService.calculateTax").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(1_500_000, 4_000_000);
            return subtotal.multiply(new BigDecimal("0.08"));
        } finally {
            span.end();
        }
    }

    private BigDecimal estimateShipping(List<Product> products) {
        Span span = tracer.spanBuilder("OrderService.estimateShipping").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(2_500_000, 6_000_000);
            return new BigDecimal("9.99");
        } finally {
            span.end();
        }
    }

    private Order buildOrder(Long userId, List<Long> productIds, BigDecimal total) {
        Span span = tracer.spanBuilder("OrderService.buildOrder").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(1_500_000, 3_500_000);
            Order order = new Order();
            order.setId(idGenerator.getAndIncrement());
            order.setUserId(userId);
            order.setProductIds(productIds);
            order.setTotalAmount(total);
            order.setStatus("PENDING");
            order.setCreatedAt(LocalDateTime.now());
            return order;
        } finally {
            span.end();
        }
    }

    private void processPayment(Order order) {
        Span span = tracer.spanBuilder("OrderService.processPayment").startSpan();
        try (Scope scope = span.makeCurrent()) {
            // Submit heavy CPU work to thread pool - simulates async payment processing
            Future<?> paymentFuture = workerPool.submit(() -> {
                // This runs on a DIFFERENT thread - no span context here!
                cpuWork(5_000_000, 12_000_000);
            });
            try {
                paymentFuture.get(); // Wait for completion
            } catch (Exception e) {
                throw new RuntimeException("Payment processing failed", e);
            }
        } finally {
            span.end();
        }
    }

    private void reserveInventory(List<Product> products) {
        Span span = tracer.spanBuilder("OrderService.reserveInventory").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(3_000_000, 7_000_000);
        } finally {
            span.end();
        }
    }

    private void sendConfirmation(Order order) {
        Span span = tracer.spanBuilder("OrderService.sendConfirmation").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(2_000_000, 5_000_000);
        } finally {
            span.end();
        }
    }

    private List<Order> enrichOrders(List<Order> orders) {
        Span span = tracer.spanBuilder("OrderService.enrichOrders").startSpan();
        try (Scope scope = span.makeCurrent()) {
            // Submit CPU work to thread pool - simulates production async behavior
            // The worker thread will NOT have the span context set by setTracingContext
            List<Future<Order>> futures = new ArrayList<>();
            for (Order order : orders) {
                Future<Order> future = workerPool.submit(() -> {
                    // This runs on a DIFFERENT thread - no span context here!
                    cpuWork(3_500_000, 8_000_000);
                    return cloneOrder(order);
                });
                futures.add(future);
            }

            // Collect results
            List<Order> result = new ArrayList<>();
            for (Future<Order> future : futures) {
                try {
                    result.add(future.get());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to enrich order", e);
                }
            }
            return result;
        } finally {
            span.end();
        }
    }

    private Order cloneOrder(Order order) {
        Span span = tracer.spanBuilder("OrderService.cloneOrder").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String json = serializeOrder(order);
            return deserializeOrder(json);
        } finally {
            span.end();
        }
    }

    private void enrichOrderDetails(Order order) {
        Span span = tracer.spanBuilder("OrderService.enrichOrderDetails").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(2_500_000, 6_000_000);
        } finally {
            span.end();
        }
    }

    private void calculateShipping(Order order) {
        Span span = tracer.spanBuilder("OrderService.calculateShipping").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(2_000_000, 5_000_000);
        } finally {
            span.end();
        }
    }

    private void validateStatusTransition(String from, String to) {
        Span span = tracer.spanBuilder("OrderService.validateStatusTransition").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(1_500_000, 3_500_000);
        } finally {
            span.end();
        }
    }

    private void notifyStatusChange(Order order) {
        Span span = tracer.spanBuilder("OrderService.notifyStatusChange").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(2_000_000, 5_000_000);
        } finally {
            span.end();
        }
    }

    private void refundPayment(Order order) {
        Span span = tracer.spanBuilder("OrderService.refundPayment").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(4_000_000, 9_000_000);
        } finally {
            span.end();
        }
    }

    private void releaseInventory(Order order) {
        Span span = tracer.spanBuilder("OrderService.releaseInventory").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(2_500_000, 6_000_000);
        } finally {
            span.end();
        }
    }

    private String serializeOrder(Order order) {
        Span span = tracer.spanBuilder("OrderService.serializeOrder").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(1_500_000, 3_500_000);
            return objectMapper.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            span.recordException(e);
            throw new RuntimeException("Serialization failed", e);
        } finally {
            span.end();
        }
    }

    private Order deserializeOrder(String json) {
        Span span = tracer.spanBuilder("OrderService.deserializeOrder").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(1_500_000, 3_500_000);
            return objectMapper.readValue(json, Order.class);
        } catch (JsonProcessingException e) {
            span.recordException(e);
            throw new RuntimeException("Deserialization failed", e);
        } finally {
            span.end();
        }
    }

    private void auditAccess(String operation, int count) {
        Span span = tracer.spanBuilder("OrderService.auditAccess")
                .setAttribute("operation", operation)
                .setAttribute("count", count)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(1_000_000, 2_500_000);
        } finally {
            span.end();
        }
    }

    private void cpuWork(int minIterations, int maxIterations) {
        int iterations = ThreadLocalRandom.current().nextInt(minIterations, maxIterations);
        double sum = 0;
        for (int i = 0; i < iterations; i++) {
            sum += Math.sqrt(i) * Math.tan(i * 0.0001) * Math.sin(i);
            if (i % 800 == 0) {
                sum = Math.cbrt(Math.abs(sum) + 1);
            }
        }
    }
}
