package com.automation.api.datafactory;

import com.automation.api.config.ConfigManager;

public class LoginTestDataFactory {
    public static String getUsername() {
        return ConfigManager.getInstance().get("test.login.username");
    }

    public static String getPassword() {
        return ConfigManager.getInstance().get("test.login.password");
    }
}
