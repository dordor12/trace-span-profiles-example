 package com.example.demo.controller;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Test controller for span profile correlation testing.
 * All spans run on worker threads (not the request thread) with 2-10ms CPU work.
 */
@RestController
@RequestMapping("/api/span-test")
public class SpanProfileTestController {

    private final ExecutorService executor = Executors.newFixedThreadPool(8);
    private volatile Tracer tracer;

    @PostConstruct
    public void init() {
        tracer = GlobalOpenTelemetry.getTracer("span-profile-test");
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * Test endpoint that creates multiple spans on worker threads.
     * Each span does CPU-intensive work for 2-10ms.
     *
     * Usage: GET /api/span-test/worker-spans?count=5&durationMs=5
     */
    @GetMapping("/worker-spans")
    public SpanTestResult testWorkerSpans(
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(defaultValue = "5") int durationMs) throws Exception {

        String requestThread = Thread.currentThread().getName();
        List<SpanInfo> spanInfos = new ArrayList<>();
        List<Future<SpanInfo>> futures = new ArrayList<>();

        // Submit all spans to worker threads
        for (int i = 0; i < count; i++) {
            final int spanIndex = i;
            final Context parentContext = Context.current(); // Capture parent context

            Future<SpanInfo> future = executor.submit(() -> {
                // Propagate context to worker thread
                try (Scope scope = parentContext.makeCurrent()) {
                    return executeSpanOnWorkerThread(spanIndex, durationMs);
                }
            });
            futures.add(future);
        }

        // Wait for all spans to complete
        for (Future<SpanInfo> future : futures) {
            spanInfos.add(future.get());
        }

        return new SpanTestResult(requestThread, spanInfos);
    }

    /**
     * Sequential version - spans run one after another on worker threads
     */
    @GetMapping("/worker-spans-sequential")
    public SpanTestResult testWorkerSpansSequential(
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(defaultValue = "5") int durationMs) throws Exception {

        String requestThread = Thread.currentThread().getName();
        List<SpanInfo> spanInfos = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            final int spanIndex = i;
            final Context parentContext = Context.current();

            Future<SpanInfo> future = executor.submit(() -> {
                try (Scope scope = parentContext.makeCurrent()) {
                    return executeSpanOnWorkerThread(spanIndex, durationMs);
                }
            });
            spanInfos.add(future.get()); // Wait for each before starting next
        }

        return new SpanTestResult(requestThread, spanInfos);
    }

    private SpanInfo executeSpanOnWorkerThread(int spanIndex, int durationMs) {
        String workerThread = Thread.currentThread().getName();
        String spanName = "worker-span-" + spanIndex;

        Span span = tracer.spanBuilder(spanName)
                .startSpan();

        long startTime = System.nanoTime();
        try (Scope scope = span.makeCurrent()) {
            // Do CPU-intensive work for the specified duration
            long targetNanos = durationMs * 1_000_000L;
            long sum = 0;
            while (System.nanoTime() - startTime < targetNanos) {
                sum = cpuIntensiveWork(sum);
            }

            // Record the actual duration
            long actualDurationNs = System.nanoTime() - startTime;

            return new SpanInfo(
                    spanName,
                    span.getSpanContext().getSpanId(),
                    workerThread,
                    actualDurationNs / 1_000_000.0, // Convert to ms
                    sum // Prevent JIT optimization
            );
        } finally {
            span.end();
        }
    }

    /**
     * CPU-intensive work to generate profiler samples
     */
    private long cpuIntensiveWork(long input) {
        long result = input;
        for (int i = 0; i < 1000; i++) {
            result = result * 31 + i;
            result = Long.rotateLeft(result, 13);
            result ^= result >>> 7;
        }
        return result;
    }

    // Response classes
    public static class SpanTestResult {
        public final String requestThread;
        public final List<SpanInfo> spans;
        public final int totalSpans;

        public SpanTestResult(String requestThread, List<SpanInfo> spans) {
            this.requestThread = requestThread;
            this.spans = spans;
            this.totalSpans = spans.size();
        }
    }

    public static class SpanInfo {
        public final String spanName;
        public final String spanId;
        public final String workerThread;
        public final double durationMs;
        public final long checksum;

        public SpanInfo(String spanName, String spanId, String workerThread, double durationMs, long checksum) {
            this.spanName = spanName;
            this.spanId = spanId;
            this.workerThread = workerThread;
            this.durationMs = durationMs;
            this.checksum = checksum;
        }
    }
}
