# Kafka Migration Status Report

## Summary

CashCached microservices are being migrated from Feign clients/HTTP APIs to Kafka-based asynchronous messaging.

## Completed Tasks

### 1. ✅ Dependencies Added (Task 1)

- Added `spring-kafka` dependency to all 6 microservices
- Updated pom.xml files:
  - accounts/pom.xml
  - customer/pom.xml
  - fd-calculator/pom.xml
  - product-pricing/pom.xml
  - (main, blockchain - not microservices)

### 2. ✅ Kafka Event DTOs Created (Task 2)

Created event classes for all services:

- **Customer Service Events:**
  - CustomerValidationRequest/Response
- **Product Service Events:**
  - ProductDetailsRequest/Response
- **FD Calculator Events:**
  - FdCalculationRequestEvent/FdCalculationResponseEvent
  - FdHistoryRequest/Response
- **Kafka Topics Configuration:**
  - KafkaTopics.java in each service package
  - RequestResponseStore for correlation store pattern

### 3. ✅ Kafka Configuration (Task 2 cont'd)

- Created KafkaConfig.java in each service:
  - Defined 8 Kafka topics with 3 partitions and 1 replica
  - Configured JSON serialization/deserialization
  - Set up producer and consumer factories
  - Configured consumer group IDs per service

### 4. ✅ Kafka Producers Created (Task 3)

- **FD-Calculator Producer:**
  - Sends CustomerValidationResponse
  - Sends ProductDetailsResponse
- **Product-Pricing Producer:**
  - Sends CustomerValidationResponse
  - Sends ProductDetailsResponse
- **Accounts Producer:**
  - Sends CustomerValidationRequest
  - Sends ProductDetailsRequest
  - Sends FdCalculationRequestEvent

### 5. ✅ Kafka Consumers Created (Task 4)

- **FD-Calculator Consumer:**
  - Listens on: customer.validation.request
  - Listens on: product.details.request
  - Still using Feign clients internally (bridge to legacy endpoints)
  - Sends responses back to response topics

### 6. ⏳ FD-Calculator Migration (Task 5)

**PARTIALLY COMPLETED:**

- ✅ Replaced FdCalculationService to use Kafka for customer/product validation
- ✅ Implemented request-response correlation pattern
- ✅ Removed EnableFeignClients from FdCalculatorApplication
- ✅ Updated application.yml with Kafka config
- ⏳ Still uses Feign clients in KafkaConsumerService for bridge (can be removed after Product service is updated)

### 7. ✅ Application Properties Updated (Task 9)

Updated all service application.yml files:

- fd-calculator/src/main/resources/application.yml
- product-pricing/src/main/resources/application.yml
- customer/src/main/resources/application.yml
- accounts/src/main/resources/application.yml

Added Kafka bootstrap servers and consumer group configurations.
Removed old Feign configuration properties.
Removed service.\*.url properties.

## Remaining Tasks

### Product-Pricing Migration (Task 6)

- Create KafkaProducerService consumer in Product service
- Update ProductController to listen to product.details.request events
- Create producer to send ProductDetailsResponse
- Refactor service logic to use Kafka instead of Feign clients
- Remove ProductServiceClient and CustomerServiceClient Feign interfaces
- Remove @EnableFeignClients from ProductApplication

### Accounts Migration (Task 7)

- Implement KafkaConsumerService to listen to responses from other services
- Create correlation store in Accounts service
- Refactor all services (AccountService, TransactionService, PricingRuleEvaluator, etc.)
- Replace all Feign client calls with Kafka requests
- Remove all Feign client interfaces from Accounts module
- Remove @EnableFeignClients from AccountsApplication

### Customer & Blockchain Microservices (Task 8)

- Customer: Add Kafka producers for customer events
- Blockchain: Add Kafka consumers for transaction events

### Cleanup - Remove Feign Dependencies (Task 10)

- Remove spring-cloud-starter-openfeign from remaining pom.xml files:
  - product-pricing/pom.xml (if not already done)
  - accounts/pom.xml (if not already done)
  - customer/pom.xml (if not already done)
- Remove all @FeignClient interfaces
- Remove all FeignException handlers

## Communication Flows

### Current (Feign-based):

```
Accounts Service
    ├─> Feign: GET /api/v1/customers/{id}  → Customer Service
    ├─> Feign: GET /api/v1/product/{code}  → Product Service
    └─> Feign: POST /api/fd/calculate      → FD-Calculator Service
```

### New (Kafka-based):

```
Accounts Service
    ├─> Kafka: customer.validation.request → Customer Service (via FD-Calculator consumer)
    │   ↓
    │   Kafka: customer.validation.response ← Customer Service
    │
    ├─> Kafka: product.details.request → Product Service
    │   ↓
    │   Kafka: product.details.response ← Product Service
    │
    └─> Kafka: fd.calculation.request → FD-Calculator Service
        ↓
        Kafka: fd.calculation.response ← FD-Calculator Service
```

## Architecture Benefits

1. **Decoupling**: Services don't need direct knowledge of each other's endpoints
2. **Asynchronous**: Non-blocking request-response patterns
3. **Scalability**: Can add/remove service instances without configuration changes
4. **Event-Driven**: Foundation for future event-based features
5. **Resilience**: Queue provides message persistence and retry capability
6. **Load Distribution**: Kafka distributes messages across partitions

## Testing Instructions

1. Start Kafka broker:

   ```bash
   docker-compose up kafka zookeeper
   ```

2. Start services:

   ```bash
   ./start-all-services.bat
   ```

3. Test FD calculation via Accounts:

   ```bash
   curl -X POST http://localhost:8084/api/accounts/fd-calculation \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer <token>" \
     -d '{
       "customerId": 1,
       "productCode": "FD-001",
       "principalAmount": 10000,
       "tenureMonths": 12
     }'
   ```

4. Monitor Kafka topics:
   ```bash
   kafka-console-consumer.sh --bootstrap-server localhost:9092 \
     --topic customer.validation.request --from-beginning
   ```

## Configuration Reference

### Kafka Bootstrap Servers

Default: `localhost:9092`
Override: `KAFKA_BOOTSTRAP_SERVERS` environment variable

### Request Timeout

Default: `30 seconds`
Override: `KAFKA_REQUEST_TIMEOUT` environment variable

### Topics Created

- customer.validation.request (3 partitions)
- customer.validation.response (3 partitions)
- product.details.request (3 partitions)
- product.details.response (3 partitions)
- fd.calculation.request (3 partitions)
- fd.calculation.response (3 partitions)
- fd.history.request (3 partitions)
- fd.history.response (3 partitions)
- account.created (3 partitions)
- account.updated (3 partitions)
- customer.created (3 partitions)
- customer.updated (3 partitions)

## Next Steps

1. Complete Product-Pricing service migration
2. Complete Accounts service migration
3. Remove remaining Feign dependencies
4. Test end-to-end workflows
5. Deploy to production
6. Monitor and optimize Kafka performance
