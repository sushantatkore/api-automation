# API Automation Framework - Architecture & Chaining Patterns

## Framework Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    TEST LAYER                              │
│        (ProductApiTests, UserApiTests, etc.)               │
│  - Clean test methods with @Test annotations              │
│  - Uses workflows only                                    │
│  - Manages cleanup in @AfterMethod                        │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                  WORKFLOW LAYER (API Chaining)             │
│        (ProductWorkflow, UserWorkflow, etc.)               │
│  - Combines multiple service calls                        │
│  - Implements business logic                              │
│  - Adds assertions & validations                          │
│  - Uses @Step for Allure reporting                        │
│  - CHAINING HAPPENS HERE                                  │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   SERVICE LAYER                            │
│          (ProductService, UserService, etc.)               │
│  - Thin wrappers around API endpoints                     │
│  - No business logic                                      │
│  - No assertions                                          │
│  - Returns ApiResponse                                    │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   CORE LAYER                               │
│  - BaseService: GET/POST/PUT/PATCH/DELETE methods         │
│  - ApiResponse: Response wrapper                          │
│  - RequestBuilder: Auth & headers setup                   │
│  - RetryHandler: Automatic retry logic                    │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              REST ASSURED (HTTP Client)                    │
└─────────────────────────────────────────────────────────────┘
```

---

## API Chaining Examples

### 1. Simple Chain: Create → Fetch

```
ProductWorkflow:
  └─ createThenFetchProduct()
      ├─ ProductService.createProduct()      [POST /products]
      │   └─ Creates product, returns ID
      │
      └─ ProductService.getProduct(id)        [GET /products/{id}]
          └─ Fetches created product

Result: Verify created data matches fetched data
```

### 2. Complex Chain: Create → Update → Fetch

```
ProductWorkflow:
  └─ createUpdateFetchProduct()
      ├─ ProductService.createProduct()        [POST /products]
      │   └─ Creates product
      │
      ├─ ProductService.updateProduct(id)      [PUT /products/{id}]
      │   └─ Updates product name
      │
      └─ ProductService.getProduct(id)         [GET /products/{id}]
          └─ Fetches and verifies update

Result: Verify updates persist in database
```

### 3. Search Chain: Create → Search → Fetch

```
ProductWorkflow:
  └─ createSearchFetchProduct()
      ├─ ProductService.createProduct()        [POST /products]
      │   └─ Creates product
      │
      ├─ ProductService.searchProducts(name)   [GET /products/search?q=...]
      │   └─ Searches for product
      │
      └─ ProductService.getProduct(id)         [GET /products/{id}]
          └─ Fetches full details

Result: Verify search index is updated and data is consistent
```

### 4. Multi-Resource Chain: Create User → Create Product → Link

```
ProductWorkflow:
  └─ createProductForUser()
      ├─ UserService.getUser(userId)           [GET /users/{userId}]
      │   └─ Validate user exists
      │
      ├─ ProductService.createProduct()        [POST /products]
      │   └─ Creates product
      │
      └─ ProductService.linkToUser()            [POST /products/{id}/users/{userId}]
          └─ Associates product with user

Result: Verify product is linked to user
```

---

## Adding a New API - Complete Checklist

### Layer 1: Model

- [ ] Create `Model.java` in `src/main/java/.../models/`
- [ ] Use Lombok: `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- [ ] Add `@JsonIgnoreProperties(ignoreUnknown = true)`

### Layer 2: Constants

- [ ] Update `Endpoints.java`
- [ ] Add static inner class for your API
- [ ] Define all endpoints (BASE, BY_ID, etc.)
- [ ] Add helper methods like `byId(Object id)`

### Layer 3: Service

- [ ] Create `XxxService.java` in `src/main/java/.../services/`
- [ ] Extend `BaseService`
- [ ] Implement methods using: `get()`, `post()`, `put()`, `patch()`, `delete()`
- [ ] Return `ApiResponse` from all methods
- [ ] No assertions, no logging of results

### Layer 4: Data Factory

- [ ] Create `XxxDataFactory.java` in `src/main/java/.../datafactory/`
- [ ] Use `Faker` for realistic test data
- [ ] Create variations: `random()`, `withCategory()`, `withPrice()`, etc.

### Layer 5: Workflow (API Chaining)

- [ ] Create `XxxWorkflow.java` in `src/main/java/.../workflows/`
- [ ] Use methods from Service
- [ ] Add assertions using `AssertionUtils`
- [ ] Validate responses with `SchemaValidator`
- [ ] Chain multiple service calls in one method
- [ ] Use `@Step` annotations for Allure
- [ ] Add cleanup method

### Layer 6: Schema Validation

- [ ] Create `xxx-schema.json` in `src/test/resources/schemas/`
- [ ] Define JSON schema with required fields
- [ ] Reference in workflow using `SchemaValidator.validate()`

### Layer 7: Test Class

- [ ] Create `XxxApiTests.java` in `src/test/java/.../tests/`
- [ ] Extend `BaseTest`
- [ ] Create `Workflow` instance
- [ ] Maintain `createdIds` list
- [ ] Clear IDs in `@BeforeMethod`
- [ ] Cleanup resources in `@AfterMethod`
- [ ] Write test methods using workflow methods
- [ ] Use `@Test`, `@Story`, `@Severity` annotations
- [ ] Add `@Description` for clarity

### Layer 8: Configuration

- [ ] Update `testng.xml` with new test package
- [ ] Add environment configs if needed in `config-*.properties`

---

## API Chaining Best Practices

### ✅ DO THIS:

```java
// Workflow method that chains multiple calls
@Step("Create → Update → Fetch chain")
public Product createUpdateFetchChain(Product candidate) {
    // Step 1: Create
    Product created = createAndValidateProduct(candidate);

    // Step 2: Update
    Product updated = created.toBuilder()
        .name("Updated: " + created.getName())
        .build();
    ApiResponse response = productService.updateProduct(created.getId(), updated);
    AssertionUtils.assertStatusCode(response, 200);

    // Step 3: Fetch and verify
    Product fetched = fetchAndValidateProduct(created.getId());
    AssertionUtils.assertEquals(fetched.getName(), updated.getName(), "name mismatch");

    return fetched;
}
```

### ❌ DON'T DO THIS:

```java
// BAD: Chaining without assertions between steps
public Product badChain(Product candidate) {
    Product created = productService.createProduct(candidate).as(Product.class);
    Product updated = productService.updateProduct(created.getId(), candidate).as(Product.class);
    return productService.getProduct(created.getId()).as(Product.class);
    // No assertions! Can't tell which step failed
}

// BAD: Putting chaining logic in test
@Test
public void testBadChain() {
    Product created = productService.createProduct(...);
    Product updated = productService.updateProduct(...);
    Product fetched = productService.getProduct(...);
    // Tests should use workflows, not compose services!
}
```

---

## Execution Flow Example

### Test: `createUpdateFetchProduct_chain()`

```
START TEST
  │
  ├─→ Workflow.createUpdateFetchProduct()
  │     │
  │     ├─→ Service.createProduct()
  │     │     └─→ BaseService.post()
  │     │           └─→ RequestBuilder.build()
  │     │                 └─→ Rest Assured HTTP POST
  │     │
  │     ├─→ AssertionUtils.assertStatusCode(201)
  │     │
  │     ├─→ SchemaValidator.validate(response, schema)
  │     │
  │     ├─→ Service.updateProduct(id, updatedData)
  │     │     └─→ BaseService.put()
  │     │           └─→ Rest Assured HTTP PUT
  │     │
  │     ├─→ AssertionUtils.assertStatusCode(200)
  │     │
  │     └─→ Service.getProduct(id)
  │           └─→ BaseService.get()
  │                 └─→ Rest Assured HTTP GET
  │
  └─→ Verify all responses match expected values

ALLURE REPORT SHOWS:
  - CREATE → UPDATE → FETCH (3 @Step entries)
  - All HTTP requests & responses
  - Assertion details
  - Timeline of execution
```

---

## Configuration Files

### Environment-specific Properties

**src/test/resources/config-qa.properties:**

```properties
api.base.url=https://qa-api.example.com
api.connection.timeout.ms=10000
api.read.timeout.ms=30000
```

**src/test/resources/config-dev.properties:**

```properties
api.base.url=https://dev-api.example.com
api.connection.timeout.ms=15000
api.read.timeout.ms=45000
```

Run tests with specific environment:

```bash
mvn clean test -Dtest.env=qa
mvn clean test -Dtest.env=dev
```

---

## Common Issues & Solutions

| Issue                          | Cause                   | Solution                             |
| ------------------------------ | ----------------------- | ------------------------------------ |
| 401 Unauthorized               | Auth token expired      | Check `RequestBuilder` auth logic    |
| 404 Not Found                  | Wrong endpoint          | Verify `Endpoints.java` constants    |
| Timeout                        | Slow API                | Increase timeout in config           |
| Schema validation fails        | Response format changed | Update `xxx-schema.json`             |
| Assertions fail inconsistently | Race condition          | Add delay or retry logic             |
| Tests in wrong order           | Test dependency         | Use `@BeforeMethod` & `@AfterMethod` |

---

## Key Files to Know

| File                   | Purpose                            |
| ---------------------- | ---------------------------------- |
| `BaseService.java`     | HTTP methods (GET/POST/PUT/DELETE) |
| `ApiResponse.java`     | Response wrapper                   |
| `RequestBuilder.java`  | Auth & headers                     |
| `Endpoints.java`       | Central endpoint registry          |
| `AssertionUtils.java`  | Custom assertions                  |
| `SchemaValidator.java` | JSON schema validation             |
| `testng.xml`           | Test suite configuration           |
