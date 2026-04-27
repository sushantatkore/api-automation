package com.automation.api.listeners;

import com.automation.api.utils.CorrelationIdUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG listener that wires per-test correlation IDs and emits clean lifecycle logs.
 */
public class TestListener implements ITestListener {

    private static final Logger LOGGER = LogManager.getLogger(TestListener.class);

    @Override
    public void onStart(ITestContext context) {
        LOGGER.info("===== Suite START: {} =====", context.getName());
    }

    @Override
    public void onFinish(ITestContext context) {
        LOGGER.info("===== Suite FINISH: {} | passed={} failed={} skipped={} =====",
                context.getName(),
                context.getPassedTests().size(),
                context.getFailedTests().size(),
                context.getSkippedTests().size());
    }

    @Override
    public void onTestStart(ITestResult result) {
        String correlationId = CorrelationIdUtil.generate();
        LOGGER.info(">>> TEST START: {}.{} | correlationId={}",
                result.getTestClass().getRealClass().getSimpleName(),
                result.getMethod().getMethodName(),
                correlationId);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        LOGGER.info("<<< TEST PASS: {} ({}ms)",
                result.getMethod().getMethodName(),
                result.getEndMillis() - result.getStartMillis());
        CorrelationIdUtil.clear();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        LOGGER.error("<<< TEST FAIL: {} | reason={}",
                result.getMethod().getMethodName(),
                result.getThrowable() != null ? result.getThrowable().getMessage() : "unknown");
        CorrelationIdUtil.clear();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        LOGGER.warn("<<< TEST SKIP: {}", result.getMethod().getMethodName());
        CorrelationIdUtil.clear();
    }
}
