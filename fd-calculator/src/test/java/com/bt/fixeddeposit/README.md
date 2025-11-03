# FD Calculator Module - Test Suite Documentation

## Overview

Comprehensive test suite for the Fixed Deposit Calculator microservice covering unit tests, integration tests, and API endpoint validation with security testing.

## Test Structure

### Service Layer Tests (`FdCalculationServiceTest.java`)

**Purpose**: Validate business logic for FD calculations, external service integration, and calculation algorithms.

**Test Coverage**:

1. **testCalculateFd_Success**

   - Validates successful FD calculation with valid inputs
   - Verifies customer validation via Customer service
   - Verifies product fetching via Product service
   - Confirms calculation persistence in database
   - Validates maturity amount calculation accuracy
   - **Expected**: HTTP 201 with calculated FD response

2. **testCalculateFd_CustomerNotFound**

   - Tests behavior when Customer service fails or customer doesn't exist
   - Validates proper exception handling for external service failures
   - **Expected**: ServiceIntegrationException thrown

3. **testCalculateFd_ProductNotFound**

   - Tests behavior when Product service fails or product doesn't exist
   - Validates proper error propagation from external services
   - **Expected**: ServiceIntegrationException thrown

4. **testCalculateFd_InactiveCustomer**

   - Validates rejection of calculations for inactive customers
   - Tests business rule enforcement for customer status
   - **Expected**: InvalidCalculationDataException thrown

5. **testCalculateFd_InactiveProduct**

   - Validates rejection of calculations for inactive products
   - Tests business rule enforcement for product status
   - **Expected**: InvalidCalculationDataException thrown

6. **testCalculateFd_InvalidPrincipalAmount**

   - Tests validation of principal amount against product constraints
   - Validates min/max amount boundary checks
   - **Expected**: InvalidCalculationDataException thrown

7. **testCalculateFd_InvalidTenure**

   - Tests validation of tenure against product term limits
   - Validates min/max tenure boundary checks
   - **Expected**: InvalidCalculationDataException thrown

8. **testGetCalculationById_Success**

   - Tests retrieval of existing calculation by ID
   - Validates product name enrichment from Product service
   - **Expected**: Valid FdCalculationResponse returned

9. **testGetCalculationById_NotFound**

   - Tests behavior when calculation ID doesn't exist
   - **Expected**: CalculationNotFoundException thrown

10. **testGetCalculationHistory_Success**

    - Tests retrieval of all calculations for a customer
    - Validates customer authentication check
    - Verifies chronological ordering (newest first)
    - **Expected**: List of FdCalculationResponse objects

11. **testGetRecentCalculations_Success**
    - Tests retrieval of calculations within date range
    - Validates date filtering logic
    - **Expected**: Filtered list of calculations

### Controller Layer Tests (`FdCalculatorControllerTest.java`)

**Purpose**: Validate REST API endpoints, request/response handling, security, and error responses.

**Test Coverage**:

1. **testCalculateFd_Success**

   - Tests POST /api/fd/calculate endpoint
   - Validates JWT authentication requirement
   - Tests request body validation and mapping
   - **Expected**: HTTP 201, success=true, calculation data in response

2. **testCalculateFd_InvalidRequest**

   - Tests validation annotations on request DTO
   - Validates @NotNull, @Pattern, @Min, @Max constraints
   - **Expected**: HTTP 400, validation error messages

3. **testCalculateFd_CustomerNotFound**

   - Tests error response for non-existent customer
   - Validates error message formatting
   - **Expected**: HTTP 404, "Customer Not Found" error

4. **testCalculateFd_InvalidCalculationData**

   - Tests error response for invalid calculation parameters
   - **Expected**: HTTP 400, "Invalid Calculation Data" error

5. **testGetCalculationById_Success**

   - Tests GET /api/fd/calculations/{id} endpoint
   - Validates path variable binding
   - **Expected**: HTTP 200, calculation details

6. **testGetCalculationById_NotFound**

   - Tests error response for non-existent calculation
   - **Expected**: HTTP 404, "Calculation Not Found" error

7. **testGetCalculationHistory_Success**

   - Tests GET /api/fd/history/{customerId} endpoint
   - Validates customer-specific filtering
   - **Expected**: HTTP 200, array of calculations

8. **testGetRecentCalculations_Success**

   - Tests GET /api/fd/recent/{customerId} with query params
   - Validates default value for 'days' parameter
   - **Expected**: HTTP 200, filtered calculations

9. **testCalculateFd_Unauthorized**

   - Tests endpoint access without authentication
   - Validates JWT filter enforcement
   - **Expected**: HTTP 403 Forbidden

10. **testGetCalculationHistory_AdminAccess**
    - Tests role-based access control
    - Validates ADMIN role can access customer data
    - **Expected**: HTTP 200 with authorization

### Repository Layer Tests (`FdCalculationRepositoryTest.java`)

**Purpose**: Validate database operations, custom queries, and JPA entity mappings.

**Test Coverage**:

1. **testSaveAndFindById**

   - Tests basic CRUD save and findById operations
   - Validates entity persistence and retrieval
   - Verifies @PrePersist hook execution
   - **Expected**: Saved entity retrieved with same data

2. **testFindByCustomerIdOrderByCreatedAtDesc**

   - Tests custom query method for customer filtering
   - Validates descending order by creation date
   - **Expected**: List ordered by createdAt DESC

3. **testFindByCustomerIdAndProductCodeOrderByCreatedAtDesc**

   - Tests compound filtering by customer and product
   - Validates multiple where clauses
   - **Expected**: Filtered and ordered results

4. **testFindRecentCalculationsByCustomer**

   - Tests @Query annotation with date filtering
   - Validates LocalDateTime parameter binding
   - **Expected**: Results within date range

5. **testFindCalculationsByProductAndDate**

   - Tests product-based filtering with date range
   - **Expected**: Product-specific calculations after date

6. **testCountByCustomerId**

   - Tests aggregate query for counting calculations
   - **Expected**: Correct count of customer calculations

7. **testCountByCustomerId_NoResults**
   - Tests count query with no matching records
   - **Expected**: Count of 0 (not null)

## Calculation Algorithm Validation

### Compound Interest Formula

```
A = P * (1 + r/n)^(n*t)
Where:
- A = Maturity Amount
- P = Principal Amount
- r = Annual Interest Rate (decimal)
- n = Compounding Frequency (quarterly = 4)
- t = Time in years (tenureMonths / 12)
```

### Effective Rate Formula

```
Effective Rate = [(1 + r/n)^n - 1] * 100
```

### Test Scenarios

- **Principal**: 100,000
- **Rate**: 6.5% p.a.
- **Tenure**: 12 months
- **Compounding**: Quarterly (4)
- **Expected Maturity**: ~106,659.46
- **Expected Interest**: ~6,659.46
- **Effective Rate**: ~6.66%

## Integration Testing

### External Service Integration

1. **Customer Service Integration**

   - Validates JWT token in Authorization header
   - Tests customer existence check
   - Tests customer active status validation
   - Handles Feign exceptions gracefully

2. **Product Service Integration**
   - Fetches product details by code
   - Validates product active status
   - Retrieves interest rates and term limits
   - Handles service unavailability

### Security Testing

- JWT token validation on all endpoints
- Role-based authorization (CUSTOMER, BANKOFFICER, ADMIN)
- Unauthorized access rejection (HTTP 403)
- Bearer token format validation

## Running Tests

### Run All Tests

```bash
.\mvnw.cmd clean test
```

### Run Specific Test Class

```bash
.\mvnw.cmd test -Dtest=FdCalculationServiceTest
```

### Run Single Test Method

```bash
.\mvnw.cmd test -Dtest=FdCalculationServiceTest#testCalculateFd_Success
```

### Generate Test Coverage Report

```bash
.\mvnw.cmd clean verify
```

## Test Dependencies

- JUnit 5 (Jupiter)
- Mockito for mocking
- Spring Boot Test
- Spring Security Test
- MockMvc for controller testing
- @DataJpaTest for repository testing
- H2 in-memory database for tests

## Mocking Strategy

- **Service Layer**: Mock repositories and Feign clients
- **Controller Layer**: Mock service layer, use real security config
- **Repository Layer**: Use @DataJpaTest with real H2 database

## Expected Test Results

- **Total Test Cases**: 28
- **Service Tests**: 11
- **Controller Tests**: 10
- **Repository Tests**: 7
- **Expected Pass Rate**: 100%
- **Code Coverage Target**: >80%

## Test Data Strategy

- **Customer IDs**: 1L (valid), 999L (non-existent)
- **Product Codes**: "FD-001" (valid), "FD-002" (valid), "DUMMY" (invalid)
- **Principal Amounts**: 100,000 (valid), 5,000 (too low), 200,000 (valid)
- **Tenures**: 12 months (valid), 3 months (too short), 24 months (valid)
- **Auth Tokens**: "Bearer valid-token", "Bearer admin-token"

## Error Scenarios Covered

1. Customer not found
2. Product not found
3. Inactive customer
4. Inactive product
5. Invalid principal amount (too low/high)
6. Invalid tenure (too short/long)
7. Missing required fields
8. Invalid field formats
9. Unauthorized access
10. Service integration failures

## Assertions Validated

- Response status codes
- Success/error flags
- Data field values
- Calculation accuracy
- Exception types
- Error messages
- Security enforcement
- Database persistence
- Query result ordering
- Null safety

## Notes

- All tests use in-memory H2 database to avoid external dependencies
- Feign clients are mocked to avoid calling real external services
- JWT tokens are mocked in security tests
- Tests are isolated and can run in any order
- Each test has setup/teardown for clean state
