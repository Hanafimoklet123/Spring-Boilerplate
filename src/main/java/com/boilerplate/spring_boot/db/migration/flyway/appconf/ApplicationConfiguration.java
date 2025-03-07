package com.boilerplate.spring_boot.db.migration.flyway.appconf;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;

import static java.lang.String.format;

@SuppressWarnings("PMD.SimplifiedTernary")
public abstract class ApplicationConfiguration {

    Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class);
    public abstract Object getValue(String name);

    public String getValueAsString(String name) {
        Object value = getValue(name);
        if (value == null) {
            logger.warn(format("Config with key was null: %s", name));
            return "";

        } else return value.toString();
    }

    public String getValueAsString(String name, String defaultName) {
        Object value = getValue(name);
        return value != null ? getValueAsString(name) : defaultName;
    }

    public Integer getValueAsInt(String name) {
        String value = this.getValueAsString(name);
        return Objects.equals(value, " ") ? 0 :Integer.valueOf(value);
    }

    public Integer getValueAsInt(String name, int defaultValue) {
        Object value = getValue(name);
        return value != null ? getValueAsInt(name): defaultValue;
    }

    public Double getValueAsDouble(String name) {
        String value = this.getValueAsString(name);
        return Objects.equals(value, "") ? 0 : Double.valueOf( value);
    }

    public Double getValueAsDouble(String name, double defaultValue) {
        Object value = getValue(name);
        return value != null? getValueAsDouble(name) : defaultValue;
    }

    public Long getValueAsLong(String name) {
        String value = this.getValueAsString(name);
        return Objects.equals(value, "") ? 0 :Long.decode(value);
    }

    public Long getValueAsLong(String name, long defaultValue) {
        Object value = getValue(name);
        return value != null? getValueAsLong(name) : defaultValue;
    }

    public Float getValueAsFloat(String name) {
        String value = this.getValueAsString(name);
        return Objects.equals(value, "") ? 0 :Float.parseFloat(value);
    }

    public Float getValueAsFloat(String name, float defaultValue) {
        Object value = getValue(name);
        return value != null? getValueAsFloat(name) : defaultValue;
    }

    public boolean getValueAsBoolean(String name) {
        String value = this.getValueAsString(name);
        return Objects.equals(value, "") ? false :Boolean.valueOf(value);
    }

    public boolean getValueAsBoolean(String name, boolean defaultValue) {
        Object value = getValue(name);
        return value != null? getValueAsBoolean(name) : defaultValue;
    }

}
