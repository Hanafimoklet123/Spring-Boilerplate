package com.boilerplate.spring_boot.db.migration.flyway;

import com.boilerplate.spring_boot.db.migration.flyway.appconf.ApplicationConfiguration;
import com.boilerplate.spring_boot.db.migration.flyway.appconf.Figaro;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.internal.database.postgresql.PostgreSQLConfigurationExtension;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

public class Migration {

    public static void main(String... args){
        Set<String> requiredConfig = new HashSet<>(asList(
                "DB_HOST",
                "DB_PORT",
                "DB_NAME",
                "DB_USERNAME",
                "DB_PASSWORD"
        ));

        ApplicationConfiguration config = Figaro.configure(requiredConfig);
        FluentConfiguration configuration = Flyway.configure()
                .dataSource(
                        String.format(
                                "jdbc:postgresql://%s:%s/%s",
                                config.getValueAsString("DB_HOST"),
                                config.getValueAsString("DB_PORT"),
                                config.getValueAsString("DB_NAME")
                        ),
                        config.getValueAsString("DB_USERNAME"),
                        config.getValueAsString("DB_NAME"))
                .baselineOnMigrate(true)
                .validateMigrationNaming(true)
                .validateOnMigrate(true);

        PostgreSQLConfigurationExtension configurationExtension = configuration.getPluginRegister().getPlugin(PostgreSQLConfigurationExtension.class);
        configurationExtension.setTransactionalLock(false);

        Flyway flyway = new Flyway(configuration);
        flyway.migrate();
    }
}
