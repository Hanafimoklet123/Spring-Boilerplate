package com.boilerplate.spring_boot.db.migration.flyway.appconf;

import java.util.Set;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class MissingRequiredConfigurationException extends RuntimeException{

    public MissingRequiredConfigurationException(Set<String> missingConfigurationNames) {
        super("Missing required configurations: " + missingConfigurationNames);
    }
}
