package com.boilerplate.spring_boot.commons.interceptor;

import com.google.api.client.json.Json;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import org.aspectj.apache.bcel.util.ClassPath;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import retrofit2.Invocation;
import software.amazon.awssdk.core.ApiName;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
public class MaskingDataInterceptor {

    public static final String REQUEST_KEY = "request";
    public static final String RESPONSE_KEY = "response";
    public static final String MASKING_VALUE = "*";
    public static final int BYTE_COUNT = 10000;

    public final Gson gson;
    private Map<String, List<MaskingDataRules>> maskingDataMap;
    private final boolean isMaskingDataEnabled;

    public MaskingDataInterceptor(Gson gson, boolean isMaskingDataEnabled) {
        this.gson = gson;
        this.isMaskingDataEnabled = isMaskingDataEnabled;
    }

    private void resolveMaskingDataFileToMap(String appName, Gson gson) {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(Thread.currentThread().getContextClassLoader());
        Resource[] resources = null;
        try {
            resources = resolver.getResources("/mask_request/*.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (resources.length > 0) {
            for (Resource resource : resources) {

                if(resource.getFilename() == null || ! resource.getFilename().equals(appName + "_masking_data_rules.json")) {
                    continue;
                }

                String maskingDataRequest = null;

                try {
                    maskingDataRequest = new String(new ClassPathResource("/mask_request/" + resource.getFilename()).getInputStream().readAllBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                maskingDataMap = gson.fromJson(maskingDataRequest, new TypeToken<Map<String, List<MaskingDataRules>>>() {}.getType());

                for (Map.Entry<String, List<MaskingDataRules>> request : maskingDataMap.entrySet()) {
                    List<MaskingDataRules> maskingDataList = request.getValue();

                    for (MaskingDataRules maskingData : maskingDataList) {
                        String field = maskingData.getField();
                        maskingData.setFieldTree(field.split("\\,"));
                    }
                }
            }
        }else {
            log.info("MaskingDataInterceptor | No masking data found for app: {}", appName);
        }
    }

    public JsonObject maskingBody(Object body, String method, String apiName, String action, Invocation invocationTags) throws IOException {

        try {
            JsonObject bodyMasked = null;
            if (body != null && action.equals(REQUEST_KEY)) {
                if (invocationTags == null) {
                    bodyMasked = gson.fromJson(body.toString(), JsonObject.class);
                } else {
                    try (Buffer buffer = new Buffer()) {
                        ((RequestBody) body).writeTo(buffer);
                        bodyMasked = gson.fromJson(buffer.readUtf8(), JsonObject.class);
                    }
                }
            }

            if (body != null && action.equals(RESPONSE_KEY)) {
                bodyMasked = gson.fromJson(new String(((Response) body).peekBody(BYTE_COUNT).bytes()), JsonObject.class);
            }

            if (! isMaskingDataEnabled || body == null) {
                return bodyMasked;
            }

            String key = "[" + method + "]" + apiName + "[" + action + "]";
            if (maskingDataMap == null || ! maskingDataMap.containsKey(key)) {
                return bodyMasked;
            }

            List<MaskingDataRules> maskingDataRules = maskingDataMap.get(key);
            for (MaskingDataRules maskingDataRule : maskingDataRules) {
                bodyMasked = mask(bodyMasked, maskingDataRule);
            }

            return bodyMasked;
        } catch (Exception e) {
            log.error("MaskingDataInterceptor - maskingBody | Error occurred while masking body: {}", e);
            return null;
        }
    }

    private JsonObject mask(JsonObject requestBody, MaskingDataRules maskingDataRules) {
        return maskRecursive(requestBody, maskingDataRules.getFieldTree(), 0, maskingDataRules.getStartIndexMasking());
    }

    private JsonObject maskRecursive(JsonObject requestBody, String[] fieldTree, int index, int startIndexMasking) {
        if (index >= fieldTree.length) {
            return requestBody;
        }

        String field = fieldTree[index];
        if (field.endsWith("[]")) {
            field = field.substring(0, field.length() - 2);
            if(requestBody.has(field) && requestBody.get(field).isJsonArray()) {
                for(JsonElement element : requestBody.getAsJsonArray(field)) {
                    if (element.isJsonObject()) {
                        maskRecursive(element.getAsJsonObject(), fieldTree, index + 1, startIndexMasking);
                    }
                }
            }
        } else if (requestBody.has(field)) {
            if (requestBody.get(field).isJsonObject()) {
                JsonObject nestedObject = requestBody.getAsJsonObject(field);
                nestedObject = maskRecursive(nestedObject, fieldTree, index + 1, startIndexMasking);
                requestBody.add(field, nestedObject);
            } else {
                String fieldString = requestBody.get(field).getAsString();
                if (fieldString.length() > startIndexMasking) {
                    fieldString = fieldString.replace(
                            fieldString.substring(startIndexMasking, fieldString.length()),
                            MASKING_VALUE.repeat(fieldString.length() - startIndexMasking)
                    );
                    requestBody.addProperty(field, fieldString);
                }
            }
        }
        return  requestBody;
    }
}
