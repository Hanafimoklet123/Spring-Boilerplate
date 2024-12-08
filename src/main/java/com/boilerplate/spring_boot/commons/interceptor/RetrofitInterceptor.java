package com.boilerplate.spring_boot.commons.interceptor;

import com.boilerplate.spring_boot.commons.Constants;
import com.boilerplate.spring_boot.commons.instrumentation.MetricService;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.AntPathMatcher;
import retrofit2.Invocation;

import java.io.IOException;
import java.util.*;

@Slf4j
@Lazy
@SuppressWarnings("PMD.CloseResource")
public class RetrofitInterceptor implements Interceptor {

    private final MetricService metricService;

    private final boolean isOkHttp;

    private final String appName;

    private final AntPathMatcher antPathMatcher;

    private final Map<String, List<EndpointEntity>> endpointsToSkip;
    private final MaskingDataInterceptor maskingDataInterceptor;


    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        String retrofitRequestID = UUID.randomUUID().toString();
        MDC.put("retrofitRequestID", retrofitRequestID);

        try{
            Request request = chain.request();

            if (shouldSkip(request)) {
                return chain.proceed(request);
            }

            Invocation invocationTag = getInvocationTag(chain);

            String apiName = getApiName(request.url());
            String requestName = invocationTag != null ? getClientInvocationMethod(invocationTag) : "";
            Object requestBody = null;
            if (isOkHttp && invocationTag == null) {
                requestBody = getRequestPayload(request);
            }else  {
                requestBody = request.body();
            }

            String method = request.method();
            String host = "";
            if (request.url() != null && request.url().host() != null) {
                host = request.url().host();
            }

            JsonObject requestBodyMasked = maskingDataInterceptor.maskingBody(requestBody, method, MaskingDataInterceptor.REQUEST_KEY, invocationTag);
            log.info("--> Calling {} {} {} {}", method, request.url(), requestBodyMasked == null ? requestBody : requestBodyMasked, request.headers());

            Map<String, String> tags = getMetricInterceptor(apiName, requestName, method, host);

            metricService.incrementCounterWithoutServicePrefix("http_client_api", tags);

            try {
                request = setCorrelationIdToDownstreamServiceRequest(request);

                Response response = chain.proceed(request);
                String responseCode = String.valueOf(response.code());

                tags = getMetricInterceptor(apiName, requestName, method, host);
                tags.put(Constants.RESPONSE_CODE, getNullSafeString(responseCode));

                if (response.isSuccessful()) {
                    metricService.incrementCounterWithoutServicePrefix("http_client_api_success", tags);
                } else {
                    metricService.incrementCounterWithoutServicePrefix("http_client_api_failure", tags);
                }

                long timeElapsed = response.receivedResponseAtMillis() - response.sentRequestAtMillis();
                JsonObject maskedResponse = maskingDataInterceptor.maskingBody(response, method, apiName, MaskingDataInterceptor.RESPONSE_KEY, invocationTag);
                log.info(
                        append("time_taken", timeElapsed),
                        "<-- {} {} {}", response.protocol(), response.code(), maskedResponse == null ? new String(response.peekBody(4096).bytes()) : maskedResponse
                );

                metricService.recordTiming("digisign_http_client_api_latency",
                        timeElapsed,
                        Constants.HOST, getNullSafeString(host),
                        Constants.REQUEST_NAME, getNullSafeString(requestName),
                        Constants.API_NAME, getNullSafeString(apiName),
                        Constants.RESPONSE_CODE, getNullSafeString(responseCode),
                        Constants.METHOD, getNullSafeString(method),
                        Constants.APP_NAME, getNullSafeString(appName)
                );

                return response;
            } catch (Exception exception) {
                log.error("RetrofitMetricInterceptor - intercept | Caught exception in host: {} and api name: {} with error: {}", host, apiName, exception);

                tags = getMetricInterceptor(apiName, requestName, method, host);

                tags.put(Constants.RESPONSE_CODE, "caught_exception");

                metricService.incrementCounterWithoutServicePrefix("digisign_http_client_api_failure", tags);

                throw exception;
            }
        } catch (Exception exception) {
            log.error("RetrofitMetricInterceptor | Error while making HTTP request", exception);
            throw exception;
        } finally {
            MDC.remove("retrofitRequestID");
        }

    }

    public RetrofitInterceptor(MetricService metricService, boolean isOkHttp, String appName, AntPathMatcher antPathMatcher, Map<String, List<EndpointEntity>> endpointsToSkip, MaskingDataInterceptor maskingDataInterceptor) {
        this.metricService = metricService;
        this.isOkHttp = isOkHttp;
        this.appName = appName;
        this.antPathMatcher = antPathMatcher;
        this.endpointsToSkip = endpointsToSkip;
        this.maskingDataInterceptor = maskingDataInterceptor;
    }



    protected Request setCorrelationIdToDownstreamServiceRequest(Request request) {
        if (request != null && request.header(Constants.CORRELATION_ID_HEADER) == null) {
            String correlationId = MDC.get(Constants.CORRELATION_ID_HEADER);

            if (StringUtils.isEmpty(correlationId)) {
                correlationId = UUID.randomUUID().toString();
            }

            request = request.newBuilder()
                    .addHeader(Constants.CORRELATION_ID_HEADER, correlationId)
                    .build();
        }
        return request;
    }

    private Map<String, String> getMetricInterceptor(String apiName, String requestName, String method, String host) {
        Map<String, String> tags = new HashMap<>();
        tags.put(Constants.HOST, getNullSafeString(host));
        tags.put(Constants.REQUEST_NAME, getNullSafeString(requestName));
        tags.put(Constants.API_NAME, getNullSafeString(apiName));
        tags.put(Constants.METHOD, getNullSafeString(method));
        tags.put(Constants.APP_NAME, getNullSafeString(appName));
        return tags;
    }

    private String getNullSafeString(String value) {
        return value != null ? value : "";
    }
    private String getApiName(HttpUrl url) {

        if (url == null || url.encodedPath() == null) {
            return "";
        }

        String originalPath = url.encodedPath();

        originalPath = originalPath
                .replaceAll("/\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12}", "/{id}")
                .replaceAll("/\\d+-[\\w-]+-\\d+", "/{id}")
                .replaceAll("\\d+-\\d+-\\d+", "{id}")
                .replaceAll("/\\d+", "/{id}");

        return originalPath;
    }

    protected String getClientInvocationMethod(Invocation invocationTag) {
        return invocationTag.method() != null ? invocationTag.method().getName() : "";
    }
    protected Invocation getInvocationTag(Chain chain) {

        if (chain.call() == null) {
            return null;
        }

        if (chain.call().request() == null) {
            return null;
        }

        return chain.call().request().tag(Invocation.class);
    }
    private String getRequestPayload(Request request) {
        if (request.body() == null) {
            return null;
        }

        Buffer buffer = new Buffer();
        try {
            request.body().writeTo(buffer);
        } catch (IOException e) {
            log.error("unable to read request payload");
        }

        return buffer.readUtf8();
    }

    public boolean shouldSkip(Request request) {
        try {
            if(!endpointsToSkip.containsKey(request.url().host())) {
                return false;
            }
            List<EndpointEntity> endpoints = endpointsToSkip.get(request.url().host());

            return endpoints.stream().anyMatch(
                    entity -> areMethodsEqual(entity.getMethod(), request.method()) &&
                            areEndpointsEquivalent(entity.getEndpointPattern(), request.url())
            );
        } catch(Exception ex) {
            log.warn("Exception while determining log skipping criteria in HTTPLoggingInterceptor: ", ex);
            return false;
        }
    }

    public boolean areMethodsEqual(String method1, String method2) {
        return method1.toLowerCase(Locale.ROOT).equals(method2.toLowerCase(Locale.ROOT));
    }

    public boolean areEndpointsEquivalent(String sourcePath, HttpUrl endpoint) {
        return antPathMatcher.match(sourcePath, endpoint.uri().getPath());
    }

    @AllArgsConstructor
    @Getter
    @EqualsAndHashCode
    public static class EndpointEntity {
        @SerializedName("method")
        private final String method;
        @SerializedName("endpointPattern")
        private final String endpointPattern;
    }

}
