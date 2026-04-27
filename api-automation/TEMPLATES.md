# Quick Reference: Templates for Adding New APIs

Use these templates to quickly add new APIs to your framework.

## Template 1: Model Class

**File**: `src/main/java/com/automation/api/models/YourModel.java`

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
public class YourModel {
    private String id;
    private String name;
    // Add other fields
}
```

---

## Template 2: Service Class

**File**: `src/main/java/com/automation/api/services/YourService.java`

```java
package com.automation.api.services;

import com.automation.api.constants.Endpoints;
import com.automation.api.core.ApiResponse;
import com.automation.api.core.BaseService;
import com.automation.api.models.YourModel;

import java.util.Collections;
import java.util.Map;

public class YourService extends BaseService {

    public ApiResponse create(YourModel model) {
        return post(Endpoints.YourApi.BASE, model);
    }

    public ApiResponse get(String id) {
        return get(Endpoints.YourApi.BY_ID, Collections.emptyMap(), Map.of("id", id));
    }

    public ApiResponse list(int page, int pageSize) {
        return get(Endpoints.YourApi.BASE, Map.of("page", page, "pageSize", pageSize));
    }

    public ApiResponse update(String id, YourModel model) {
        return put(Endpoints.YourApi.BY_ID, model, Map.of("id", id));
    }

    public ApiResponse delete(String id) {
        return delete(Endpoints.YourApi.BY_ID, Map.of("id", id));
    }
}
```

---

## Template 3: Endpoint Constants

**Update**: `src/main/java/com/automation/api/constants/Endpoints.java`

```java
public static final class YourApi {
    public static final String BASE = "/your-endpoint";
    public static final String BY_ID = "/your-endpoint/{id}";
    // Add other endpoints

    public static String byId(Object id) {
        return BASE + "/" + id;
    }

    private YourApi() {
    }
}
```

---

## Template 4: Workflow with API Chaining

**File**: `src/main/java/com/automation/api/workflows/YourWorkflow.java`

```java
package com.automation.api.workflows;

import com.automation.api.core.ApiResponse;
import com.automation.api.models.YourModel;
import com.automation.api.services.YourService;
import com.automation.api.utils.AssertionUtils;
import com.automation.api.utils.SchemaValidator;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class YourWorkflow {

    private static final Logger LOGGER = LogManager.getLogger(YourWorkflow.class);
    private static final String SCHEMA = "schemas/your-schema.json";

    private final YourService service = new YourService();

    @Step("Create and validate")
    public YourModel createAndValidate(YourModel candidate) {
        ApiResponse response = service.create(candidate);
        AssertionUtils.assertStatusCode(response, 201);
        SchemaValidator.validate(response, SCHEMA);

        YourModel created = response.as(YourModel.class);
        AssertionUtils.assertNotBlank(created.getId(), "id");
        LOGGER.info("Created: {}", created.getId());
        return created;
    }

    @Step("Fetch and validate")
    public YourModel fetchAndValidate(String id) {
        ApiResponse response = service.get(id);
        AssertionUtils.assertStatusCode(response, 200);
        SchemaValidator.validate(response, SCHEMA);
        return response.as(YourModel.class);
    }

    /**
     * CHAINING: Create → Fetch
     */
    @Step("Create then fetch (chain)")
    public YourModel createThenFetch(YourModel candidate) {
        // Step 1: Create
        YourModel created = createAndValidate(candidate);

        // Step 2: Fetch
        YourModel fetched = fetchAndValidate(created.getId());

        // Step 3: Assert
        AssertionUtils.assertEquals(fetched.getId(), created.getId(), "id");
        return fetched;
    }

    /**
     * CHAINING: Create → Update → Fetch
     */
    @Step("Create → Update → Fetch (chain)")
    public YourModel createUpdateFetch(YourModel candidate, String newName) {
        // Step 1: Create
        YourModel created = createAndValidate(candidate);

        // Step 2: Update
        YourModel toUpdate = created.toBuilder().name(newName).build();
        ApiResponse updateResponse = service.update(created.getId(), toUpdate);
        AssertionUtils.assertStatusCode(updateResponse, 200);

        // Step 3: Fetch and verify
        YourModel fetched = fetchAndValidate(created.getId());
        AssertionUtils.assertEquals(fetched.getName(), newName, "name");
        return fetched;
    }

    /**
     * CHAINING: Create Multiple → List → Fetch
     */
    @Step("Create multiple → List → Fetch (chain)")
    public void createMultipleListFetch(int count) {
        // Step 1: Create multiple
        for (int i = 0; i < count; i++) {
            createAndValidate(YourDataFactory.random());
        }

        // Step 2: List
        ApiResponse listResponse = service.list(1, 10);
        AssertionUtils.assertStatusCode(listResponse, 200);

        // Step 3: Fetch each to verify
        java.util.List<YourModel> listed = listResponse.asList(YourModel.class);
        for (YourModel item : listed) {
            fetchAndValidate(item.getId());
        }
    }

    @Step("Cleanup")
    public void cleanup(String id) {
        if (id == null || id.isBlank()) return;
        try {
            service.delete(id);
        } catch (Exception e) {
            LOGGER.warn("Cleanup failed: {}", e.getMessage());
        }
    }
}
```

---

## Template 5: Data Factory

**File**: `src/main/java/com/automation/api/datafactory/YourDataFactory.java`

```java
package com.automation.api.datafactory;

import com.automation.api.models.YourModel;
import net.datafaker.Faker;

public class YourDataFactory {

    private static final Faker faker = new Faker();

    public static YourModel random() {
        return YourModel.builder()
            .name(faker.name().fullName())
            .build();
    }

    public static YourModel withName(String name) {
        return random().toBuilder().name(name).build();
    }
}
```

---

## Template 6: Test Class

**File**: `src/test/java/com/automation/api/tests/YourApiTests.java`

```java
package com.automation.api.tests;

import com.automation.api.datafactory.YourDataFactory;
import com.automation.api.models.YourModel;
import com.automation.api.workflows.YourWorkflow;
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

@Epic("Your API")
@Feature("Your Feature")
public class YourApiTests extends BaseTest {

    private final YourWorkflow workflow = new YourWorkflow();
    private final List<String> createdIds = new ArrayList<>();

    @BeforeMethod(alwaysRun = true)
    public void resetIds() {
        createdIds.clear();
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupAll() {
        for (String id : createdIds) {
            workflow.cleanup(id);
        }
    }

    @Test(groups = {"smoke", "regression"})
    @Story("Create")
    @Severity(SeverityLevel.BLOCKER)
    @Description("POST /your-endpoint should create resource")
    public void createTest() {
        YourModel model = YourDataFactory.random();
        YourModel created = workflow.createAndValidate(model);
        createdIds.add(created.getId());
    }

    @Test(groups = {"regression"})
    @Story("Create → Fetch Chain")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify create → fetch chain works")
    public void createThenFetchTest() {
        YourModel model = YourDataFactory.random();
        YourModel fetched = workflow.createThenFetch(model);
        createdIds.add(fetched.getId());
    }

    @Test(groups = {"regression"})
    @Story("Create → Update → Fetch Chain")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify updates persist")
    public void createUpdateFetchTest() {
        YourModel model = YourDataFactory.random();
        YourModel updated = workflow.createUpdateFetch(model, "Updated-Name");
        createdIds.add(updated.getId());
    }

    @Test(groups = {"regression"})
    @Story("Create Multiple → List → Fetch Chain")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify list endpoint works")
    public void createMultipleListFetchTest() {
        workflow.createMultipleListFetch(3);
    }
}
```

---

## Template 7: JSON Schema

**File**: `src/test/resources/schemas/your-schema.json`

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
    "createdAt": {
      "type": "integer"
    },
    "updatedAt": {
      "type": "integer"
    }
  },
  "required": ["id", "name"]
}
```

---

## Template 8: Update testng.xml

```xml
<test name="Your API - Regression">
    <groups>
        <run>
            <include name="smoke"/>
            <include name="regression"/>
        </run>
    </groups>
    <packages>
        <package name="com.automation.api.tests.YourApiTests"/>
    </packages>
</test>
```

---

## 5-Minute Implementation Checklist

1. **Model** (2 min)
   - [ ] Copy Template 1
   - [ ] Add your fields

2. **Service** (2 min)
   - [ ] Copy Template 2
   - [ ] Update method names

3. **Endpoints** (1 min)
   - [ ] Update `Endpoints.java` with Template 3

4. **Workflow** (5 min)
   - [ ] Copy Template 4
   - [ ] Add your chaining logic

5. **DataFactory** (1 min)
   - [ ] Copy Template 5
   - [ ] Generate realistic data

6. **Tests** (5 min)
   - [ ] Copy Template 6
   - [ ] Write test methods

7. **Schema** (2 min)
   - [ ] Copy Template 7
   - [ ] Define required fields

8. **Config** (1 min)
   - [ ] Update `testng.xml` with Template 8

**Total: ~20 minutes per new API**

---

## Running Your New API Tests

```bash
# Run your test class
mvn clean test -Dtest=YourApiTests

# Run only smoke tests
mvn clean test -Dtest=YourApiTests -Dgroups=smoke

# Run only your chaining tests
mvn clean test -Dtest=YourApiTests#createUpdateFetchTest

# Run all with coverage
mvn clean test -Dtest=YourApiTests coverage

# Generate Allure report
mvn allure:report
mvn allure:serve
```

---

## Common Chaining Patterns to Reuse

### Pattern: Create → Fetch

```java
public Item createThenFetch(Item candidate) {
    Item created = createAndValidate(candidate);
    Item fetched = fetchAndValidate(created.getId());
    AssertionUtils.assertEquals(fetched.getId(), created.getId(), "id");
    return fetched;
}
```

### Pattern: Create → Update → Fetch

```java
public Item createUpdateFetch(Item candidate, String newValue) {
    Item created = createAndValidate(candidate);
    Item toUpdate = created.toBuilder().field(newValue).build();
    ApiResponse response = service.update(created.getId(), toUpdate);
    AssertionUtils.assertStatusCode(response, 200);
    Item fetched = fetchAndValidate(created.getId());
    AssertionUtils.assertEquals(fetched.getField(), newValue, "field");
    return fetched;
}
```

### Pattern: Create Multiple → List

```java
public void createMultipleList(int count) {
    List<Item> created = new ArrayList<>();
    for (int i = 0; i < count; i++) {
        created.add(createAndValidate(YourDataFactory.random()));
    }
    ApiResponse response = service.list(1, 10);
    AssertionUtils.assertStatusCode(response, 200);
    List<Item> listed = response.asList(Item.class);
    AssertionUtils.assertTrue(listed.size() >= count, "list size");
}
```

### Pattern: Search/Filter → Fetch

```java
public Item searchThenFetch(String query) {
    ApiResponse response = service.search(query);
    AssertionUtils.assertStatusCode(response, 200);
    List<Item> results = response.asList(Item.class);
    AssertionUtils.assertFalse(results.isEmpty(), "search results");
    Item first = results.get(0);
    return fetchAndValidate(first.getId());
}
```
