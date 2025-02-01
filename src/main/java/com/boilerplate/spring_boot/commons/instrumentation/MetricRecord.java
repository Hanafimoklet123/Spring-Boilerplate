package com.boilerplate.spring_boot.commons.instrumentation;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MetricRecord {

    String metricName;
    MetricData metricData;

    @Data
    @Builder
    public static class MetricData {
        String source;
        String className;
        String methodName;
        String onboardingPartner;
    }
}
