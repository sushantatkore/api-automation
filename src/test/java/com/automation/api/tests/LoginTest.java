package com.automation.api.tests;

import com.automation.api.core.ApiResponse;
import com.automation.api.services.LoginService;
import com.automation.api.utils.AssertionUtils;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    private final LoginService loginService = new LoginService();

    @Test(groups = {"smoke", "regression"})
    public void login_happyPath() {
        ApiResponse response = loginService.login("Sushant.atkore+test1@webmobinfo.ch" + // 
                                                , "Admin@1234");
        AssertionUtils.assertStatusCode(response, 200);
        System.out.println("Response: " + response.body());
    }
}
