# Kafka Migration Testing Guide

## Prerequisites

1. Docker and Docker Compose installed
2. Java 17 installed
3. Maven 3.8+ installed
4. Kafka CLI tools (optional, for manual testing)

## Step 1: Start Infrastructure

```bash
# Navigate to CashCached root directory
cd c:\Users\hp\Desktop\CashCached

# Start Kafka, Zookeeper, MySQL, and Kafdrop using docker-compose
docker-compose -f docker-compose-kafka.yml up -d

# Verify containers are running
docker-compose -f docker-compose-kafka.yml ps
```

Expected output:

```
zookeeper    UP      2181/tcp
kafka        UP      9092/tcp, 9094/tcp
kafdrop      UP      9000/tcp
mysql        UP      3306/tcp
```

## Step 2: Build All Services

```bash
# Clean and build all modules
mvn clean install -DskipTests -q
```

## Step 3: Start Microservices

In separate terminal windows, start each service:

### Terminal 1 - Customer Service

```bash
cd customer
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

### Terminal 2 - Product Service

```bash
cd product-pricing
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"
```

### Terminal 3 - FD Calculator Service

```bash
cd fd-calculator
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8083"
```

### Terminal 4 - Accounts Service

```bash
cd accounts
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8084"
```

## Step 4: Monitor Kafka Topics

### Option A: Using Kafdrop UI

Open browser: http://localhost:9000

- View all topics
- Monitor messages in real-time
- Check consumer groups

### Option B: Using Kafka CLI

```bash
# List all topics
docker-compose -f docker-compose-kafka.yml exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Monitor customer.validation.request topic
docker-compose -f docker-compose-kafka.yml exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic customer.validation.request \
  --from-beginning

# Monitor customer.validation.response topic (in another terminal)
docker-compose -f docker-compose-kafka.yml exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic customer.validation.response \
  --from-beginning
```

## Step 5: Test FD Calculation Flow

### 5.1 Authentication

First, get an auth token (if authentication is enabled):

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user@example.com",
    "password": "password"
  }'
```

### 5.2 Create FD Calculation Request

```bash
# Without authentication (for testing)
curl -X POST http://localhost:8084/api/v1/accounts/fd-calculation \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dummy-token" \
  -d '{
    "customerId": 1,
    "productCode": "FD-001",
    "principalAmount": 100000.00,
    "tenureMonths": 12,
    "compoundingFrequency": 4
  }' | jq .
```

### 5.3 Expected Response

```json
{
  "id": 1,
  "customerId": 1,
  "productCode": "FD-001",
  "productName": "Premium Fixed Deposit",
  "principalAmount": 100000.0,
  "maturityAmount": 107520.29,
  "interestEarned": 7520.29,
  "interestRate": 7.5,
  "effectiveRate": 7.75,
  "tenureMonths": 12,
  "compoundingFrequency": 4,
  "currency": "INR",
  "calculationDate": "2025-10-30T12:00:00",
  "createdAt": "2025-10-30T12:00:00"
}
```

## Step 6: Verify Kafka Message Flow

### Expected Message Flow

1. **Accounts → Kafka (customer.validation.request)**

   ```json
   {
     "customerId": 1,
     "requestId": "uuid-1234",
     "timestamp": "2025-10-30T12:00:00"
   }
   ```

2. **FD-Calculator ← Kafka (receives request)**

   - Processes validation via Feign client (bridge)
   - Publishes response

3. **Kafka ← FD-Calculator (customer.validation.response)**

   ```json
   {
     "customerId": 1,
     "requestId": "uuid-1234",
     "valid": true,
     "active": true,
     "timestamp": "2025-10-30T12:00:00"
   }
   ```

4. **Accounts ← Kafka (receives response)**
   - Continues with product validation
   - Similar flow for product details

## Step 7: Health Checks

### Check Service Health

```bash
# Customer Service
curl http://localhost:8081/actuator/health

# Product Service
curl http://localhost:8082/actuator/health

# FD Calculator Service
curl http://localhost:8083/actuator/health

# Accounts Service
curl http://localhost:8084/actuator/health
```

### Check Kafka Connectivity

```bash
# From any service logs, verify this appears:
# "Kafka topics created successfully"
# "Consumer group registered: fd-calculator-service"
```

## Step 8: Troubleshooting

### Issue: Services can't connect to Kafka

**Solution:**

```bash
# Verify Kafka is running
docker-compose -f docker-compose-kafka.yml ps

# Check Kafka logs
docker-compose -f docker-compose-kafka.yml logs kafka

# Verify bootstrap servers setting
# Should be localhost:9092 or kafka:9092 (if in same network)
```

### Issue: Timeout on requests

**Causes:**

1. Kafka broker not responding
2. Consumer service not listening
3. Timeout too short

**Solution:**

```bash
# Increase timeout in application.yml
app:
  kafka:
    request-timeout-seconds: 60  # Increase from default 30

# Restart service
```

### Issue: Messages not appearing on topics

**Solution:**

```bash
# Check if topics were created
docker-compose -f docker-compose-kafka.yml exec kafka \
  kafka-topics --list --bootstrap-server localhost:9092

# Manually create missing topics
docker-compose -f docker-compose-kafka.yml exec kafka \
  kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic customer.validation.request \
  --partitions 3 \
  --replication-factor 1
```

### Issue: Consumer lag increasing

**Solution:**

```bash
# Check consumer group status
docker-compose -f docker-compose-kafka.yml exec kafka \
  kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group fd-calculator-service \
  --describe

# If stuck, reset offset (CAUTION - loses messages)
docker-compose -f docker-compose-kafka.yml exec kafka \
  kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group fd-calculator-service \
  --reset-offsets \
  --to-earliest \
  --execute
```

## Step 9: Load Testing

### Test with Multiple Concurrent Requests

```bash
# Using Apache Bench
ab -n 100 -c 10 \
  -H "Authorization: Bearer dummy-token" \
  -H "Content-Type: application/json" \
  -p test-payload.json \
  http://localhost:8084/api/v1/accounts/fd-calculation

# test-payload.json:
# {
#   "customerId": 1,
#   "productCode": "FD-001",
#   "principalAmount": 100000,
#   "tenureMonths": 12
# }
```

### Monitor Performance

```bash
# Watch consumer lag
watch -n 1 'docker-compose -f docker-compose-kafka.yml exec kafka \
  kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group accounts-service \
  --describe'
```

## Step 10: Cleanup

### Stop All Services

```bash
# Stop microservices (use Ctrl+C in each terminal)

# Stop Docker containers
docker-compose -f docker-compose-kafka.yml down

# Remove volumes (if needed)
docker-compose -f docker-compose-kafka.yml down -v
```

## Success Criteria

✅ All services start without errors
✅ Kafka topics are created automatically
✅ FD calculation requests succeed
✅ Messages appear in Kafka topics
✅ Response times are <1 second
✅ No timeout errors in logs
✅ Consumer groups show no lag

## Common Tests

### Test 1: Basic FD Calculation

- Send valid calculation request
- Verify success response
- Check Kafka messages

### Test 2: Invalid Customer

- Send request with non-existent customer ID
- Verify error response
- Check error message in Kafka response

### Test 3: Product Validation

- Send request with valid product code
- Verify product details in response
- Test with invalid product code

### Test 4: Multiple Concurrent Requests

- Send 10+ concurrent requests
- Verify all succeed within timeout
- Monitor Kafka consumer lag

### Test 5: Consumer Group Recovery

- Kill one consumer
- Verify rebalancing
- Kill another and verify it recovers
