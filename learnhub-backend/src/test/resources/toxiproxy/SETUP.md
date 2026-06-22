# Toxiproxy Setup Guide - Network Resilience Testing

## Overview

Toxiproxy is a tool that simulates network conditions and failures for testing application resilience.

**Use Cases:**
- Simulate network latency (1-2s delays)
- Simulate connection loss / timeouts
- Simulate packet loss
- Test error recovery and retry mechanisms
- Verify graceful degradation under adverse conditions

## Installation

### Option 1: Docker (Recommended)

```bash
docker run --name toxiproxy -p 8474:8474 -p 20000:20000 shopify/toxiproxy:2.5.0
```

### Option 2: Local Binary

```bash
# Download from GitHub
wget https://github.com/Shopify/toxiproxy/releases/download/v2.5.0/toxiproxy-server-linux-amd64
chmod +x toxiproxy-server-linux-amd64
./toxiproxy-server-linux-amd64

# In another terminal, control via CLI
toxiproxy-cli
```

## Configuration

### Proxied Services

Toxiproxy sits between your test client and the target services, allowing you to inject faults.

#### 1. Redis Proxy

```bash
# Create Redis proxy
toxiproxy-cli toxic add \
  -t toxiproxy_testcontainers \
  -n redis_latency \
  -a latency:1000 \
  redis:6379

# Enable latency (1000ms)
toxiproxy-cli toxic update \
  -p redis \
  -t latency \
  -n redis_latency \
  -a latency:1000 -a jitter:100
```

**Example Fault Patterns:**

- **Latency Injection:**
  ```bash
  toxiproxy-cli toxic add -t redis -n latency -a latency:1500 -a jitter:200
  ```
  Adds 1500ms ± 200ms delay to all Redis operations

- **Connection Loss:**
  ```bash
  toxiproxy-cli toxic add -t redis -n timeout -a timeout:5000
  ```
  Timeout after 5 seconds (simulates stuck connections)

- **Packet Loss:**
  ```bash
  toxiproxy-cli toxic add -t redis -n packet_loss -a percentage:10
  ```
  Drop 10% of packets (simulates poor network)

#### 2. HTTP Proxy (for Assessment API)

```bash
# Create proxy for assessment endpoints
toxiproxy-cli toxic add \
  -t toxiproxy_http \
  -n progress_latency \
  -a latency:2000 \
  localhost:8080
```

**Example HTTP Fault Patterns:**

- **Slow Progress Endpoint (2s latency):**
  ```bash
  toxiproxy-cli toxic add \
    -t http \
    -n slow_progress \
    -a latency:2000 \
    -a pattern:"GET /api/v1/assessments/.*/progress"
  ```

- **Intermittent Failures (5% of requests):**
  ```bash
  toxiproxy-cli toxic add \
    -t http \
    -n intermittent_fail \
    -a percentage:5 \
    -a pattern:"GET /api/v1/assessments/.*/progress"
  ```

- **Bandwidth Throttling:**
  ```bash
  toxiproxy-cli toxic add \
    -t http \
    -n throttle \
    -a rate:1000 \
    -a pattern:"GET /api/v1/assessments/.*/progress/stream"
  ```
  Limits to 1KB/s (SSE will be very slow)

## Integration with Tests

### Java/JUnit Integration

**Step 1: Configure test properties to use Toxiproxy**

```yaml
# src/test/resources/application-test.yml
spring:
  redis:
    host: localhost
    port: 20000  # Toxiproxy port (instead of 6379)
  
  datasource:
    url: jdbc:postgresql://localhost:20001/learnhub_test
    # (If using PostgreSQL through Toxiproxy)
```

**Step 2: Create Toxiproxy fixture**

```java
// src/test/java/com/learnhub/assessment/test/ToxiproxyFixture.java
@Slf4j
public class ToxiproxyFixture {
  private static final String TOXIPROXY_URL = "http://localhost:8474";
  private static final RestTemplate restTemplate = new RestTemplate();

  public static void injectRedisLatency(long latencyMs, long jitterMs) throws Exception {
    // Configure Redis proxy with latency via Toxiproxy API
    String payload = String.format(
      "{\"name\": \"latency\", \"type\": \"latency\", " +
      "\"attributes\": {\"latency\": %d, \"jitter\": %d}}",
      latencyMs, jitterMs
    );
    
    restTemplate.postForObject(
      TOXIPROXY_URL + "/proxies/redis/toxics",
      payload,
      String.class
    );
    
    log.info("Injected Redis latency: {}ms ± {}ms", latencyMs, jitterMs);
  }

  public static void disableRedisProxy() throws Exception {
    // Remove all toxics from Redis proxy
    restTemplate.delete(TOXIPROXY_URL + "/proxies/redis/toxics");
    log.info("Disabled Redis proxy faults");
  }
}
```

**Step 3: Use in resilience tests**

```java
@Test
void testAssessmentContinuesUnderLatency() throws Exception {
  // Given: Redis latency injected (1.5s)
  ToxiproxyFixture.injectRedisLatency(1500, 200);
  
  try {
    // When: Job enqueued and dequeued
    AssessmentJob job = new AssessmentJob(UUID.randomUUID(), UUID.randomUUID(), "1.0.0");
    jobQueue.enqueue(job);
    
    // Then: Job still dequeued successfully (slowly)
    Optional<AssessmentJob> dequeued = jobQueue.dequeue(Duration.ofSeconds(5));
    assertTrue(dequeued.isPresent());
  } finally {
    // Clean up
    ToxiproxyFixture.disableRedisProxy();
  }
}
```

## Toxiproxy REST API

### Common Endpoints

**List proxies:**
```bash
curl http://localhost:8474/proxies
```

**Add toxic to proxy:**
```bash
curl -X POST http://localhost:8474/proxies/redis/toxics \
  -H "Content-Type: application/json" \
  -d '{
    "name": "latency",
    "type": "latency",
    "attributes": {"latency": 1500, "jitter": 100}
  }'
```

**Remove toxic:**
```bash
curl -X DELETE http://localhost:8474/proxies/redis/toxics/latency
```

**Update toxic:**
```bash
curl -X PATCH http://localhost:8474/proxies/redis/toxics/latency \
  -H "Content-Type: application/json" \
  -d '{"attributes": {"latency": 2000, "jitter": 150}}'
```

## Test Scenarios

### Scenario 1: Slow Assessment Progress

**Setup:**
```bash
# 2s latency on progress endpoint
toxiproxy-cli toxic add \
  -t http_progress \
  -n latency \
  -a latency:2000
```

**Test Expectation:**
- Progress updates take 2s longer
- Client should handle timeouts gracefully
- No assessment data loss

### Scenario 2: Network Partition (Total Loss)

**Setup:**
```bash
# Drop 100% of packets for 5 seconds
toxiproxy-cli toxic add \
  -t redis \
  -n packet_loss \
  -a percentage:100 \
  -a timeout:5000
```

**Test Expectation:**
- Job dequeue fails with timeout
- Retry logic triggered
- Job remains in queue for retry
- No duplicate processing

### Scenario 3: Intermittent Failures (Chaos)

**Setup:**
```bash
# Drop 10% of packets randomly
toxiproxy-cli toxic add \
  -t redis \
  -n chaos \
  -a percentage:10
```

**Test Expectation:**
- Some operations fail intermittently
- Retry logic handles failures
- Assessment eventually completes
- No permanent data corruption

### Scenario 4: SSE Connection Loss

**Setup:**
```bash
# Drop 50% of SSE packets (simulates poor connection)
toxiproxy-cli toxic add \
  -t http_sse \
  -n packet_loss \
  -a percentage:50
```

**Test Expectation:**
- SSE events lost intermittently
- Client detects connection issues
- Fallback to polling activated
- Progress continues to update

## Cleanup

### Reset all proxies

```bash
toxiproxy-cli reset
```

### Stop Toxiproxy container

```bash
docker stop toxiproxy
docker rm toxiproxy
```

## Best Practices

1. **Isolate Faults:** Each test should add/remove its own toxics (don't depend on shared state)

2. **Measure Impact:** Document expected performance degradation:
   - Base latency: 50ms
   - With 1s injection: ~1050ms

3. **Reasonable Bounds:** 
   - Keep injected latency < 5s (tests should complete)
   - Keep packet loss < 50% (network should be recoverable)

4. **Clean Up:** Always remove toxics in `@AfterEach` or try-finally

5. **Monitor:** Watch test durations - injected delays add to total test runtime

## Troubleshooting

**Issue: Port 20000 already in use**
```bash
lsof -i :20000
kill -9 <PID>
```

**Issue: Toxiproxy doesn't start**
```bash
# Check logs
docker logs toxiproxy

# Verify port bindings
docker port toxiproxy
```

**Issue: Proxy doesn't intercept traffic**
- Verify test config points to toxiproxy port (20000) not original (6379)
- Check proxies exist: `toxiproxy-cli list`
- Verify toxics are enabled: `toxiproxy-cli inspect redis`

## References

- [Toxiproxy GitHub](https://github.com/Shopify/toxiproxy)
- [Toxiproxy Documentation](https://github.com/Shopify/toxiproxy/wiki)
- [Network Resilience Testing](https://martinfowler.com/articles/patterns-of-resilient-interaction.html)
