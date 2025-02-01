package com.boilerplate.spring_boot.config;

import com.google.gson.Gson;
import com.boilerplate.spring_boot.commons.interceptor.MaskingDataInterceptor;
import com.boilerplate.spring_boot.convertors.JsonArrayToPGobject;
import com.boilerplate.spring_boot.convertors.JsonObjectToPGobject;
import com.boilerplate.spring_boot.convertors.PGObjectToJsonArray;
import com.boilerplate.spring_boot.convertors.PGObjectToJsonObject;
import com.boilerplate.spring_boot.db.migration.flyway.appconf.ApplicationConfiguration;
import com.boilerplate.spring_boot.db.migration.flyway.appconf.Figaro;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionManager;

import javax.sql.DataSource;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Set;

@SpringBootConfiguration
@EnableJdbcRepositories("com.boilerplate.spring_boot.repositories")
@EnableAspectJAutoProxy
@EnableJdbcAuditing
public class ConfigurationFactory extends AbstractJdbcConfiguration {

    private ApplicationConfiguration appConfig;

    public ConfigurationFactory() {
        this.appConfig = Figaro.configure(requiredConfigurations());
    }

    @Bean
    public ApplicationConfiguration getAppConfig() {
        return this.appConfig;
    }

    @Bean
    NamedParameterJdbcOperations namedParameterJdbcOperations(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    TransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Override
    public JdbcCustomConversions jdbcCustomConversions() {
        return new JdbcCustomConversions(Arrays.asList(
                new PGObjectToJsonObject(),
                new JsonObjectToPGobject(),
                new PGObjectToJsonArray(),
                new JsonArrayToPGobject()
        ));
    }
    private static Set<String> requiredConfigurations() {
        return Set.of(
            "APP_ENVIRONMENT",
            "APP_NAME",
            "APP_PORT",
            "DB_HOST",
            "DB_PORT",
            "DB_NAME",
            "DB_USERNAME",
            "DB_PASSWORD"
        );
    }

    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }

    @Bean
    public RandomStringUtils randomStringUtils() {
        return RandomStringUtils.secure();
    }

    @Bean
    public MaskingDataInterceptor maskingDataInterceptor(
            @Value("${APP_NAME}") String appName,
            @Value("${MASKING_DATA_ENABLED}") Boolean maskingDataEnabled,
            Gson gson
    ) {
        return new MaskingDataInterceptor(appName, maskingDataEnabled, gson);
    }
}
