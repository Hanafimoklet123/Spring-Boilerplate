package com.boilerplate.spring_boot.interceptor;

import com.boilerplate.spring_boot.commons.interceptor.MaskingDataInterceptor;
import com.boilerplate.spring_boot.commons.interceptor.MaskingDataRule;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import retrofit2.Invocation;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.MockitoAnnotations.openMocks;

@SuppressWarnings({"PMD.CloseResource", "PMD.AvoidDuplicateLiterals"})
public class MaskingDataInterceptorTest {

    private MaskingDataInterceptor interceptor;
    private Gson gson = new Gson();
    private Map<String, List<MaskingDataRule>> maskingDataMap = Map.of(
            "[POST]/v1/test[request]", List.of(
                    MaskingDataRule.builder()
                            .field("name")
                            .fieldTree(new String[]{"name"})
                            .startIndexMasking(0)
                            .build(),
                    MaskingDataRule.builder()
                            .field("data.dummy")
                            .fieldTree(new String[]{"data", "dummy"})
                            .startIndexMasking(100)
                            .build(),
                    MaskingDataRule.builder()
                            .field("dataList[].dummy")
                            .fieldTree(new String[]{"dataList[]", "dummy"})
                            .startIndexMasking(3)
                            .build()
            ),
            "[POST]/v1/test[response]", List.of(
                    MaskingDataRule.builder()
                            .field("name")
                            .fieldTree(new String[]{"name"})
                            .startIndexMasking(0)
                            .build(),
                    MaskingDataRule.builder()
                            .field("data.dummy")
                            .fieldTree(new String[]{"data", "dummy"})
                            .startIndexMasking(100)
                            .build(),
                    MaskingDataRule.builder()
                            .field("dataList[].dummy")
                            .fieldTree(new String[]{"dataList[]", "dummy"})
                            .startIndexMasking(3)
                            .build()
            )
    );

    @Mock
    private Invocation invocation;

    class TestPojo {

        public TestPojo(String name, Data data, List<Data> dataList) {
            this.name = name;
            this.data = data;
            this.dataList = dataList;
        }

        private String name;
        private Data data;
        private List<Data> dataList;

        static class Data {

            public Data(String dummy) {
                this.dummy = dummy;
            }

            private String dummy;
        }
    }

    @BeforeEach
    public void setUp() {
        openMocks(this);
        interceptor = new MaskingDataInterceptor("testApp", true, gson);

        ReflectionTestUtils.setField(interceptor, "maskingDataMap", maskingDataMap);
    }

    @Nested
    class MaskingBody {

        @Test
        void testMaskingBody_withNullBody_returnsNull() throws IOException {
            JsonObject result = interceptor.maskingBody(null, "POST", "/v1/test", MaskingDataInterceptor.REQUEST_KEY, invocation);
            assertNull(result);
        }

        @Test
        void testMaskingBody_withEmptyBody_returnsEmptyJsonObject() throws IOException {
            RequestBody body = RequestBody.create("", MediaType.parse("application/json"));
            JsonObject result = interceptor.maskingBody(body, "POST", "/v1/test", MaskingDataInterceptor.REQUEST_KEY, invocation);
            assertNull(result);
        }

        @Test
        void testMaskingBody_withKeyNotFound_returnsBody() throws IOException {
            RequestBody body = RequestBody.create("{}", MediaType.parse("application/json"));
            JsonObject result = interceptor.maskingBody(body, "POST", "/v1/unknown", MaskingDataInterceptor.REQUEST_KEY, invocation);
            assertEquals(new JsonObject(), result);
        }

        @Test
        void testMaskingBody_withMaskingDataMapNull_returnsBody() throws IOException {
            ReflectionTestUtils.setField(interceptor, "maskingDataMap", null);
            RequestBody body = RequestBody.create("{}", MediaType.parse("application/json"));
            JsonObject result = interceptor.maskingBody(body, "POST", "/v1/test", MaskingDataInterceptor.REQUEST_KEY, invocation);
            assertEquals(new JsonObject(), result);
        }

        @Test
        void testMaskingBody_whenActionIsRequest_returnsRequestBody() throws IOException {
            TestPojo testPojo = new TestPojo("test-test-test-test", new TestPojo.Data("test-test-test-test"), List.of(new TestPojo.Data("test-test-test-test")));

            RequestBody body = RequestBody.create(gson.toJson(testPojo), MediaType.parse("application/json"));
            JsonObject actualResult = interceptor.maskingBody(body, "POST", "/v1/test", MaskingDataInterceptor.REQUEST_KEY, invocation);

            JsonObject expectedResult = gson.fromJson(gson.toJson(new TestPojo("*******************", new TestPojo.Data("test-test-test-test"), List.of(new TestPojo.Data("tes****************")))), JsonObject.class);

            assertEquals(expectedResult, actualResult);
        }

        @Test
        void testMaskingBody_whenActionIsRequest_withInvocationIsNull_returnsRequestBody() throws IOException {
            TestPojo testPojo = new TestPojo("test-test-test-test", new TestPojo.Data("test-test-test-test"), List.of(new TestPojo.Data("test-test-test-test")));

            JsonObject actualResult = interceptor.maskingBody(gson.toJson(testPojo), "POST", "/v1/test", MaskingDataInterceptor.REQUEST_KEY, null);

            JsonObject expectedResult = gson.fromJson(gson.toJson(new TestPojo("*******************", new TestPojo.Data("test-test-test-test"), List.of(new TestPojo.Data("tes****************")))), JsonObject.class);

            assertEquals(expectedResult, actualResult);
        }

        @Test
        void testMaskingBody_whenActionIsResponse_returnsRequestBody() throws IOException {
            TestPojo testPojo = new TestPojo("test-test-test-test", new TestPojo.Data("test-test-test-test"), List.of(new TestPojo.Data("test-test-test-test")));

            Response body = new Response.Builder()
                    .request(new okhttp3.Request.Builder().url("http://localhost").build())
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(gson.toJson(testPojo), MediaType.parse("application/json")))
                    .build();
            JsonObject actualResult = interceptor.maskingBody(body, "POST", "/v1/test", MaskingDataInterceptor.RESPONSE_KEY, invocation);

            JsonObject expectedResult = gson.fromJson(gson.toJson(new TestPojo("*******************", new TestPojo.Data("test-test-test-test"), List.of(new TestPojo.Data("tes****************")))), JsonObject.class);

            assertEquals(expectedResult, actualResult);
        }
    }
}
