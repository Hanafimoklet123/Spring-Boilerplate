package com.boilerplate.spring_boot.commons.instrumentation;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static com.boilerplate.spring_boot.commons.instrumentation.MetricConstants.*;
import static net.logstash.logback.marker.Markers.append;

@Slf4j
@Component
@SuppressWarnings({"PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals"})
public class MetricService {

    private final MeterRegistryConfig meterRegistryConfig;

    private Map<String, AtomicInteger> gauges;

    @Autowired
    public MetricService(MeterRegistryConfig meterRegistryConfig) {
        this.meterRegistryConfig = meterRegistryConfig;
        this.gauges = new HashMap<>();
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

    public void incrementCounterWithoutServicePrefix(String name, Map<String, String> tags) {

        if (name == null) {
            return;
        }

        MeterRegistry metricsRegistry = meterRegistryConfig.getMeterRegistry();

        String service = meterRegistryConfig.getStatsdPrefix();
        if (service.endsWith(".")) {
            service = service.substring(0, service.length() - 1);
        }
        tags.put(TAG_SERVICE, service);

        log.debug(append("FLOW", name).
                        and(append("METADATA", tags)),
                "increment counter.");

        List<String> tagList = new ArrayList<>();

        for (Map.Entry<String, String> tag: tags.entrySet()) {
            if (tag.getValue() != null) {
                tagList.add(tag.getKey());
                tagList.add(tag.getValue());
            }
        }

        metricsRegistry.counter(name, tagList.toArray(new String[0])).increment();
    }

    public void recordTiming(String name, long timeElapsed, String... tags) {
        MeterRegistry metricsRegistry = meterRegistryConfig.getMeterRegistry();

        log.debug(append("FLOW", name).
                        and(append("METADATA", tags)),
                "record timing");

        metricsRegistry.timer(buildNameWithStatsdPrefix(name), tags).record(Duration.ofMillis(timeElapsed));
    }

    public void recordTimingWithoutServicePrefix(String name, long timeElapsed, Map<String, String> tags) {

        if (name == null) {
            return;
        }

        MeterRegistry metricsRegistry = meterRegistryConfig.getMeterRegistry();

        String service = meterRegistryConfig.getStatsdPrefix();
        if (service.endsWith(".")) {
            service = service.substring(0, service.length() - 1);
        }
        tags.put(TAG_SERVICE, service);

        log.debug(append("FLOW", name).
                        and(append("METADATA", tags)),
                "record timing");

        List<String> tagList = new ArrayList<>();
        for (Map.Entry<String, String> tag : tags.entrySet()) {
            if (tag.getValue() != null) {
                tagList.add(tag.getKey());
                tagList.add(tag.getValue());
            }
        }

        metricsRegistry.timer(name, tagList.toArray(new String[0])).record(Duration.ofMillis(timeElapsed));
    }

    public String buildNameWithStatsdPrefix(String name) {
        if (!meterRegistryConfig.getStatsdPrefix().endsWith(".")) {
            name = meterRegistryConfig.getStatsdPrefix() + "." + name;
        } else {
            name = meterRegistryConfig.getStatsdPrefix() + name;
        }

        return name;
    }

    /**
     * This function increments the counter metric with the MetricRecord POJO as a parameter.
     *
     * Mandatory fields
     * - metricName
     * - metricData
     *  - class
     *  - method
     *  - source
     *  - partner
     *  - onboardingPartner
     *  --> example case, we call ChallengeService@checkUser from ChallengeController@getChallenge
     *  --> inside of ChallengeService@checkUser there is the metric counter, so source would be the function name from the controller level ('getChallenge')
     *
     * @param metricRecord
     */
    public void incrementCounter(MetricRecord metricRecord) {

        MetricRecord.MetricData metricData = validateMetricRecord(metricRecord);
        if (metricData == null) return;

        List<Pair<String, String>> pairMetricRecords = getPairMetricRecords(metricData);

        List<String> tagList = new ArrayList<>();

        for (Pair<String, String> pairMetricRecord: pairMetricRecords) {
            tagList = checkMetricRecord(tagList, pairMetricRecord);
        }

        incrementCounter(metricRecord.getMetricName().toLowerCase(Locale.ROOT), tagList.toArray(new String[0]));
    }

    public void incrementCounter(MetricRecord metricRecord, Map<String, String> tagsMap) {

        MetricRecord.MetricData metricData = validateMetricRecord(metricRecord);
        if (metricData == null) return;

        List<Pair<String, String>> pairMetricRecords = getPairMetricRecords(metricData);

        List<String> tagList = new ArrayList<>();

        for (Pair<String, String> pairMetricRecord: pairMetricRecords) {
            tagList = checkMetricRecord(tagList, pairMetricRecord);
        }

        for (Map.Entry<String, String> entry: tagsMap.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isBlank()) {
                tagList.add(entry.getKey());
                tagList.add(entry.getValue());
            }
        }

        incrementCounter(metricRecord.getMetricName().toLowerCase(Locale.ROOT), tagList.toArray(new String[0]));
    }

    public void setGauge(String gaugeName, Integer value) {
        MeterRegistry metricsRegistry = meterRegistryConfig.getMeterRegistry();

        String name;
        if (!meterRegistryConfig.getStatsdPrefix().endsWith(".")) {
            name = meterRegistryConfig.getStatsdPrefix() + "." + gaugeName;
        } else {
            name = meterRegistryConfig.getStatsdPrefix() + gaugeName;
        }

        log.debug(append("FLOW", name).and(append("VALUE", value)), "set gauge.");

        if (!gauges.containsKey(name)) {
            gauges.put(name, metricsRegistry.gauge(name, new AtomicInteger(value)));
        }

        AtomicInteger gaugeValue = gauges.get(name);
        gaugeValue.set(value);
    }

    private MetricRecord.MetricData validateMetricRecord(MetricRecord metricRecord) {
        if (metricRecord.getMetricName() == null) {
            log.error("Error: metric name required. statsd prefix: {}", meterRegistryConfig.getStatsdPrefix());
            return null;
        }

        MetricRecord.MetricData metricData = metricRecord.getMetricData();

        if (metricData == null) {
            log.error("Error: metric data at least not null. statsd prefix: {}", meterRegistryConfig.getStatsdPrefix());
            return null;
        }
        return metricData;
    }

    private List<Pair<String, String>> getPairMetricRecords(MetricRecord.MetricData metricData) {

        RequestAttributeTag requestAttributeTag = getRequestAttributeTag();

        return List.of(
                Pair.of(TAG_SOURCE, metricData.getSource()),
                Pair.of(TAG_CLASS_NAME, metricData.getClassName()),
                Pair.of(TAG_METHOD_NAME, metricData.getMethodName()),
                Pair.of(TAG_POD_NAME, requestAttributeTag.getPodNameTag())
        );
    }

    protected RequestAttributeTag getRequestAttributeTag() {
        return new RequestAttributeTag("MetricService");
    }

    private List<String> checkMetricRecord(List<String> tagList, Pair<String, String> pairMetricRecord) {

        if (pairMetricRecord.getRight() != null && !pairMetricRecord.getRight().isBlank()) {
            tagList.add(pairMetricRecord.getLeft());
            tagList.add(pairMetricRecord.getRight());
        }

        return tagList;
    }
}
