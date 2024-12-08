package com.boilerplate.spring_boot.commons.instrumentation;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static net.logstash.logback.marker.Markers.append;


@Slf4j
@Component
@SuppressWarnings({"PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals"})
public class MetricService {

    private final MeterRegistryConfig meterRegistryConfig;

    private Map<String, AtomicInteger> gauges;

    public MetricService(MeterRegistryConfig meterRegistryConfig, Map<String, AtomicInteger> gauges) {
        this.meterRegistryConfig = meterRegistryConfig;
        this.gauges = gauges;
    }

    public void incrementCounter(String name, String... tags) {
        MeterRegistry metricsRegistry = meterRegistryConfig.getMeterRegistry();

        if (!meterRegistryConfig.getStatsdPrefix().endsWith(".")) {
            name = meterRegistryConfig.getStatsdPrefix() + "." + name;
        } else {
            name = meterRegistryConfig.getStatsdPrefix() + name;
        }

        log.debug(append("FLOW", name).
                        and(append("METADATA", tags)),
                "increment counter.");

        metricsRegistry.counter(name.toLowerCase(Locale.ROOT), tags).increment();
    }

    public void recordTiming(String name, long timeElapsed, String... tags) {
        MeterRegistry metricsRegistry = meterRegistryConfig.getMeterRegistry();

        log.debug(append("FLOW", name).
                        and(append("METADATA", tags)),
                "record timing");

        metricsRegistry.timer(buildNameWithStatsdPrefix(name), tags).record(Duration.ofMillis(timeElapsed));
    }

}
