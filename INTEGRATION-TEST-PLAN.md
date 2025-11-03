# Integration Test Script - Banking Microservices Ecosystem

# Run this after all 4 microservices are started

## Test Scenario: Complete FD Account Opening Flow

### Phase 1: User Authentication (Customer Service)

#### Step 1.1: Register a Bank Officer

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "officer1",
    "password": "Officer@123",
    "email": "officer1@banktest.com",
    "fullName": "John Officer",
    "role": "BANKOFFICER"
  }'
```

**Expected:** 201 Created, returns user with BANKOFFICER role

#### Step 1.2: Register a Customer

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "customer1",
    "password": "Customer@123",
    "email": "customer1@test.com",
    "fullName": "Alice Customer",
    "role": "CUSTOMER"
  }'
```

**Expected:** 201 Created, returns user with CUSTOMER role

#### Step 1.3: Login as Bank Officer (Get JWT Token)

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "officer1",
    "password": "Officer@123"
  }'
```

**Expected:** 200 OK, returns JWT token
**Action:** Copy the JWT token for subsequent requests

#### Step 1.4: Login as Customer (Get JWT Token)

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "customer1",
    "password": "Customer@123"
  }'
```

**Expected:** 200 OK, returns JWT token
**Action:** Copy the customer JWT token

---

### Phase 2: Customer Profile Creation

#### Step 2.1: Get Customer Profile (Using Customer Token)

```bash
curl -X GET http://localhost:8081/api/customers/me \
  -H "Authorization: Bearer {CUSTOMER_JWT_TOKEN}"
```

**Expected:** 200 OK, returns customer profile with generated customer_id
**Action:** Note the customer_id for FD account creation

#### Step 2.2: Update Customer Profile

```bash
curl -X PUT http://localhost:8081/api/customers/me \
  -H "Authorization: Bearer {CUSTOMER_JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice.updated@test.com",
    "fullName": "Alice M. Customer",
    "phoneNumber": "+1234567890",
    "address": "123 Main Street, City, State"
  }'
```

**Expected:** 200 OK, returns updated profile

---

### Phase 3: Product Management (Product-Pricing Service)

#### Step 3.1: Create FD Products (Requires ADMIN/BANKOFFICER role)

```bash
# Create Premium FD Product
curl -X POST http://localhost:8082/api/products \
  -H "Authorization: Bearer {OFFICER_JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "Premium Fixed Deposit",
    "productCode": "FD-PREMIUM",
    "productType": "PREMIUM",
    "baseInterestRate": 6.75,
    "minTenureMonths": 12,
    "maxTenureMonths": 60,
    "minAmount": 100000,
    "maxAmount": 10000000,
    "description": "High-value FD with premium interest rates",
    "isActive": true
  }'
```

**Expected:** 201 Created, returns product details

```bash
# Create Senior Citizen FD Product
curl -X POST http://localhost:8082/api/products \
  -H "Authorization: Bearer {OFFICER_JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "Senior Citizen FD",
    "productCode": "FD-SENIOR",
    "productType": "SENIOR",
    "baseInterestRate": 7.25,
    "minTenureMonths": 12,
    "maxTenureMonths": 120,
    "minAmount": 50000,
    "maxAmount": 5000000,
    "description": "Special FD for senior citizens with higher rates",
    "isActive": true
  }'
```

**Expected:** 201 Created, returns product details

#### Step 3.2: Get All Products (Any authenticated user)

```bash
curl -X GET http://localhost:8082/api/products \
  -H "Authorization: Bearer {CUSTOMER_JWT_TOKEN}"
```

**Expected:** 200 OK, returns list of all active products

#### Step 3.3: Get Product by Code

```bash
curl -X GET http://localhost:8082/api/products/code/FD-PREMIUM \
  -H "Authorization: Bearer {CUSTOMER_JWT_TOKEN}"
```

**Expected:** 200 OK, returns FD-PREMIUM product details

---

### Phase 4: FD Calculation (FD-Calculator Service)

#### Step 4.1: Calculate FD Maturity - Simple Interest

```bash
curl -X POST http://localhost:8083/api/calculator/calculate \
  -H "Authorization: Bearer {CUSTOMER_JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "{CUSTOMER_ID_FROM_STEP_2.1}",
    "productCode": "FD-PREMIUM",
    "principalAmount": 100000,
    "interestRate": 6.75,
    "tenureMonths": 24,
    "calculationMethod": "SIMPLE"
  }'
```

**Expected:** 200 OK, returns maturity amount with simple interest calculation
**Action:** Note the calculation ID and maturity amount

#### Step 4.2: Calculate FD Maturity - Compound Interest (Quarterly)

```bash
curl -X POST http://localhost:8083/api/calculator/calculate \
  -H "Authorization: Bearer {CUSTOMER_JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "{CUSTOMER_ID}",
    "productCode": "FD-PREMIUM",
    "principalAmount": 100000,
    "interestRate": 6.75,
    "tenureMonths": 24,
    "calculationMethod": "COMPOUND_QUARTERLY"
  }'
```

**Expected:** 200 OK, returns higher maturity amount with compound interest

#### Step 4.3: Get Calculation by ID

```bash
curl -X GET http://localhost:8083/api/calculator/calculation/{CALCULATION_ID} \
  -H "Authorization: Bearer {CUSTOMER_JWT_TOKEN}"
```

**Expected:** 200 OK, returns stored calculation details

#### Step 4.4: Test Senior Citizen FD Calculation

```bash
curl -X POST http://localhost:8083/api/calculator/calculate \
  -H "Authorization: Bearer {CUSTOMER_JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "{CUSTOMER_ID}",
    "productCode": "FD-SENIOR",
    "principalAmount": 200000,
    "interestRate": 7.25,
    "tenureMonths": 36,
    "calculationMethod": "COMPOUND_QUARTERLY"
  }'
```

**Expected:** 200 OK, returns maturity amount for senior citizen FD

---

### Phase 5: FD Account Creation (Accounts Service)

#### Step 5.1: Create FD Account (BANKOFFICER only)

```bash
curl -X POST http://localhost:8084/api/accounts/create \
  -H "Authorization: Bearer {OFFICER_JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "{CUSTOMER_ID}",
    "productCode": "FD-PREMIUM",
    "principalAmount": 100000,
    "interestRate": 6.75,
    "tenureMonths": 24,
    "branchCode": "BR001",
    "remarks": "FD account opened via API"
  }'
```

**Expected:** 201 Created, returns FD account with:

- Auto-generated account number (e.g., FD-BR001-20251023-10000001)
- Maturity amount calculated via FD Calculator service
- Maturity date calculated
- Status: ACTIVE
  **Action:** Note the account number

#### Step 5.2: Verify Customer Validation (Try invalid customer)

```bash
curl -X POST http://localhost:8084/api/accounts/create \
  -H "Authorization: Bearer {OFFICER_JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "INVALID_CUSTOMER",
    "productCode": "FD-PREMIUM",
    "principalAmount": 100000,
    "interestRate": 6.75,
    "tenureMonths": 24,
    "branchCode": "BR001"
  }'
```

**Expected:** 404 Not Found, "Customer not found" error

#### Step 5.3: Verify Product Validation (Try invalid product)

```bash
curl -X POST http://localhost:8084/api/accounts/create \
  -H "Authorization: Bearer {OFFICER_JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "{CUSTOMER_ID}",
    "productCode": "INVALID_PRODUCT",
    "principalAmount": 100000,
    "interestRate": 6.75,
    "tenureMonths": 24,
    "branchCode": "BR001"
  }'
```

**Expected:** 404 Not Found, "Product not found" error

#### Step 5.4: Verify Amount Validation (Below minimum)

```bash
curl -X POST http://localhost:8084/api/accounts/create \
  -H "Authorization: Bearer {OFFICER_JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "{CUSTOMER_ID}",
    "productCode": "FD-PREMIUM",
    "principalAmount": 50000,
    "interestRate": 6.75,
    "tenureMonths": 24,
    "branchCode": "BR001"
  }'
```

**Expected:** 400 Bad Request, "Principal amount below minimum" error

#### Step 5.5: Verify Authorization (Customer cannot create account)

```bash
curl -X POST http://localhost:8084/api/accounts/create \
  -H "Authorization: Bearer {CUSTOMER_JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "{CUSTOMER_ID}",
    "productCode": "FD-PREMIUM",
    "principalAmount": 100000,
    "interestRate": 6.75,
    "tenureMonths": 24,
    "branchCode": "BR001"
  }'
```

**Expected:** 403 Forbidden, access denied

---

### Phase 6: Account Operations

#### Step 6.1: Get Account Details (Any authenticated user)

```bash
curl -X GET http://localhost:8084/api/accounts/{ACCOUNT_NUMBER} \
  -H "Authorization: Bearer {CUSTOMER_JWT_TOKEN}"
```

**Expected:** 200 OK, returns complete account details

#### Step 6.2: Get Customer's All Accounts

```bash
curl -X GET http://localhost:8084/api/accounts/customer/{CUSTOMER_ID} \
  -H "Authorization: Bearer {CUSTOMER_JWT_TOKEN}"
```

**Expected:** 200 OK, returns list of all accounts for the customer

#### Step 6.3: Record a Transaction

```bash
curl -X POST http://localhost:8084/api/accounts/{ACCOUNT_NUMBER}/transactions \
  -H "Authorization: Bearer {OFFICER_JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionType": "DEPOSIT",
    "amount": 10000,
    "description": "Additional deposit to FD account",
    "referenceNo": "REF-20251023-001"
  }'
```

**Expected:** 201 Created, returns transaction details with auto-generated transaction ID

#### Step 6.4: Get Account Transaction History

```bash
curl -X GET http://localhost:8084/api/accounts/{ACCOUNT_NUMBER}/transactions \
  -H "Authorization: Bearer {CUSTOMER_JWT_TOKEN}"
```

**Expected:** 200 OK, returns list of all transactions for the account

#### Step 6.5: Close FD Account (BANKOFFICER only)

```bash
curl -X PUT http://localhost:8084/api/accounts/{ACCOUNT_NUMBER}/close \
  -H "Authorization: Bearer {OFFICER_JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "closureReason": "Customer requested premature closure"
  }'
```

**Expected:** 200 OK, account status changed to CLOSED

---

### Phase 7: Health Checks & Service Status

#### Step 7.1: Customer Service Health

```bash
curl -X GET http://localhost:8081/actuator/health
```

**Expected:** 200 OK, {"status":"UP"}

#### Step 7.2: Product Service Health

```bash
curl -X GET http://localhost:8082/actuator/health
```

**Expected:** 200 OK, {"status":"UP"}

#### Step 7.3: FD-Calculator Service Health

```bash
curl -X GET http://localhost:8083/actuator/health
```

**Expected:** 200 OK, {"status":"UP"}

#### Step 7.4: Accounts Service Health (Custom)

```bash
curl -X GET http://localhost:8084/api/accounts/status
```

**Expected:** 200 OK, returns service health with database and Feign client connectivity status

---

## Test Scenarios Summary

### ✅ Authentication & Authorization

- User registration (CUSTOMER, BANKOFFICER roles)
- JWT token generation and validation
- Role-based access control enforcement
- Token expiration handling

### ✅ Customer Management

- Customer profile creation
- Profile retrieval and updates
- Customer validation in downstream services

### ✅ Product Management

- Product creation (ADMIN/BANKOFFICER only)
- Product listing (all authenticated users)
- Product retrieval by code
- Product validation in FD calculator and account services

### ✅ FD Calculations

- Simple interest calculation
- Compound interest calculation (multiple frequencies)
- Customer and product validation via Feign clients
- Calculation history persistence

### ✅ FD Account Lifecycle

- Account creation with multi-service validation
- Account number auto-generation
- Maturity amount calculation via FD Calculator
- Account retrieval
- Transaction recording
- Transaction history
- Account closure

### ✅ Inter-Service Communication

- Customer service → Accounts service (customer validation)
- Product service → FD Calculator service (product validation)
- Product service → Accounts service (product validation)
- FD Calculator service → Accounts service (maturity calculation)
- All Feign client interactions

### ✅ Error Handling

- Invalid credentials
- Unauthorized access attempts
- Invalid input validation
- Resource not found errors
- Business rule violations

---

## Integration Test Metrics

### Expected Pass Rate

- **Authentication:** 100% (4/4 tests)
- **Customer Management:** 100% (3/3 tests)
- **Product Management:** 100% (4/4 tests)
- **FD Calculations:** 100% (4/4 tests)
- **Account Operations:** 100% (10/10 tests)
- **Health Checks:** 100% (4/4 tests)

**Total:** 29 integration tests

---

## Troubleshooting Guide

### If a service fails to start:

1. Check MySQL is running: `mysql -u root -p`
2. Verify database exists: `SHOW DATABASES;`
3. Check port availability: `netstat -ano | findstr "808X"`
4. Review application logs in the terminal

### If JWT authentication fails:

1. Verify JWT secret matches across all services
2. Check token expiration (24 hours)
3. Ensure "Bearer " prefix in Authorization header

### If Feign client fails:

1. Verify target service is running
2. Check service URLs in application.yml
3. Review Feign client configuration
4. Check network connectivity between services

---

## Success Criteria

✅ All 4 services start without errors  
✅ JWT authentication works across all services  
✅ User registration and login successful  
✅ Customer profile operations work  
✅ Products can be created and retrieved  
✅ FD calculations work with different methods  
✅ FD accounts can be created with validations  
✅ Transactions can be recorded  
✅ Account closure works with authorization  
✅ All Feign clients communicate successfully  
✅ Health checks return positive status  
✅ No database connection errors  
✅ Authorization rules enforced correctly

---

**Ready to Execute:** Once you confirm all 4 services are running, I'll execute these tests systematically and report results.
