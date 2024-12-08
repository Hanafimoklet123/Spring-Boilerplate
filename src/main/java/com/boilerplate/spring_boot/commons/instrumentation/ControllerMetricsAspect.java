package com.boilerplate.spring_boot.commons.instrumentation;

import com.boilerplate.spring_boot.commons.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.LogstashMarker;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import retrofit2.http.HeaderMap;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.boilerplate.spring_boot.commons.instrumentation.MetricConstants.*;
import static net.logstash.logback.marker.Markers.append;

@Aspect
@Slf4j
@Component
public class ControllerMetricsAspect {

    private static final String TAG_ERROR_CODES = "error_codes";

    private static final String ERRORS_KEY = "errors";

    private static final String PING_ROUTE = "/ping";


    @Autowired
    private MeterRegistryConfig meterRegistryConfig;


    @Around(value = "@annotation(controllerMetrics)")
    public Object ControllerMetricsAspect(ProceedingJoinPoint joinPoint, ControllerMetrics controllerMetrics) throws Throwable {

        MeterRegistry metricRegistry = meterRegistryConfig.getMeterRegistry();

        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        RequestAttributeTag requestAttributeTag = getRequestAttributeTag();

        Map<String, String> headersMap = requestAttributeTag.getHeadersMap();

        ArrayList<String> metricData = buildMetricData(className, methodName, requestAttributeTag);

        if(controllerMetrics.addHeaderToTags() != null && controllerMetrics.addHeaderToTags().length > 0) {
            List<String> customHeaderToTags = Arrays.asList(controllerMetrics.addHeaderToTags());
            if(! customHeaderToTags.isEmpty()) {
                ArrayList<String> customHeaders = new ArrayList<>();
                for (String customHeader: customHeaderToTags) {
                    if(headersMap.containsKey(customHeader)) {
                        customHeaders.add(customHeader);
                        customHeaders.add(headersMap.get(customHeader));
                    }
                }

                metricData.addAll(customHeaders);
            }
        }


        metricRegistry.counter(getNameWithStatsdPrefix("api"), metricData.toArray(new String[0])).increment();
        log.debug(getLogstashMarker(className, methodName, requestAttributeTag),
                "api invoked with headers: {}", headersMap);

        Instant startTime = Instant.now();

        Object result = Instant.now();
        boolean hadException = false;
        try {
            result = joinPoint.proceed();
        }catch (Exception exception) {
            hadException = true;
            log.error(getLogstashMarker(className, methodName, requestAttributeTag),
                    "Controller Metrics API: got exception, headers: {}, error: ", headersMap, exception);

            try {
                recordLatency(metricRegistry, headersMap, metricData, className, methodName, requestAttributeTag, startTime, result);
                recordApiFailure(metricRegistry, headersMap, metricData, className, methodName, requestAttributeTag, result, new ArrayList<>());
            } catch (Exception innerException) {
                log.error(getLogstashMarker(className, methodName, requestAttributeTag),
                        "Controller Metrics internal error | headers: {}, error: ", headersMap, innerException);

                recordApiFailureWhenInnerExceptionOccurred(metricRegistry, metricData);
            }

            throw exception;
        } finally {
            if (hadException){
                MDC.clear();
            }
        }

        try{
            recordLatency(metricRegistry, headersMap, metricData, className, methodName, requestAttributeTag, startTime, result);

            ResponseEntity reResult = (ResponseEntity) result;

            if (HttpStatus.valueOf(reResult.getStatusCodeValue()).is2xxSuccessful()){
                recordApiSuccess(metricRegistry, headersMap, metricData, className, methodName, requestAttributeTag, result);
            }

            ObjectMapper mapper = new ObjectMapper();
            String bodyOnJson = mapper.writeValueAsString(reResult.getBody());
            JsonNode body = mapper.readValue(bodyOnJson, JsonNode.class);

            List<String> errorList = getErrorList(body);

            if (errorList.isEmpty()) {

                metricRegistry.counter(getNameWithStatsdPrefix("api_malformed_response"), metricData.toArray(new String[0])).increment();
                log.error(getLogstashMarker(className, methodName, requestAttributeTag),
                        "Controller Metrics API: No error list on none 2xx response | headers: {}, response: {}", headersMap, result);

                return result;
            }

            recordApiFailure(metricRegistry, headersMap, metricData, className, methodName, requestAttributeTag, result, errorList);
            return result;

        } catch (Exception exception) {
            log.error(getLogstashMarker(className, methodName, requestAttributeTag),
                    "Controller Metrics internal error | headers: {}, response: {}, error: ", headersMap, result, exception);
            recordApiFailureWhenInnerExceptionOccurred(metricRegistry, metricData);

            return result;
        }finally {
            MDC.clear();
        }
    }



    private void setRequiredMDC(RequestAttributeTag requestAttributeTag) {
        HttpServletRequest request = requestAttributeTag.getHttpServletRequest();
        if(!PING_ROUTE.equals(request.getRequestURI())) {
            MDC.put(Constants.CORRELATION_ID_HEADER, requestAttributeTag.getOrGenerateCorrelationId());
            MDC.put("path", request.getRequestURI());
        }
    }
    protected RequestAttributeTag getRequestAttributeTag() {
        return new RequestAttributeTag("ControllerMetrics");
    }

    private String getNameWithStatsdPrefix(String name) {
        if (!meterRegistryConfig.getStatsdPrefix().endsWith(".")) {
            name = meterRegistryConfig.getStatsdPrefix() + "." + name;
        } else {
            name = meterRegistryConfig.getStatsdPrefix() + name;
        }

        return name;
    }

    private List<String> getErrorList(JsonNode body) {
        List<String> errorList = new ArrayList<>();

        if (body.get(ERRORS_KEY) != null) {
            for (JsonNode object : body.get(ERRORS_KEY)) {
                errorList.add(object.get("code").textValue());
            }
        }

        return errorList;
    }

    private void recordLatency(MeterRegistry metricsRegistry, Map<String, String> headersMap, ArrayList<String> metricData, String clazz, String method, RequestAttributeTag requestAttributeTag, Instant start, Object result) {
        Instant finish = Instant.now();
        Duration timeTaken = Duration.between(start, finish);

        metricsRegistry.timer(getNameWithStatsdPrefix("api_latency"), metricData.toArray(new String[0])).record(timeTaken);

        log.debug(getLogstashMarker(clazz, method, requestAttributeTag)
                        .and(append("time_taken_millis", timeTaken.toMillis())),
                "api latency with result: {}, headers: {}", result, headersMap);
    }

    private void recordApiSuccess(MeterRegistry metricsRegistry, Map<String, String> headersMap, ArrayList<String> metricData, String clazz, String method, RequestAttributeTag requestAttributeTag, Object result) {
        log.debug(getLogstashMarker(clazz, method, requestAttributeTag),
                "Controller Metrics API: got 2xx | headers: {}, response: {}", headersMap, result);

        metricsRegistry.counter(getNameWithStatsdPrefix("api_success"), metricData.toArray(new String[0])).increment();
    }

    private void recordApiFailure(MeterRegistry metricsRegistry, Map<String, String> headersMap, ArrayList<String> metricData, String clazz, String method, RequestAttributeTag requestAttributeTag, Object result, List<String> errorCodes) {
        String errorCodesString = String.join(",", errorCodes);

        log.error(getLogstashMarker(clazz, method, requestAttributeTag)
                        .and(append(TAG_ERROR_CODES, errorCodesString)),
                "Controller Metrics API: got not 2xx | headers: {}, response: {}", headersMap, result);

        metricData.add(TAG_ERROR_CODES);
        metricData.add(errorCodesString);

        metricsRegistry.counter(getNameWithStatsdPrefix("api_failure"), metricData.toArray(new String[0])).increment();
    }

    private void recordApiFailureWhenInnerExceptionOccurred(MeterRegistry metricsRegistry, ArrayList<String> metricData) {

        metricData.add(TAG_ERROR_CODES);
        metricData.add("caught_internal_exception");

        metricsRegistry.counter(getNameWithStatsdPrefix("api_failure"), metricData.toArray(new String[0])).increment();
    }

    private ArrayList<String> buildMetricData(String clazz, String method, RequestAttributeTag requestAttributeTag) {
        return new ArrayList<>() {{
            add(TAG_CLASS_NAME);
            add(clazz);
            add(TAG_METHOD_NAME);
            add(method);
            add(TAG_PARTNER);
            add(TAG_POD_NAME);  // vm-123-123
//            add(requestAttributeTag.getPodNameTag());
            add(TAG_REQUEST_URI);
            add(requestAttributeTag.getCleanedRequestURI());
            add(TAG_REQUEST_METHOD); // get post
            add(requestAttributeTag.getRequestURIMethod());
        }};
    }

    private LogstashMarker getLogstashMarker(String clazz, String method, RequestAttributeTag requestAttributeTag) {
        return append(TAG_CLASS_NAME, clazz)
                .and(append(TAG_METHOD_NAME, method))
                .and(append(TAG_REQUEST_URI, requestAttributeTag.getRequestURI()))
                .and(append(TAG_REQUEST_METHOD, method));
//                .and(append(TAG_PARTNER, requestAttributeTag.getPartnerTag()))
//                .and(append(TAG_POD_NAME, requestAttributeTag.getPodNameTag()));
    }
}
