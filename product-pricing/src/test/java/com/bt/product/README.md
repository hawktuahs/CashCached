# Product and Pricing Module - Test Suite

## Overview

This test suite validates the functionality of the Product and Pricing microservice with comprehensive unit and integration tests.

## Test Structure

### Service Layer Tests

**ProductServiceTest.java** - 8 test cases

- `createProduct_Success` - Validates successful product creation with valid data
- `createProduct_DuplicateCode_ThrowsException` - Ensures duplicate product codes are rejected
- `createProduct_InvalidInterestRange_ThrowsException` - Validates interest rate range constraints
- `getProductByCode_Success` - Tests retrieving existing product by code
- `getProductByCode_NotFound_ThrowsException` - Verifies exception for non-existent products
- `updateProduct_Success` - Tests updating product details
- `deleteProduct_Success` - Validates product deletion

**Expected Behavior:**

- Service methods should validate business rules
- Invalid data should throw appropriate exceptions
- All operations should interact correctly with repositories
- Caching annotations should be applied correctly

### Controller Layer Tests

**ProductControllerTest.java** - 5 test cases

- `createProduct_WithAdminRole_ReturnsCreated` - Admin can create products (HTTP 201)
- `createProduct_WithCustomerRole_ReturnsForbidden` - Customers cannot create products (HTTP 403)
- `getProductByCode_WithAuthentication_ReturnsProduct` - Authenticated users can view products
- `getProductByCode_WithoutAuthentication_ReturnsUnauthorized` - Unauthenticated requests are rejected (HTTP 401)
- `deleteProduct_WithAdminRole_ReturnsSuccess` - Only admins can delete products

**Expected Behavior:**

- Role-based access control enforced
- JWT authentication required for all endpoints
- ADMIN/BANKOFFICER roles for creation/updates
- CUSTOMER/ADMIN/BANKOFFICER roles for read operations
- Proper HTTP status codes returned

### Repository Layer Tests

**ProductRepositoryTest.java** - 4 test cases

- `findByProductCode_ReturnsProduct` - Tests finding products by unique code
- `existsByProductCode_ReturnsTrue` - Validates existence check functionality
- `findByProductType_ReturnsProducts` - Tests filtering by product type
- `searchProducts_WithFilters_ReturnsFilteredProducts` - Tests complex search queries

**Expected Behavior:**

- JPA queries execute correctly
- Custom query methods return accurate results
- Filtering and search work with multiple criteria
- Database constraints are enforced

## Running Tests

### Run All Tests

```bash
cd product-pricing
.\mvnw.cmd test
```

### Run Specific Test Class

```bash
.\mvnw.cmd test -Dtest=ProductServiceTest
```

### Run with Coverage

```bash
.\mvnw.cmd clean test jacoco:report
```

## Test Data

- Product codes follow pattern: `TYPE###` (e.g., SAV001, LOAN001)
- Effective dates default to current date
- Test products use USD currency unless specified
- Default status is ACTIVE for new products

## Role-Based Testing

- **CUSTOMER**: Can view products, search, and check status
- **ADMIN**: Full CRUD operations on products and pricing rules
- **BANKOFFICER**: Can create and update products, cannot delete

## Integration Testing

Tests use `@DataJpaTest` for repository tests with in-memory H2 database
Controllers tested with `@WebMvcTest` and MockMvc for endpoint validation
Security tests use `@WithMockUser` annotations for role simulation

## Expected Test Results

All 17 test cases should pass successfully with the following validations:

- Business rule enforcement
- Security and authorization
- Data persistence and retrieval
- Exception handling
- API contract compliance
