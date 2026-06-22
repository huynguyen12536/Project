# TEST SCENARIOS - LearnHub Real-time Assessment Integration

**Status:** ✅ System UP and Running  
**Date:** 2026-06-22  
**Services:** PostgreSQL, Redis, Prometheus, Grafana

---

## 🚀 **QUICK START - NGAY BÂY GIỜ**

### **Step 1: Verify Services Running**
```bash
docker-compose -f docker-compose.monitoring.yml ps
```

Expected output: All services showing `Up` and mostly `healthy`

### **Step 2: Access Prometheus**
```
Browser: http://localhost:9090
```

Click: Status → Targets  
You should see: PostgreSQL and Redis targets available

### **Step 3: Access Grafana Dashboard**
```
Browser: http://localhost:3001
Login: admin / admin
```

Navigate to: Dashboards → LearnHub Production  
You should see: 10 dashboard panels (initially empty - waiting for backend metrics)

---

## 📊 **TEST SCENARIOS**

### **SCENARIO 1: Verify Prometheus is Scraping Targets**

**URL:** http://localhost:9090/api/v1/targets

**Test Command:**
```bash
curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job: .job, url: .scrapeUrl}'
```

**Expected:**
- job: `learnhub-backend-actuator`
- url: `http://backend:8080/actuator/prometheus`

**Status:** Will show when backend is running

---

### **SCENARIO 2: Query Basic Metrics (Prometheus)**

**URL:** http://localhost:9090/api/v1/query

**Test 1: Check Prometheus UP Status**
```bash
curl 'http://localhost:9090/api/v1/query?query=up{job="prometheus"}'
```

Expected: `result: [{"value": [timestamp, "1"]}]`

**Test 2: Query JVM Metrics (when backend running)**
```bash
curl 'http://localhost:9090/api/v1/query?query=jvm_memory_used_bytes'
```

Expected: JVM memory metrics in bytes

**Test 3: Query HTTP Requests (when backend running)**
```bash
curl 'http://localhost:9090/api/v1/query?query=http_requests_total'
```

Expected: HTTP request count across all endpoints

---

### **SCENARIO 3: Grafana Dashboard Panels**

**Access:** http://localhost:3001 → Dashboards → LearnHub Production

**10 Panels to Verify:**

1. **LLM API Latency**
   - Displays: p95, p99, average latency
   - Target: < 5s p95
   - Status: Will show metrics once backend makes LLM calls

2. **LLM API Error Rates**
   - Displays: 5xx errors, rate limits (429), timeouts (408)
   - Target: < 0.1 errors/sec
   - Status: Will show errors if any

3. **SSE Connections**
   - Displays: Active connection count
   - Target: < 5 concurrent
   - Status: Will show count once clients connect

4. **SSE Drop Rates**
   - Displays: Disconnect %, polling fallback %
   - Target: < 1% drop
   - Status: Will show reliability metrics

5. **Worker Pool Saturation**
   - Displays: Live threads, peak threads
   - Target: 2-4 threads
   - Status: Will show thread usage

6. **Worker Pool Tasks**
   - Displays: Queue size, tasks/sec, failure rate
   - Target: Queue < 100
   - Status: Will show async task metrics

7. **Database GIN Index Performance**
   - Displays: p95, p99, avg query time
   - Target: < 50ms p95
   - Status: Will show query performance

8. **End-to-End Assessment Time**
   - Displays: p95, p99, avg duration
   - Target: 3-6s average
   - Status: Will show assessment latency

9. **JVM Memory & Heap**
   - Displays: Heap usage (max 2GB)
   - Target: < 1.5GB
   - Status: Will show memory trends

10. **HTTP Request Rate**
    - Displays: Throughput (requests/sec)
    - Target: Monitor overall load
    - Status: Will show API throughput

---

### **SCENARIO 4: Test API Endpoints (When Backend Running)**

**Backend Health Check:**
```bash
curl http://localhost:8080/health
```

**Expected Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"}
  }
}
```

---

### **SCENARIO 5: Database Connectivity**

**PostgreSQL Connection:**
```bash
docker-compose -f docker-compose.monitoring.yml exec postgres psql -U postgres -c "SELECT version();"
```

**Expected:** PostgreSQL version info

**Check Databases:**
```bash
docker-compose -f docker-compose.monitoring.yml exec postgres psql -U postgres -l
```

**Expected:** learnhub database listed

---

### **SCENARIO 6: Redis Connectivity**

**Redis PING:**
```bash
docker-compose -f docker-compose.monitoring.yml exec redis redis-cli ping
```

**Expected:** PONG

**Check Redis Memory:**
```bash
docker-compose -f docker-compose.monitoring.yml exec redis redis-cli info memory
```

**Expected:** Memory stats (used_memory, max_memory, etc.)

---

### **SCENARIO 7: Full Integration Test (When Backend Added)**

**Scenario:**
1. Submit an assessment via API
2. Watch progress stream via SSE
3. Check Prometheus metrics  
4. View results in Grafana dashboard

**Step-by-step:**
```bash
# 1. Submit assessment
curl -X POST http://localhost:8080/api/assessments \
  -H "Content-Type: application/json" \
  -d '{"repositoryId": "test-repo", "userId": "test-user"}'

# 2. Stream progress (in another terminal)
curl -N http://localhost:8080/api/assessments/{assessmentId}/progress

# 3. Query Prometheus for metrics
curl 'http://localhost:9090/api/v1/query?query=assessment_duration_seconds'

# 4. View Grafana panel
# Browser: http://localhost:3001 → Dashboard → End-to-End Assessment Time
```

---

## 🔧 **TROUBLESHOOTING**

### **Prometheus Not Scraping Backend**
```bash
# Check target status
curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | select(.job=="learnhub-backend-actuator")'

# Expected: job shows "down" until backend starts
# Once backend starts: job will show "up"
```

### **Grafana Datasource Not Connected**
```bash
# Check Prometheus health from Grafana
curl -X POST http://localhost:3001/api/datasources \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Prometheus", "type":"prometheus", "url":"http://prometheus:9090"}'
```

### **Services Not Starting**
```bash
# Check logs
docker-compose -f docker-compose.monitoring.yml logs

# Check specific service
docker-compose -f docker-compose.monitoring.yml logs prometheus
docker-compose -f docker-compose.monitoring.yml logs grafana
```

### **Port Already in Use**
```bash
# Stop all services
docker-compose -f docker-compose.monitoring.yml down

# Free ports
sudo lsof -i :9090  # Prometheus
sudo lsof -i :3001  # Grafana
sudo lsof -i :5432  # PostgreSQL
sudo lsof -i :6379  # Redis

# Kill process if needed
kill -9 <PID>
```

---

## ✅ **SUCCESS CRITERIA**

- [ ] All 4 services running (PostgreSQL, Redis, Prometheus, Grafana)
- [ ] Prometheus health check: http://localhost:9090/-/healthy returns 200
- [ ] Grafana login works: http://localhost:3001 (admin/admin)
- [ ] Grafana loads dashboard: 10 panels visible
- [ ] Prometheus targets page shows available targets
- [ ] Can query basic metrics via Prometheus API
- [ ] PostgreSQL database accessible
- [ ] Redis accessible and responding

---

## 🎯 **NEXT STEPS**

**After verifying monitoring stack:**

1. ✅ Backend deployment (when JAR is ready)
   - Update docker-compose.yml to include backend service
   - Start backend: `docker-compose up -d backend`

2. ✅ Submit assessments via API
   - Use assessment endpoints to generate metrics
   - Watch Grafana dashboard update in real-time

3. ✅ Set up alerting (optional)
   - Create alert rules in Prometheus
   - Configure Slack notifications
   - Test alert triggers

4. ✅ Performance testing
   - Load test the system
   - Monitor metrics during load
   - Identify bottlenecks using Grafana

---

**STATUS: ✅ READY FOR TESTING**

All monitoring infrastructure is in place and running. Proceed with backend deployment when ready.
