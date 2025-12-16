package com.authenticationservice.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class LoggingConstants {

    public static final String TRACE_ID_HEADER = "X-Request-Id";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String USER_EMAIL_MDC_KEY = "userEmail";
    public static final String CLIENT_IP_MDC_KEY = "clientIp";
    public static final String HTTP_METHOD_MDC_KEY = "httpMethod";
    public static final String REQUEST_PATH_MDC_KEY = "requestPath";
    public static final String USER_AGENT_MDC_KEY = "userAgent";
}

