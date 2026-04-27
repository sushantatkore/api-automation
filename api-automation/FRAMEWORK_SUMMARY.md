# Framework Structure - Summary

## Current Framework State

Your API Automation Framework is built with **4 Layers + API Chaining**:

```
TESTS (ProductApiTests, UserApiTests)
    ↓ Uses
WORKFLOWS (Chaining Logic - ProductWorkflow, UserWorkflow)
    ↓ Uses
SERVICES (Pure I/O - ProductService, UserService)
    ↓ Uses
CORE (BaseService, ApiResponse, RequestBuilder)
    ↓ Uses
REST ASSURED (HTTP Client)
```

---

## How to Add New API: 8-Step Process

### Step 1: Create Model

- **Path**: `src/main/java/com/automation/api/models/YourModel.java`
- **Contains**: Data fields with Lombok annotations
- **Example**: `Product.java`, `User.java`

### Step 2: Add Endpoints

- **Path**: `src/main/java/com/automation/api/constants/Endpoints.java`
- **Contains**: URL constants for your API
- **Example**: Add `Products` class with BASE, BY_ID endpoints

### Step 3: Create Service

- **Path**: `src/main/java/com/automation/api/services/YourService.java`
- **Contains**: Thin wrappers around service methods
- **Methods**: `create()`, `get()`, `list()`, `update()`, `delete()`
- **Returns**: `ApiResponse` (no assertions)

### Step 4: Create Workflow (API Chaining)

- **Path**: `src/main/java/com/automation/api/workflows/YourWorkflow.java`
- **Contains**: Business logic combining multiple service calls
- **Methods**: Single-step operations + chaining operations
- **Chaining**: `createThenFetch()`, `createUpdateFetch()`, `createMultipleList()`
- **Returns**: Model objects (with assertions already done)

### Step 5: Create Data Factory

- **Path**: `src/main/java/com/automation/api/datafactory/YourDataFactory.java`
- **Contains**: Realistic test data generation using Faker
- **Methods**: `random()`, `withName()`, `withCategory()`

### Step 6: Create Test Class

- **Path**: `src/test/java/com/automation/api/tests/YourApiTests.java`
- **Contains**: Test methods using workflows
- **Methods**: Only call workflow methods, never service methods directly
- **Cleanup**: `@AfterMethod` with cleanup logic

### Step 7: Create JSON Schema

- **Path**: `src/test/resources/schemas/your-schema.json`
- **Contains**: JSON schema for response validation
- **Used by**: `SchemaValidator.validate(response, schema)`

### Step 8: Update Configuration

- **File**: `testng.xml`
- **Action**: Add your test class to suite

---

## API Chaining Explained

**What is API Chaining?**
Combining multiple API calls in a single workflow to test complex business scenarios.

**Why Chain APIs?**

- Verify data persistence across operations
- Test dependencies between resources
- Simulate real-world user workflows
- Ensure consistency across system

### Chaining Examples

#### Example 1: Create → Fetch

```
ProductWorkflow.createThenFetchProduct()
├─ Call 1: POST /products (create)
│   Response: { id: "123", name: "Widget" }
└─ Call 2: GET /products/123 (fetch)
    Response: { id: "123", name: "Widget" }

Verification: Both responses have same data ✓
```

#### Example 2: Create → Update → Fetch

```
ProductWorkflow.createUpdateFetchProduct()
├─ Call 1: POST /products (create)
│   Response: { id: "123", name: "Widget", price: 100 }
├─ Call 2: PUT /products/123 (update)
│   Request: { name: "Updated Widget", price: 150 }
│   Response: { id: "123", name: "Updated Widget", price: 150 }
└─ Call 3: GET /products/123 (fetch)
    Response: { id: "123", name: "Updated Widget", price: 150 }

Verification: Updates persisted in database ✓
```

#### Example 3: Create → Search → Fetch

```
ProductWorkflow.createSearchFetchProduct()
├─ Call 1: POST /products (create)
│   Response: { id: "123", name: "Widget XYZ" }
├─ Call 2: GET /products/search?q=Widget (search)
│   Response: [{ id: "123", name: "Widget XYZ" }, ...]
└─ Call 3: GET /products/123 (fetch)
    Response: { id: "123", name: "Widget XYZ" }

Verification: Product appears in search results ✓
```

---

## File Organization

### Main Source Code

```
src/main/java/com/automation/api/
├── core/
│   ├── BaseService.java          ← HTTP methods (GET, POST, PUT, DELETE)
│   ├── ApiResponse.java           ← Response wrapper
│   ├── RequestBuilder.java        ← Auth & headers
│   └── RetryHandler.java          ← Automatic retries
│
├── constants/
│   ├── Endpoints.java             ← Endpoint URLs
│   └── Headers.java               ← Header constants
│
├── services/                       ← ONE per API
│   ├── UserService.java
│   ├── LoginService.java
│   └── ProductService.java        ← NEW
│
├── models/                         ← Data classes
│   ├── User.java
│   ├── AuthResponse.java
│   └── Product.java               ← NEW
│
├── workflows/                      ← API Chaining logic
│   ├── UserWorkflow.java
│   └── ProductWorkflow.java       ← NEW
│
├── datafactory/                    ← Test data
│   ├── UserDataFactory.java
│   └── ProductDataFactory.java    ← NEW
│
├── utils/
│   ├── AssertionUtils.java
│   ├── SchemaValidator.java
│   └── JsonUtils.java
│
└── Other folders...
```

### Test Code

```
src/test/java/com/automation/api/tests/
├── BaseTest.java                  ← Base test class
├── UserApiTests.java
├── LoginTest.java
└── ProductApiTests.java           ← NEW

src/test/resources/
├── schemas/
│   ├── user-schema.json
│   └── product-schema.json        ← NEW
├── config.properties              ← Base config
├── config-dev.properties
├── config-qa.properties
└── testdata/
```

---

## Key Framework Components

### BaseService - The HTTP Foundation

```java
protected ApiResponse get(String path, Map<String, ?> queryParams, Map<String, ?> pathParams)
protected ApiResponse post(String path, Object body)
protected ApiResponse put(String path, Object body, Map<String, ?> pathParams)
protected ApiResponse patch(String path, Object body, Map<String, ?> pathParams)
protected ApiResponse delete(String path, Map<String, ?> pathParams)
```

Every service extends `BaseService` and uses these methods.

### ApiResponse - Safe Response Handling

```java
response.statusCode()           // Get HTTP status
response.body()                 // Get response body as string
response.as(User.class)         // Convert to model
response.asList(User.class)     // Convert to list
response.isSuccess()            // Check if 200-299
response.header("Content-Type") // Get header
```

### Workflow Pattern - Where Chaining Happens

```java
@Step("Descriptive name for Allure report")
public Model doSomething() {
    // Step 1: Single operation
    Model result = service.operation();

    // Step 2: Assert result
    AssertionUtils.assertStatusCode(response, 200);

    // Step 3: Chain another call
    Model fetched = service.fetch(result.getId());

    // Step 4: Assert consistency
    AssertionUtils.assertEquals(fetched.getId(), result.getId(), "id");

    return fetched;
}
```

---

## Execution Flow Example

### Test: `ProductApiTests.createUpdateFetchTest()`

```
1. TEST START
   └─ Test method calls: workflow.createUpdateFetch(product, "NewName")

2. WORKFLOW LAYER
   └─ productWorkflow.createUpdateFetch()
      ├─ Call workflow.createAndValidate()
      │   └─ service.createProduct(product)
      │       └─ BaseService.post("/products", product)
      │           └─ RestAssured: POST request
      │               ├─ Response: 201, { id: "123", ... }
      │               └─ Assert & Validate
      │
      ├─ Call service.updateProduct(id, updatedProduct)
      │   └─ BaseService.put("/products/123", updatedProduct)
      │       └─ RestAssured: PUT request
      │           ├─ Response: 200, { id: "123", name: "NewName" }
      │           └─ Assert
      │
      └─ Call workflow.fetchAndValidate()
          └─ service.getProduct(id)
              └─ BaseService.get("/products/123")
                  └─ RestAssured: GET request
                      ├─ Response: 200, { id: "123", name: "NewName" }
                      └─ Assert name matches "NewName"

3. ALLURE REPORT
   └─ Shows 3 steps with all details:
      ├─ Create and validate ✓
      ├─ Update product ✓
      └─ Fetch and validate ✓

4. TEST COMPLETE
   └─ Result: PASSED or FAILED
```

---

## Running Tests

```bash
# Run all tests
mvn clean test

# Run specific test class
mvn clean test -Dtest=ProductApiTests

# Run specific test method
mvn clean test -Dtest=ProductApiTests#createUpdateFetchTest

# Run by group
mvn clean test -Dgroups=smoke

# Run with specific environment
mvn clean test -Dtest.env=qa

# Generate Allure report
mvn allure:report
mvn allure:serve  # Opens in browser
```

---

## Best Practices

### ✅ DO:

- Keep services thin (pure I/O)
- Put logic in workflows
- Chain operations in workflows
- Use `@Step` for Allure
- Validate responses with schemas
- Clean up resources in `@AfterMethod`
- Use data factories for test data
- Extract IDs for cleanup
- Add meaningful descriptions

### ❌ DON'T:

- Don't put assertions in service layer
- Don't hardcode endpoints in tests
- Don't skip schema validation
- Don't forget to cleanup
- Don't make tests dependent on each other
- Don't compose services directly in tests
- Don't ignore exceptions
- Don't forget cleanup for failed tests
- Don't mix business logic with assertions

---

## Troubleshooting

| Problem                   | Solution                                |
| ------------------------- | --------------------------------------- |
| Tests fail with 401       | Check auth token in RequestBuilder      |
| 404 errors                | Verify endpoint paths in Endpoints.java |
| Timeout errors            | Increase timeout in config properties   |
| Schema validation fails   | Update JSON schema file                 |
| Tests fail inconsistently | Add retry logic or increase timeout     |
| Allure report missing     | Run `mvn allure:report`                 |

---

## Quick Links to Documents

1. **NEW_API_GUIDE.md** - Complete step-by-step guide with code examples
2. **ARCHITECTURE_AND_CHAINING.md** - Detailed architecture & chaining patterns
3. **TEMPLATES.md** - Ready-to-use code templates for quick implementation

---

## Next Steps

1. Read **NEW_API_GUIDE.md** for complete walkthrough
2. Use **TEMPLATES.md** for quick implementation
3. Reference **ARCHITECTURE_AND_CHAINING.md** for patterns
4. Create your first new API using the 8-step process
5. Run tests: `mvn clean test -Dtest=YourApiTests`
6. View report: `mvn allure:serve`

---

## Support

All methods in `BaseService` handle:

- ✓ Retry on failure (automatic)
- ✓ Logging (check target/logs/)
- ✓ Response time tracking
- ✓ Authentication (via RequestBuilder)
- ✓ Error handling

No manual HTTP handling needed!
