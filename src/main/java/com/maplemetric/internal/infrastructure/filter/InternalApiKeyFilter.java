package com.maplemetric.internal.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.common.ApiResponse;
import com.maplemetric.internal.infrastructure.properties.InternalApiProperties;
import com.maplemetric.internal.presentation.code.InternalErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-Internal-API-Key";
    private static final String PROTECTED_PATH = "/internal";
    private static final String PROTECTED_PATH_PREFIX = PROTECTED_PATH + "/";

    private final InternalApiProperties properties;
    private final ObjectMapper objectMapper;

    public InternalApiKeyFilter(
            InternalApiProperties properties,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = resolveApplicationPath(request);

        return !PROTECTED_PATH.equals(path)
                && !path.startsWith(PROTECTED_PATH_PREFIX);
    }

    private String resolveApplicationPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();

        if (requestUri == null) {
            return "";
        }

        String contextPath = request.getContextPath();

        if (StringUtils.hasLength(contextPath)
                && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }

        return requestUri;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isAuthorized(request.getHeader(API_KEY_HEADER))) {
            log.warn(
                    "내부 API 인증에 실패했습니다. 경로={}",
                    request.getRequestURI()
            );

            writeUnauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthorized(String requestKey) {
        String configuredKey = properties.apiKey();

        if (!StringUtils.hasText(configuredKey)
                || !StringUtils.hasText(requestKey)) {
            return false;
        }

        byte[] configured =
                configuredKey.getBytes(StandardCharsets.UTF_8);
        byte[] requested =
                requestKey.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(configured, requested);
    }

    private void writeUnauthorized(
            HttpServletResponse response
    ) throws IOException {
        InternalErrorCode errorCode =
                InternalErrorCode.INTERNAL_ACCESS_DENIED;

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                objectMapper.writeValueAsString(
                        ApiResponse.error(errorCode)
                )
        );
    }
}
