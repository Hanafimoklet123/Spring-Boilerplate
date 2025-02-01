package com.boilerplate.spring_boot.db.migration.flyway.appconf;

import java.util.Map;

@SuppressWarnings("PMD.SimplifiedTernary")
public class TestConfiguration extends ApplicationConfiguration {

    private Map<String, String> testConfig;
    public TestConfiguration(Map<String, String> testConfig) {this.testConfig = testConfig;}

    @Override
    public Object getValue(String name) { return this.testConfig.get(name); }
}
