package com.automation.api.workflows;

import com.automation.api.core.ApiResponse;
import com.automation.api.datafactory.UserDataFactory;
import com.automation.api.models.User;
import com.automation.api.services.UserService;
import com.automation.api.utils.AssertionUtils;
import com.automation.api.utils.SchemaValidator;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Business-level workflows that chain multiple service calls. Tests invoke these
 * instead of composing services themselves.
 */
public class UserWorkflow {

    private static final Logger LOGGER = LogManager.getLogger(UserWorkflow.class);
    private static final String USER_SCHEMA = "schemas/user-schema.json";

    private final UserService userService = new UserService();

    @Step("Create a new random user and validate response")
    public User createAndValidateRandomUser() {
        User candidate = UserDataFactory.randomUser();
        return createAndValidateUser(candidate);
    }

    @Step("Create user: {candidate.username}")
    public User createAndValidateUser(User candidate) {
        ApiResponse response = userService.createUser(candidate);
        AssertionUtils.assertStatusCode(response, 201);
        AssertionUtils.assertResponseTimeUnder(response, 3_000L);
        SchemaValidator.validate(response, USER_SCHEMA);

        User created = response.as(User.class);
        AssertionUtils.assertNotBlank(created.getId(), "created user id");
        AssertionUtils.assertEquals(created.getEmail(), candidate.getEmail(), "user.email");
        LOGGER.info("User created with id={}", created.getId());
        return created;
    }

    @Step("Fetch user: {id}")
    public User fetchAndValidateUser(String id) {
        ApiResponse response = userService.getUser(id);
        AssertionUtils.assertStatusCode(response, 200);
        SchemaValidator.validate(response, USER_SCHEMA);
        return response.as(User.class);
    }

    @Step("Create then fetch user (chain)")
    public User createThenFetchUser() {
        User created = createAndValidateRandomUser();
        User fetched = fetchAndValidateUser(created.getId());
        AssertionUtils.assertEquals(fetched.getId(), created.getId(), "user.id");
        AssertionUtils.assertEquals(fetched.getEmail(), created.getEmail(), "user.email");
        return fetched;
    }

    @Step("Update user email for id {id}")
    public User updateUserEmail(String id, String newEmail) {
        User current = fetchAndValidateUser(id);
        User updated = current.toBuilder().email(newEmail).build();
        ApiResponse response = userService.updateUser(id, updated);
        AssertionUtils.assertStatusCode(response, 200);
        User persisted = response.as(User.class);
        AssertionUtils.assertEquals(persisted.getEmail(), newEmail, "user.email");
        return persisted;
    }

    @Step("Cleanup user: {id}")
    public void cleanupUser(String id) {
        if (id == null || id.isBlank()) {
            LOGGER.warn("Skip cleanup — id is null/blank");
            return;
        }
        try {
            ApiResponse response = userService.deleteUser(id);
            if (response.statusCode() != 200 && response.statusCode() != 204 && response.statusCode() != 404) {
                LOGGER.warn("Cleanup DELETE /users/{} returned unexpected status {}", id, response.statusCode());
            } else {
                LOGGER.info("Cleanup DELETE /users/{} -> {}", id, response.statusCode());
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Cleanup for user {} failed: {}", id, e.getMessage());
        }
    }
}
