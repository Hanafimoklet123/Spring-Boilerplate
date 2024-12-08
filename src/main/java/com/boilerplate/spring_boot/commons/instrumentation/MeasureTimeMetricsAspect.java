package com.boilerplate.spring_boot.commons.instrumentation;


import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

import static com.boilerplate.spring_boot.commons.instrumentation.MetricConstants.*;
import static net.logstash.logback.marker.Markers.append;

@Aspect
@Slf4j
@Component
public class MeasureTimeMetricsAspect {

    @Autowired
    private MeterRegistryConfig meterRegistryConfig;

    public Object MeasureTimeMetricHandler(ProceedingJoinPoint joinPoint) throws Throwable {

        MeterRegistry metricsRegistry = meterRegistryConfig.getMeterRegistry();

        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.debug("start measure time metrics on class: {}, method: {}", className, methodName);

        RequestAttributeTag requestAttributeTag = getRequestAttributeTag();

        Instant start = Instant.now();
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Exception exception) {
            log.error("method return exception while measuring time metrics, error: {} on class: {}, method: {}", exception, className, methodName);
            recordDuration(metricsRegistry, className, methodName, start, requestAttributeTag);
            throw exception;
        }

        recordDuration(metricsRegistry, className, methodName, start, requestAttributeTag);

        log.debug("finish measure time metric on class: {}, method: {}, result: {}", className, methodName, result);

        return result;
    }

    protected RequestAttributeTag getRequestAttributeTag() {
        return new RequestAttributeTag("MeasureTimeMetrics");
    }

    private String getNameWithStatsdPrefix() {
        if (!meterRegistryConfig.getStatsdPrefix().endsWith(".")) {
            return String.format(
                    "%s.duration",
                    meterRegistryConfig.getStatsdPrefix());
        }

        return String.format(
                "%sduration",
                meterRegistryConfig.getStatsdPrefix());
    }

    private void recordDuration(MeterRegistry metricsRegistry, String className, String methodName, Instant start, RequestAttributeTag requestAttributeTag) {
        try{
            Instant finish = Instant.now();

            Duration timeTaken = Duration.between(start, finish);
            metricsRegistry.timer(getNameWithStatsdPrefix(),
                    TAG_CLASS_NAME, className,
                    TAG_METHOD_NAME, methodName
                    ).record(timeTaken);

            log.debug(append(TAG_CLASS_NAME, className)
                            .and(append(TAG_METHOD_NAME, methodName))
                            .and(append("time_taken_millis", timeTaken.toMillis())),
                    "measure time metric duration");
        } catch (Exception innerException) {
            log.debug("Exception occurred while recording duration with error: {} on class: {}, method: {}", innerException, className, methodName);
        }
    }

}
