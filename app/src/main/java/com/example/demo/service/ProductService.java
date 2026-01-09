package com.example.demo.service;

import com.example.demo.model.Product;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Tracer tracer = GlobalOpenTelemetry.getTracer("ProductService");

    public ProductService() {
        createProduct(new Product(null, "Laptop", "High-performance laptop", new BigDecimal("999.99")));
        createProduct(new Product(null, "Phone", "Latest smartphone", new BigDecimal("699.99")));
        createProduct(new Product(null, "Headphones", "Noise-cancelling headphones", new BigDecimal("299.99")));
        createProduct(new Product(null, "Tablet", "10-inch tablet", new BigDecimal("449.99")));
        createProduct(new Product(null, "Watch", "Smart watch", new BigDecimal("349.99")));
    }

    public List<Product> getAllProducts() {
        Span span = tracer.spanBuilder("ProductService.getAllProducts").startSpan();
        try (Scope scope = span.makeCurrent()) {
            validateRequest();
            List<Product> productList = new ArrayList<>(products.values());
            List<Product> enriched = enrichProducts(productList);
            List<Product> cached = cacheProducts(enriched);
            auditAccess("getAllProducts", productList.size());
            return cached;
        } finally {
            span.end();
        }
    }

    public Optional<Product> getProductById(Long id) {
        Span span = tracer.spanBuilder("ProductService.getProductById").setAttribute("product.id", id).startSpan();
        try (Scope scope = span.makeCurrent()) {
            validateRequest();
            Optional<Product> product = Optional.ofNullable(products.get(id));
            product.ifPresent(p -> {
                calculateDynamicPricing(p);
                checkInventory(p);
                auditAccess("getProductById", 1);
            });
            return product;
        } finally {
            span.end();
        }
    }

    public Product createProduct(Product product) {
        Span span = tracer.spanBuilder("ProductService.createProduct").startSpan();
        try (Scope scope = span.makeCurrent()) {
            validateProductData(product);
            Long id = idGenerator.getAndIncrement();
            product.setId(id);
            Product indexed = indexProduct(product);
            auditAccess("createProduct", 1);
            return indexed;
        } finally {
            span.end();
        }
    }

    public Optional<Product> updateProduct(Long id, Product product) {
        Span span = tracer.spanBuilder("ProductService.updateProduct").setAttribute("product.id", id).startSpan();
        try (Scope scope = span.makeCurrent()) {
            validateProductData(product);
            if (products.containsKey(id)) {
                product.setId(id);
                Product indexed = indexProduct(product);
                invalidateCache(id);
                auditAccess("updateProduct", 1);
                return Optional.of(indexed);
            }
            return Optional.empty();
        } finally {
            span.end();
        }
    }

    public boolean deleteProduct(Long id) {
        Span span = tracer.spanBuilder("ProductService.deleteProduct").setAttribute("product.id", id).startSpan();
        try (Scope scope = span.makeCurrent()) {
            validateRequest();
            boolean removed = products.remove(id) != null;
            if (removed) {
                invalidateCache(id);
                auditAccess("deleteProduct", 1);
            }
            return removed;
        } finally {
            span.end();
        }
    }

    public List<Product> searchProducts(String query) {
        Span span = tracer.spanBuilder("ProductService.searchProducts").setAttribute("search.query", query).startSpan();
        try (Scope scope = span.makeCurrent()) {
            validateRequest();
            List<Product> allProducts = new ArrayList<>(products.values());

            String jsonIndex = buildSearchIndex(allProducts);
            List<Product> indexed = parseSearchIndex(jsonIndex);

            List<Product> filtered = filterProducts(indexed, query);
            List<Product> ranked = rankResults(filtered, query);

            auditAccess("searchProducts", ranked.size());
            return ranked;
        } finally {
            span.end();
        }
    }

    private void validateRequest() {
        Span span = tracer.spanBuilder("ProductService.validateRequest").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(2_000_000, 5_000_000);
        } finally {
            span.end();
        }
    }

    private void validateProductData(Product product) {
        Span span = tracer.spanBuilder("ProductService.validateProductData").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(2_500_000, 6_000_000);
        } finally {
            span.end();
        }
    }

    private List<Product> enrichProducts(List<Product> products) {
        Span span = tracer.spanBuilder("ProductService.enrichProducts").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(4_000_000, 9_000_000);
            return products.stream()
                    .map(this::cloneProduct)
                    .collect(Collectors.toList());
        } finally {
            span.end();
        }
    }

    private Product cloneProduct(Product product) {
        Span span = tracer.spanBuilder("ProductService.cloneProduct").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String json = serializeProduct(product);
            return deserializeProduct(json);
        } finally {
            span.end();
        }
    }

    private List<Product> cacheProducts(List<Product> products) {
        Span span = tracer.spanBuilder("ProductService.cacheProducts").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(1_500_000, 3_500_000);
            return products;
        } finally {
            span.end();
        }
    }

    private void calculateDynamicPricing(Product product) {
        Span span = tracer.spanBuilder("ProductService.calculateDynamicPricing").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(3_000_000, 7_000_000);
        } finally {
            span.end();
        }
    }

    private void checkInventory(Product product) {
        Span span = tracer.spanBuilder("ProductService.checkInventory").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(2_000_000, 5_000_000);
        } finally {
            span.end();
        }
    }

    private Product indexProduct(Product product) {
        Span span = tracer.spanBuilder("ProductService.indexProduct").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(3_500_000, 8_000_000);
            String json = serializeProduct(product);
            Product indexed = deserializeProduct(json);
            products.put(indexed.getId(), indexed);
            return indexed;
        } finally {
            span.end();
        }
    }

    private void invalidateCache(Long id) {
        Span span = tracer.spanBuilder("ProductService.invalidateCache").setAttribute("product.id", id).startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(1_000_000, 2_500_000);
        } finally {
            span.end();
        }
    }

    private String buildSearchIndex(List<Product> products) {
        Span span = tracer.spanBuilder("ProductService.buildSearchIndex").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(5_000_000, 12_000_000);
            return objectMapper.writeValueAsString(products);
        } catch (JsonProcessingException e) {
            span.recordException(e);
            throw new RuntimeException("Failed to build search index", e);
        } finally {
            span.end();
        }
    }

    private List<Product> parseSearchIndex(String json) {
        Span span = tracer.spanBuilder("ProductService.parseSearchIndex").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(4_000_000, 10_000_000);
            return objectMapper.readValue(json, new TypeReference<List<Product>>() {});
        } catch (JsonProcessingException e) {
            span.recordException(e);
            throw new RuntimeException("Failed to parse search index", e);
        } finally {
            span.end();
        }
    }

    private List<Product> filterProducts(List<Product> products, String query) {
        Span span = tracer.spanBuilder("ProductService.filterProducts").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(3_000_000, 7_000_000);
            return products.stream()
                    .filter(p -> p.getName().toLowerCase().contains(query.toLowerCase()) ||
                            p.getDescription().toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList());
        } finally {
            span.end();
        }
    }

    private List<Product> rankResults(List<Product> products, String query) {
        Span span = tracer.spanBuilder("ProductService.rankResults").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(4_000_000, 9_000_000);
            return products;
        } finally {
            span.end();
        }
    }

    private String serializeProduct(Product product) {
        Span span = tracer.spanBuilder("ProductService.serializeProduct").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(1_000_000, 2_500_000);
            return objectMapper.writeValueAsString(product);
        } catch (JsonProcessingException e) {
            span.recordException(e);
            throw new RuntimeException("Serialization failed", e);
        } finally {
            span.end();
        }
    }

    private Product deserializeProduct(String json) {
        Span span = tracer.spanBuilder("ProductService.deserializeProduct").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(1_000_000, 2_500_000);
            return objectMapper.readValue(json, Product.class);
        } catch (JsonProcessingException e) {
            span.recordException(e);
            throw new RuntimeException("Deserialization failed", e);
        } finally {
            span.end();
        }
    }

    private void auditAccess(String operation, int count) {
        Span span = tracer.spanBuilder("ProductService.auditAccess")
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
            sum += Math.sqrt(i) * Math.cos(i) * Math.tan(i * 0.001);
            if (i % 500 == 0) {
                sum = Math.atan(sum);
            }
        }
    }
}
