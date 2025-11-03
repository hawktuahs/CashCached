# Accounts Module - Test Suite Documentation

## Overview

Comprehensive test suite for the Accounts microservice covering account creation, management, transactions, and inter-service integration.

## Test Structure

### 1. Service Layer Tests (`AccountServiceTest.java`)

**Purpose:** Test business logic, validation, and service integration

**Test Cases (11 tests):**

1. **createAccount_WithValidData_ShouldSucceed**

   - Tests successful account creation with valid inputs
   - Validates customer and product through Feign clients
   - Calculates maturity using FD Calculator service
   - Generates unique account number
   - Expected: Account created with status ACTIVE

2. **createAccount_WithInvalidCustomer_ShouldThrowException**

   - Tests customer validation failure
   - Expected: CustomerNotFoundException

3. **createAccount_WithPrincipalBelowMinimum_ShouldThrowException**

   - Tests product rule validation for minimum principal
   - Input: Principal = 5000, Product minimum = 10000
   - Expected: InvalidAccountDataException

4. **createAccount_WithTenureAboveMaximum_ShouldThrowException**

   - Tests tenure validation against product limits
   - Input: Tenure = 150 months, Product maximum = 120
   - Expected: InvalidAccountDataException

5. **getAccount_WithValidAccountNo_ShouldReturnAccount**

   - Tests account retrieval by account number
   - Expected: Account details returned

6. **getAccount_WithInvalidAccountNo_ShouldThrowException**

   - Tests account not found scenario
   - Expected: AccountNotFoundException

7. **closeAccount_WithActiveAccount_ShouldSucceed**

   - Tests account closure with valid reason
   - Updates status to CLOSED, records closure timestamp
   - Expected: Account status changed to CLOSED

8. **closeAccount_WithAlreadyClosedAccount_ShouldThrowException**

   - Tests duplicate closure prevention
   - Expected: AccountAlreadyClosedException

9. **getCustomerAccounts_WithValidCustomerId_ShouldReturnAccounts**

   - Tests retrieval of all accounts for a customer
   - Expected: List of customer accounts

10. **Authorization Tests**
    - Tests role-based access control
    - Only BANKOFFICER and ADMIN can create/close accounts
    - Expected: UnauthorizedAccessException for unauthorized roles

### 2. Controller Layer Tests (`AccountControllerTest.java`)

**Purpose:** Test REST API endpoints, request validation, and HTTP responses

**Test Cases (6 tests):**

1. **createAccount_WithValidRequest_ShouldReturn201**

   - POST /api/accounts/create
   - Valid account creation request
   - Expected: HTTP 201 CREATED, account details in response

2. **createAccount_WithInvalidPrincipal_ShouldReturn400**

   - Tests input validation
   - Principal amount below minimum (500 vs 1000)
   - Expected: HTTP 400 BAD REQUEST with validation errors

3. **getAccount_WithValidAccountNo_ShouldReturn200**

   - GET /api/accounts/{accountNo}
   - Expected: HTTP 200 OK with account details

4. **getCustomerAccounts_WithValidCustomerId_ShouldReturn200**

   - GET /api/accounts/customer/{customerId}
   - Expected: HTTP 200 OK with array of accounts

5. **closeAccount_WithValidRequest_ShouldReturn200**

   - PUT /api/accounts/{accountNo}/close
   - Valid closure reason provided
   - Expected: HTTP 200 OK, status changed to CLOSED

6. **createAccount_WithoutBankOfficerRole_ShouldReturn403**
   - Tests security authorization
   - User without BANKOFFICER/ADMIN role
   - Expected: HTTP 403 FORBIDDEN

### 3. Repository Layer Tests (`FdAccountRepositoryTest.java`)

**Purpose:** Test database operations and custom queries

**Test Cases (7 tests):**

1. **findByAccountNo_WithExistingAccount_ShouldReturnAccount**

   - Tests account lookup by account number
   - Expected: Account found with correct details

2. **findByAccountNo_WithNonExistingAccount_ShouldReturnEmpty**

   - Tests account not found scenario
   - Expected: Empty Optional

3. **findByCustomerId_WithExistingCustomer_ShouldReturnAccounts**

   - Tests customer account list retrieval
   - Expected: List of accounts for customer

4. **findByStatus_WithActiveStatus_ShouldReturnActiveAccounts**

   - Tests filtering accounts by status
   - Expected: Only ACTIVE accounts returned

5. **existsByAccountNo_WithExistingAccount_ShouldReturnTrue**

   - Tests account existence check
   - Expected: true for existing account

6. **existsByAccountNo_WithNonExistingAccount_ShouldReturnFalse**

   - Tests non-existent account check
   - Expected: false

7. **countByStatus_WithActiveStatus_ShouldReturnCount**
   - Tests counting accounts by status
   - Expected: Accurate count of ACTIVE accounts

## Test Configuration

### Database

- **Test Database:** H2 in-memory database
- **Auto-configured:** Spring Boot Test automatically configures H2
- **Isolation:** Each test runs in a transaction and rolls back

### Security

- **Mock Authentication:** `@WithMockUser` annotation
- **Roles Tested:** BANKOFFICER, ADMIN, USER
- **JWT:** Mocked in controller tests

### Dependencies Mocked

- **CustomerServiceClient:** Customer validation
- **ProductServiceClient:** Product details and rules
- **FdCalculatorServiceClient:** Maturity calculation

## Running Tests

### Run All Tests

```bash
.\mvnw.cmd test
```

### Run Specific Test Class

```bash
.\mvnw.cmd test -Dtest=AccountServiceTest
```

### Run with Coverage

```bash
.\mvnw.cmd test jacoco:report
```

## Test Data

### Sample Account

- **Account Number:** FD-BR001-20251023-10000001
- **Customer ID:** CUST001
- **Product Code:** FD-PREMIUM
- **Principal:** 100,000.00
- **Interest Rate:** 6.75%
- **Tenure:** 24 months
- **Branch:** BR001

### Product Rules

- **Min Amount:** 10,000
- **Max Amount:** 10,000,000
- **Min Tenure:** 6 months
- **Max Tenure:** 120 months
- **Interest Rate Range:** 5.00% - 8.00%

## Expected Results

### Successful Account Creation

```json
{
  "success": true,
  "message": "Account created successfully",
  "data": {
    "accountNo": "FD-BR001-20251023-10000001",
    "customerId": "CUST001",
    "productCode": "FD-PREMIUM",
    "principalAmount": 100000.0,
    "interestRate": 6.75,
    "tenureMonths": 24,
    "status": "ACTIVE",
    "maturityAmount": 114061.37
  }
}
```

### Validation Error

```json
{
  "timestamp": "2025-10-23T16:30:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed",
  "fieldErrors": {
    "principalAmount": "Minimum principal amount is 1000"
  }
}
```

## Coverage Goals

- **Service Layer:** > 85%
- **Controller Layer:** > 80%
- **Repository Layer:** > 90%
- **Overall:** > 80%

## Integration Testing Notes

- Tests verify inter-service communication via Feign clients
- Mocked external service responses simulate real scenarios
- Security tests validate JWT-based authorization
- Transaction tests ensure data consistency

---

**Total Test Cases:** 24 tests across 3 test classes
**Test Framework:** JUnit 5 with Mockito
**Build Status:** All tests should pass with BUILD SUCCESS
