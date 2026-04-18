package com.automation.api.listeners;

import com.automation.api.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Test-level retry analyzer. Complements the request-level RetryHandler by
 * re-running a full test when a flaky intermittent failure is suspected.
 * Disabled by default (test.retry.count=0).
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger LOGGER = LogManager.getLogger(RetryAnalyzer.class);
    private final int maxRetries = ConfigManager.getInstance().getInt("test.retry.count", 0);
    private int attempt = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (attempt < maxRetries) {
            attempt++;
            LOGGER.warn("Retrying test {} (attempt {}/{})",
                    result.getMethod().getMethodName(), attempt, maxRetries);
            return true;
        }
        return false;
    }
}
