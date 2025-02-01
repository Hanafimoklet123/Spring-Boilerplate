package com.boilerplate.spring_boot.commons;

import com.boilerplate.spring_boot.commons.instrumentation.MetricService;
import com.boilerplate.spring_boot.commons.interceptor.MaskingDataInterceptor;
import com.boilerplate.spring_boot.commons.interceptor.RetrofitInterceptor;
import okhttp3.OkHttpClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

public class UnsafeOkHttpClient {
    public static OkHttpClient getUnsafeOkHttpClient(MetricService metricService, MaskingDataInterceptor maskingDataInterceptor, String appName, int serviceTimeoutMilliseconds) {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
            builder.addInterceptor(new RetrofitInterceptor(metricService, maskingDataInterceptor, appName, true));
            builder.callTimeout(serviceTimeoutMilliseconds, TimeUnit.MILLISECONDS);

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
