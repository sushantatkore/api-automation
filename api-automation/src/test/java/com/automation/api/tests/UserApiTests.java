package com.automation.api.tests;

import com.automation.api.datafactory.UserDataFactory;
import com.automation.api.models.User;
import com.automation.api.workflows.UserWorkflow;
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

@Epic("Users API")
@Feature("User lifecycle")
public class UserApiTests extends BaseTest {

    private final UserWorkflow userWorkflow = new UserWorkflow();
    private final List<String> createdUserIds = new ArrayList<>();

    @BeforeMethod(alwaysRun = true)
    public void resetCreatedIds() {
        createdUserIds.clear();
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup() {
        for (String id : createdUserIds) {
            userWorkflow.cleanupUser(id);
        }
    }

    @Test(groups = {"smoke", "regression"}, description = "Create user returns 201 with valid schema")
    @Story("Create user")
    @Severity(SeverityLevel.BLOCKER)
    @Description("POST /users with a valid payload should return 201 and a body that matches the user schema.")
    public void createUser_happyPath() {
        User created = userWorkflow.createAndValidateRandomUser();
        createdUserIds.add(created.getId());
    }

    @Test(groups = {"smoke", "regression"}, description = "Create -> fetch chain returns the same user")
    @Story("Create then fetch")
    @Severity(SeverityLevel.CRITICAL)
    public void createThenFetchUser_chain() {
        User fetched = userWorkflow.createThenFetchUser();
        createdUserIds.add(fetched.getId());
    }

    @Test(groups = {"regression"}, description = "Update user email propagates")
    @Story("Update user")
    @Severity(SeverityLevel.NORMAL)
    public void updateUserEmail() {
        User created = userWorkflow.createAndValidateRandomUser();
        createdUserIds.add(created.getId());
        User updated = userWorkflow.updateUserEmail(created.getId(), "updated." + created.getEmail());
        org.testng.Assert.assertTrue(updated.getEmail().startsWith("updated."), "email update not applied");
    }

    @Test(groups = {"regression"}, description = "Fetch random created user matches generator output")
    @Story("Fetch user")
    @Severity(SeverityLevel.NORMAL)
    public void fetchCreatedUser_valuesRoundTrip() {
        User input = UserDataFactory.randomUser();
        User created = userWorkflow.createAndValidateUser(input);
        createdUserIds.add(created.getId());

        User fetched = userWorkflow.fetchAndValidateUser(created.getId());
        org.testng.Assert.assertEquals(fetched.getEmail(), input.getEmail(), "email mismatch after round trip");
        org.testng.Assert.assertEquals(fetched.getUsername(), input.getUsername(), "username mismatch after round trip");
    }
}
