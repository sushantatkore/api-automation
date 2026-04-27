# Guide: Adding New APIs with Chaining

## Framework Architecture Overview

Your framework follows a **layered architecture**:

```
Test Layer (Tests)
    ↓
Workflow Layer (Business Logic & API Chaining)
    ↓
Service Layer (API Operations)
    ↓
Core Layer (HTTP Handling)
    ↓
Rest Assured (HTTP Client)
```

### Layers Explained:

- **Core Layer**: `BaseService`, `ApiResponse`, `RequestBuilder` - handles all HTTP operations
- **Service Layer**: Thin wrappers for each API (UserService, LoginService) - pure I/O, no assertions
- **Workflow Layer**: Business logic combining multiple services - assertions & validations
- **Test Layer**: Test cases using workflows - clean, readable test code

---

## Step-by-Step: Adding a New API (Example: Products API)

### Step 1: Define the Model

Create a model class in `src/main/java/com/automation/api/models/`

**File**: `Product.java`

```java
package com.automation.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product {
    private String id;
    private String name;
    private String description;
    private Double price;
    private Integer quantity;
    private String category;
    private String sku;
    private Long createdAt;
    private Long updatedAt;
}
```

### Step 2: Add Endpoint Constants

Update `src/main/java/com/automation/api/constants/Endpoints.java`

```java
public static final class Products {
    public static final String BASE = "/products";
    public static final String BY_ID = "/products/{id}";
    public static final String BY_CATEGORY = "/products/category/{category}";
    public static final String SEARCH = "/products/search";

    public static String byId(Object id) {
        return BASE + "/" + id;
    }

    private Products() {
    }
}
```

### Step 3: Create the Service

Create `src/main/java/com/automation/api/services/ProductService.java`

```java
package com.automation.api.services;

import com.automation.api.constants.Endpoints;
import com.automation.api.core.ApiResponse;
import com.automation.api.core.BaseService;
import com.automation.api.models.Product;

import java.util.Collections;
import java.util.Map;

/**
 * Service wrapper for /products resource. Pure I/O, no business logic.
 */
public class ProductService extends BaseService {

    public ApiResponse createProduct(Product product) {
        return post(Endpoints.Products.BASE, product);
    }

    public ApiResponse getProduct(String id) {
        return get(Endpoints.Products.BY_ID, Collections.emptyMap(), Map.of("id", id));
    }

    public ApiResponse listProducts(int page, int pageSize) {
        return get(Endpoints.Products.BASE, Map.of("page", page, "pageSize", pageSize));
    }

    public ApiResponse getProductsByCategory(String category) {
        return get(Endpoints.Products.BY_CATEGORY, Collections.emptyMap(), Map.of("category", category));
    }

    public ApiResponse searchProducts(String query) {
        return get(Endpoints.Products.SEARCH, Map.of("q", query));
    }

    public ApiResponse updateProduct(String id, Product product) {
        return put(Endpoints.Products.BY_ID, product, Map.of("id", id));
    }

    public ApiResponse deleteProduct(String id) {
        return delete(Endpoints.Products.BY_ID, Map.of("id", id));
    }
}
```

### Step 4: Create Workflow with API Chaining

Create `src/main/java/com/automation/api/workflows/ProductWorkflow.java`

This is where you implement **API chaining** - combining multiple API calls:

```java
package com.automation.api.workflows;

import com.automation.api.core.ApiResponse;
import com.automation.api.datafactory.ProductDataFactory;
import com.automation.api.models.Product;
import com.automation.api.services.ProductService;
import com.automation.api.utils.AssertionUtils;
import com.automation.api.utils.SchemaValidator;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Business workflows combining product API calls with assertions.
 * Implements API chaining patterns.
 */
public class ProductWorkflow {

    private static final Logger LOGGER = LogManager.getLogger(ProductWorkflow.class);
    private static final String PRODUCT_SCHEMA = "schemas/product-schema.json";

    private final ProductService productService = new ProductService();

    @Step("Create a new product and validate response")
    public Product createAndValidateProduct(Product candidate) {
        ApiResponse response = productService.createProduct(candidate);
        AssertionUtils.assertStatusCode(response, 201);
        AssertionUtils.assertResponseTimeUnder(response, 3_000L);
        SchemaValidator.validate(response, PRODUCT_SCHEMA);

        Product created = response.as(Product.class);
        AssertionUtils.assertNotBlank(created.getId(), "product id");
        AssertionUtils.assertEquals(created.getName(), candidate.getName(), "product.name");
        LOGGER.info("Product created with id={}, name={}", created.getId(), created.getName());
        return created;
    }

    @Step("Fetch product by id: {id}")
    public Product fetchAndValidateProduct(String id) {
        ApiResponse response = productService.getProduct(id);
        AssertionUtils.assertStatusCode(response, 200);
        SchemaValidator.validate(response, PRODUCT_SCHEMA);
        return response.as(Product.class);
    }

    /**
     * CHAINING EXAMPLE 1: Create → Fetch chain
     * Creates a product and immediately verifies it can be retrieved
     */
    @Step("Create then fetch product (chain)")
    public Product createThenFetchProduct(Product candidate) {
        // Step 1: Create product
        Product created = createAndValidateProduct(candidate);

        // Step 2: Fetch the created product
        Product fetched = fetchAndValidateProduct(created.getId());

        // Step 3: Verify they match
        AssertionUtils.assertEquals(fetched.getId(), created.getId(), "product.id");
        AssertionUtils.assertEquals(fetched.getName(), created.getName(), "product.name");

        LOGGER.info("Create-Fetch chain successful for product id={}", created.getId());
        return fetched;
    }

    /**
     * CHAINING EXAMPLE 2: Create → Update → Fetch chain
     * Creates a product, updates it, then verifies the update persisted
     */
    @Step("Create → Update → Fetch product (chain)")
    public Product createUpdateFetchProduct(Product candidate, String updatedName) {
        // Step 1: Create product
        Product created = createAndValidateProduct(candidate);

        // Step 2: Update the product
        Product toUpdate = created.toBuilder().name(updatedName).build();
        ApiResponse updateResponse = productService.updateProduct(created.getId(), toUpdate);
        AssertionUtils.assertStatusCode(updateResponse, 200);
        Product updated = updateResponse.as(Product.class);

        // Step 3: Fetch and verify update persisted
        Product fetched = fetchAndValidateProduct(created.getId());
        AssertionUtils.assertEquals(fetched.getName(), updatedName, "product.name after update");

        LOGGER.info("Create-Update-Fetch chain successful for product id={}", created.getId());
        return fetched;
    }

    /**
     * CHAINING EXAMPLE 3: Create → Search → Fetch chain
     * Creates a product, searches for it, then fetches by ID to verify data consistency
     */
    @Step("Create → Search → Fetch product (chain)")
    public Product createSearchFetchProduct(Product candidate) {
        // Step 1: Create product
        Product created = createAndValidateProduct(candidate);

        // Step 2: Search for the product
        ApiResponse searchResponse = productService.searchProducts(candidate.getName());
        AssertionUtils.assertStatusCode(searchResponse, 200);
        List<Product> searchResults = searchResponse.asList(Product.class);
        AssertionUtils.assertTrue(
            searchResults.stream().anyMatch(p -> p.getId().equals(created.getId())),
            "Product not found in search results"
        );

        // Step 3: Fetch directly to verify complete data
        Product fetched = fetchAndValidateProduct(created.getId());

        LOGGER.info("Create-Search-Fetch chain successful for product id={}", created.getId());
        return fetched;
    }

    /**
     * CHAINING EXAMPLE 4: Create Multiple → List → Fetch chain
     * Creates multiple products, lists them, and verifies they exist
     */
    @Step("Create multiple products → List → Fetch chain")
    public List<Product> createMultipleFetchChain(int count) {
        // Step 1: Create multiple products
        List<Product> createdProducts = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            Product candidate = ProductDataFactory.randomProduct();
            Product created = createAndValidateProduct(candidate);
            createdProducts.add(created);
        }

        // Step 2: List products (page 1)
        ApiResponse listResponse = productService.listProducts(1, 10);
        AssertionUtils.assertStatusCode(listResponse, 200);
        List<Product> listedProducts = listResponse.asList(Product.class);

        // Step 3: Verify all created products exist in list
        for (Product created : createdProducts) {
            Product fetched = fetchAndValidateProduct(created.getId());
            AssertionUtils.assertNotNull(fetched, "Product should exist after creation");
        }

        LOGGER.info("Create-Multiple-List-Fetch chain successful, created {} products", count);
        return createdProducts;
    }

    /**
     * CHAINING EXAMPLE 5: Filter by Category → Fetch → Update chain
     */
    @Step("List by category → Fetch → Update chain")
    public Product categoryFilterChain(String category) {
        // Step 1: Get products by category
        ApiResponse categoryResponse = productService.getProductsByCategory(category);
        AssertionUtils.assertStatusCode(categoryResponse, 200);
        List<Product> categoryProducts = categoryResponse.asList(Product.class);
        AssertionUtils.assertFalse(categoryProducts.isEmpty(), "No products found in category");

        // Step 2: Fetch first product
        Product first = categoryProducts.get(0);
        Product fetched = fetchAndValidateProduct(first.getId());

        // Step 3: Update the product
        Product updated = fetched.toBuilder()
            .quantity(fetched.getQuantity() + 1)
            .build();
        ApiResponse updateResponse = productService.updateProduct(fetched.getId(), updated);
        AssertionUtils.assertStatusCode(updateResponse, 200);

        LOGGER.info("Category-Filter-Fetch-Update chain successful for product id={}", fetched.getId());
        return updated.as(Product.class);
    }

    @Step("Cleanup product: {id}")
    public void cleanupProduct(String id) {
        if (id == null || id.isBlank()) {
            LOGGER.warn("Skip cleanup — id is null/blank");
            return;
        }
        try {
            ApiResponse response = productService.deleteProduct(id);
            if (response.statusCode() != 200 && response.statusCode() != 204 && response.statusCode() != 404) {
                LOGGER.warn("Cleanup DELETE /products/{} returned unexpected status {}", id, response.statusCode());
            } else {
                LOGGER.info("Cleanup DELETE /products/{} -> {}", id, response.statusCode());
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Cleanup for product {} failed: {}", id, e.getMessage());
        }
    }
}
```

### Step 5: Create Test Class

Create `src/test/java/com/automation/api/tests/ProductApiTests.java`

```java
package com.automation.api.tests;

import com.automation.api.datafactory.ProductDataFactory;
import com.automation.api.models.Product;
import com.automation.api.workflows.ProductWorkflow;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

@Epic("Products API")
@Feature("Product lifecycle")
public class ProductApiTests extends BaseTest {

    private final ProductWorkflow productWorkflow = new ProductWorkflow();
    private final List<String> createdProductIds = new ArrayList<>();

    @BeforeMethod(alwaysRun = true)
    public void resetCreatedIds() {
        createdProductIds.clear();
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup() {
        for (String id : createdProductIds) {
            productWorkflow.cleanupProduct(id);
        }
    }

    @Test(groups = {"smoke", "regression"})
    @Story("Create product")
    @Severity(SeverityLevel.BLOCKER)
    @Description("POST /products with valid payload should return 201")
    public void createProduct_happyPath() {
        Product product = ProductDataFactory.randomProduct();
        Product created = productWorkflow.createAndValidateProduct(product);
        createdProductIds.add(created.getId());
    }

    @Test(groups = {"smoke", "regression"})
    @Story("Create then fetch")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Create → Fetch chain should return consistent data")
    public void createThenFetchProduct_chain() {
        Product product = ProductDataFactory.randomProduct();
        Product fetched = productWorkflow.createThenFetchProduct(product);
        createdProductIds.add(fetched.getId());
    }

    @Test(groups = {"regression"})
    @Story("Create → Update → Fetch")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Create → Update → Fetch chain verifies updates persist")
    public void createUpdateFetchProduct_chain() {
        Product product = ProductDataFactory.randomProduct();
        Product updated = productWorkflow.createUpdateFetchProduct(product, "Updated-" + product.getName());
        createdProductIds.add(updated.getId());
    }

    @Test(groups = {"regression"})
    @Story("Create → Search → Fetch")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify product appears in search results after creation")
    public void createSearchFetchProduct_chain() {
        Product product = ProductDataFactory.randomProduct();
        Product result = productWorkflow.createSearchFetchProduct(product);
        createdProductIds.add(result.getId());
    }

    @Test(groups = {"regression"})
    @Story("Create Multiple → List → Fetch")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify multiple products appear in list endpoint")
    public void createMultipleFetchChain() {
        List<Product> created = productWorkflow.createMultipleFetchChain(3);
        created.forEach(p -> createdProductIds.add(p.getId()));
    }
}
```

### Step 6: Create Data Factory

Create `src/main/java/com/automation/api/datafactory/ProductDataFactory.java`

```java
package com.automation.api.datafactory;

import com.automation.api.models.Product;
import net.datafaker.Faker;

public class ProductDataFactory {

    private static final Faker faker = new Faker();

    public static Product randomProduct() {
        return Product.builder()
            .name(faker.commerce().productName())
            .description(faker.lorem().paragraph())
            .price(Double.parseDouble(faker.commerce().price()))
            .quantity(faker.number().randomDigitNotZero())
            .category(faker.commerce().department())
            .sku(faker.code().ean13())
            .build();
    }

    public static Product productWithCategory(String category) {
        Product product = randomProduct();
        return product.toBuilder()
            .category(category)
            .build();
    }

    public static Product productWithPrice(Double price) {
        Product product = randomProduct();
        return product.toBuilder()
            .price(price)
            .build();
    }
}
```

### Step 7: Create JSON Schema Validation

Create `src/test/resources/schemas/product-schema.json`

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "id": {
      "type": "string"
    },
    "name": {
      "type": "string",
      "minLength": 1
    },
    "description": {
      "type": "string"
    },
    "price": {
      "type": "number",
      "minimum": 0
    },
    "quantity": {
      "type": "integer",
      "minimum": 0
    },
    "category": {
      "type": "string"
    },
    "sku": {
      "type": "string"
    },
    "createdAt": {
      "type": "integer"
    },
    "updatedAt": {
      "type": "integer"
    }
  },
  "required": ["id", "name", "price"]
}
```

### Step 8: Update testng.xml

Add a new test package to `testng.xml`:

```xml
<test name="Products API - Regression">
    <groups>
        <run>
            <include name="sanity"/>
            <include name="regression"/>
        </run>
    </groups>
    <packages>
        <package name="com.automation.api.tests.ProductApiTests"/>
    </packages>
</test>
```

---

## API Chaining Patterns

### Pattern 1: Sequential Chain (Create → Fetch)

```
create() → fetch() → assert match
```

**Use Case**: Verify created resource is retrievable

### Pattern 2: Modify Chain (Create → Update → Fetch)

```
create() → update() → fetch() → assert updated value
```

**Use Case**: Verify updates persist

### Pattern 3: Search Chain (Create → Search → Fetch)

```
create() → search() → list result → fetch detail → assert consistency
```

**Use Case**: Verify search indexes and data consistency

### Pattern 4: Multi-Step Chain (Create → List → Filter → Update)

```
create multiple() → list() → filter() → update() → fetch() → assert
```

**Use Case**: Complex workflows with multiple operations

### Pattern 5: Cleanup Chain (Create → Test → Delete → Verify Deleted)

```
create() → assertions() → delete() → fetch fail (404)
```

**Use Case**: Ensure proper resource cleanup

---

## File Structure Summary

```
src/
├── main/java/com/automation/api/
│   ├── core/                  # BaseService, ApiResponse, RequestBuilder
│   ├── services/              # ProductService.java ← NEW
│   ├── models/                # Product.java ← NEW
│   ├── workflows/             # ProductWorkflow.java ← NEW (API Chaining)
│   ├── datafactory/           # ProductDataFactory.java ← NEW
│   ├── constants/             # Endpoints.java (UPDATE)
│   └── ...
│
├── test/java/com/automation/api/
│   ├── tests/                 # ProductApiTests.java ← NEW
│   └── ...
│
└── test/resources/
    └── schemas/               # product-schema.json ← NEW
```

---

## Key Best Practices

✅ **DO:**

- Keep services thin (pure I/O, no assertions)
- Put all business logic in workflows
- Use `@Step` annotations for Allure reporting
- Chain multiple services in workflows
- Create reusable data factories
- Validate with JSON schemas
- Clean up resources in `@AfterMethod`

❌ **DON'T:**

- Don't put assertions in service layer
- Don't hardcode endpoints in tests
- Don't forget to extract response data (`.as(Product.class)`)
- Don't skip schema validation
- Don't ignore cleanup
- Don't make tests dependent on each other

---

## Running Tests

```bash
# Run all tests
mvn clean test

# Run specific test class
mvn clean test -Dtest=ProductApiTests

# Run specific test method
mvn clean test -Dtest=ProductApiTests#createThenFetchProduct_chain

# Run by group
mvn clean test -Dgroups=smoke

# With specific environment
mvn clean test -Dtest.env=dev

# Generate Allure report
mvn allure:report
```

---

## Debugging & Logging

All requests/responses are logged via Log4j. Check:

```
target/logs/
```

Allure reports include:

```
target/allure-report/index.html
```

---

## Next Steps

1. Create the ProductService & ProductWorkflow
2. Create ProductDataFactory with test data
3. Create ProductApiTests with API chaining examples
4. Run tests: `mvn clean test -Dtest=ProductApiTests`
5. View Allure report: `mvn allure:report`
