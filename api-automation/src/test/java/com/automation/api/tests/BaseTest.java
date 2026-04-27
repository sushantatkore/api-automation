package com.automation.api.tests;

import com.automation.api.config.ConfigManager;
import com.automation.api.utils.CorrelationIdUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.lang.reflect.Method;

/**
 * Minimal base test. No HTTP logic here — tests should delegate entirely to workflows.
 */
public abstract class BaseTest {

    protected static final Logger LOGGER = LogManager.getLogger(BaseTest.class);

    @BeforeSuite(alwaysRun = true)
    public void suiteSetup() {
        ConfigManager config = ConfigManager.getInstance();
        LOGGER.info("Framework bootstrap | env={} baseUrl={}",
                config.getEnvironment(), config.getBaseUrl());
    }

    @BeforeMethod(alwaysRun = true)
    public void beforeEach(Method method) {
        String id = CorrelationIdUtil.generate();
        LOGGER.info("--- Before {} | correlationId={}", method.getName(), id);
    }

    @AfterMethod(alwaysRun = true)
    public void afterEach(Method method) {
        LOGGER.info("--- After {} ---", method.getName());
        CorrelationIdUtil.clear();
    }
}
