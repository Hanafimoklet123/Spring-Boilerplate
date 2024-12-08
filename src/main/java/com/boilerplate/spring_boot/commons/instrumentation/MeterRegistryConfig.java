package com.boilerplate.spring_boot.commons.instrumentation;


import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.validate.ValidationException;
import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdFlavor;
import io.micrometer.statsd.StatsdMeterRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MeterRegistryConfig {

    @Getter private String statsdHost;
    @Getter private int statsdPort;
    @Getter private String statsdPrefix;
    @Getter private MeterRegistry meterRegistry;

    @Autowired
    public MeterRegistryConfig (
            @Value("${STATSD_HOST}") String statsdHost,
            @Value("${STATSD_PORT}") int statsdPort,
            @Value("${STATSD_PREFIX}") String statsdPrefix
    ) {
        this.statsdHost = statsdHost;
        this.statsdPort = statsdPort;
        this.statsdPrefix = statsdPrefix;
        this.meterRegistry = meterRegistryConfig();
    }


    private MeterRegistry meterRegistryConfig() {

        StatsdConfig config = new StatsdConfig() {
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public void requireValid() throws ValidationException {
                StatsdConfig.super.requireValid();
            }

            @Override
            public String prefix() {
                return StatsdConfig.super.prefix();
            }

            @Override
            public StatsdFlavor flavor() {
                return StatsdConfig.super.flavor();
            }

            @Override
            public String host() {
                return StatsdConfig.super.host();
            }

            @Override
            public int port() {
                return StatsdConfig.super.port();
            }
        };

        return StatsdMeterRegistry.builder(config)
                .clock(Clock.SYSTEM)
                .build();
    }


}
