package com.automation.api.services;

import com.automation.api.constants.Endpoints;
import com.automation.api.core.ApiResponse;
import com.automation.api.core.BaseService;
import com.automation.api.models.User;

import java.util.Map;

/**
 * Thin service wrapper for the /users resource. No assertions, no business logic — pure I/O.
 */
public class UserService extends BaseService {

    public ApiResponse createUser(User user) {
        return post(Endpoints.Users.BASE, user);
    }

    public ApiResponse getUser(String id) {
        return get(Endpoints.Users.BY_ID, Map.of(), Map.of("id", id));
    }

    public ApiResponse listUsers(int page, int pageSize) {
        return get(Endpoints.Users.BASE, Map.of("page", page, "pageSize", pageSize));
    }

    public ApiResponse updateUser(String id, User user) {
        return put(Endpoints.Users.BY_ID, user, Map.of("id", id));
    }

    public ApiResponse patchUser(String id, Map<String, Object> partial) {
        return patch(Endpoints.Users.BY_ID, partial, Map.of("id", id));
    }

    public ApiResponse deleteUser(String id) {
        return delete(Endpoints.Users.BY_ID, Map.of("id", id));
    }
}
