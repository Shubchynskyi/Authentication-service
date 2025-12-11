package com.authenticationservice.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class LoggingConstants {

    public static final String TRACE_ID_HEADER = "X-Request-Id";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String USER_EMAIL_MDC_KEY = "userEmail";
}

