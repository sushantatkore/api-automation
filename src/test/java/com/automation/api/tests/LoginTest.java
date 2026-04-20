package com.automation.api.tests;

import com.automation.api.core.ApiResponse;
import com.automation.api.datafactory.LoginTestDataFactory;
import com.automation.api.services.LoginService;
import com.automation.api.utils.AssertionUtils;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    private final LoginService loginService = new LoginService();

    @Test(groups = { "smoke" })
    public void login_happyPath() {
        ApiResponse response = loginService.login(
                LoginTestDataFactory.getUsername(),
                LoginTestDataFactory.getPassword());
        AssertionUtils.assertStatusCode(response, 200);
        System.out.println("Response: " + response.body());
    }
}
