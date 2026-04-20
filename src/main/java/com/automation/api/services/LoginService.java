package com.automation.api.services;

import com.automation.api.core.ApiResponse;
import com.automation.api.core.RequestBuilder;
import io.restassured.response.Response;
import java.util.Map;

public class LoginService {

    public ApiResponse login(String username, String password) {
        Response raw = RequestBuilder.createUnauthenticated()
                .build()
                .body(Map.of("username", username, "password", password))
                .post("/auth/login"); // <-- your actual login path
        return new ApiResponse(raw);
    }
}
