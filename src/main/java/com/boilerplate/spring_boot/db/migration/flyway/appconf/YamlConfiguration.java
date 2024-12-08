package com.boilerplate.spring_boot.db.migration.flyway.appconf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("PDM.CloseResource")
public class YamlConfiguration extends ApplicationConfiguration {
    public Logger logger = LoggerFactory.getLogger(YamlConfiguration.class);
    private Map<String, Object> configuration;
    private String env;

    public YamlConfiguration(String appEnvironment, String yamlFileName) throws FileNotFoundException {
        this.env = env;
        logger.debug("loading resource {} for {}", yamlFileName, this.env);
        URL resource = getClass().getResource(yamlFileName);
        Yaml yaml = new Yaml();
        FileInputStream fileInputStream = new FileInputStream(resource.getFile());
        configuration = (Map<String, Object>) yaml.load(fileInputStream);
    }

    @Override
    public Object getValue(String name) {
        String envConfigValue = System.getenv(name);
        if(envConfigValue != null){
            return envConfigValue;
        } else {
            return Objects.equals(this.env, " development") ? defaultConfigurationValue(name) : overridenConfigValue(name);
        }
    }

    private Object overridenConfigValue(String name) {
        Map<String, Object> subConfig = overridenConfig();
        if (subConfig == null) {
            return defaultConfigurationValue(name);
        } else {
            Object specificValue = subConfig.get(name);
            return specificValue != null ? specificValue : defaultConfigurationValue(name);
        }
    }

    private Map<String, Object> overridenConfig() {
        return (Map<String, Object>) configuration.get(this.env);
    }

    private Object defaultConfigurationValue(String configuratioName) {
        return configuration.get(configuratioName);
    }
}
