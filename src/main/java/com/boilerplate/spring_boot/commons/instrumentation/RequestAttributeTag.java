package com.boilerplate.spring_boot.commons.instrumentation;


import com.boilerplate.spring_boot.commons.Constants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@SuppressWarnings("PMD.ConstructurCallOverridableMethod")
public class RequestAttributeTag {

    private HttpServletRequest request;
    private String upstreamClassName;

    private static final Pattern NONUUID_SUBPATH_REGEX = Pattern.compile("^(?:v[0-9]|([A-Za-z-_]+))$");

    protected RequestAttributeTag(String upstreamClassName) {
        this.upstreamClassName = upstreamClassName;
        request = getHttpServletRequest();
    }

    protected HttpServletRequest getHttpServletRequest() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if(requestAttributes == null) {
            return null;
        }

        return requestAttributes.getRequest();
    }

//    public String getPartnerTag() {
//        String partner = "";
//        try {
//            partner = Objects.requireNonNull(request.getHeader(ONEKYC_PARTNER_HEADER_KEY));
//        } catch (Exception e) {
//            log.debug("Exception {} in {} while getting partner tag.", e.getMessage(), upstreamClassName);
//        }
//        return partner;
//    }
//
//    public String getPodNameTag() {
//        String podName = "";
//        try {
//            podName = Objects.requireNonNull(getSystemEnv(PODNAME_KEY));
//        } catch (Exception e) {
//            log.debug("Exception {} in {} while getting podName tag.", e.getMessage(), upstreamClassName);
//        }
//        return podName;
//    }

    public String getRequestURI() {
        String requestURI = "";
        try {
            requestURI = Objects.requireNonNull(request.getRequestURI());
        } catch (Exception e) {
            log.debug("Exception {} in {} while getting request URI.", e.getMessage(), upstreamClassName);
        }

        return requestURI;
    }

    public String getCleanedRequestURI() {
        String requestURI = "";
        try {
            requestURI = Objects.requireNonNull(request.getRequestURI());

            String[] splittedRequestUri = requestURI.split("/");
            StringBuilder cleanedRequestUriBuilder = new StringBuilder();
            for (int i = 1; i < splittedRequestUri.length; i++) {
                String subpath = splittedRequestUri[i];
                cleanedRequestUriBuilder.append("/");
                if (NONUUID_SUBPATH_REGEX.matcher(subpath).matches()) {
                    cleanedRequestUriBuilder.append(subpath);
                } else {
                    cleanedRequestUriBuilder.append("{uuid}");
                }
            }

            return cleanedRequestUriBuilder.toString();
        } catch (Exception e) {
            log.debug("Exception {} in {} while getting request URI.", e.getMessage(), upstreamClassName);
        }

        return requestURI;
    }

    public String getRequestURIMethod() {
        String requestURIMethod = "";
        try {
            requestURIMethod = Objects.requireNonNull(request.getMethod());
        } catch (Exception e) {
            log.debug("Exception {} in {} while getting request URI method.", e.getMessage(), upstreamClassName);
        }
        return requestURIMethod;
    }

    public Map<String, String> getHeadersMap() {
        Map<String, String> headersMap = new HashMap<>();

        try {
            Enumeration<String> headerEnumeration = request.getHeaderNames();
            while (headerEnumeration.hasMoreElements()) {
                String header = headerEnumeration.nextElement();
                headersMap.put(header, request.getHeader(header));
            }
        } catch (Exception e) {
            log.debug("Exception {} in {} while getting headers.", e.getMessage(), upstreamClassName);
        }

        return headersMap;
    }

    public String getOrGenerateCorrelationId() {
        if (request.getHeader(Constants.CORRELATION_ID_HEADER) != null) {
            return request.getHeader(Constants.CORRELATION_ID_HEADER);
        }
        return UUID.randomUUID().toString();
    }

    protected String getSystemEnv(String varName) {
        return System.getenv(varName);
    }

}
