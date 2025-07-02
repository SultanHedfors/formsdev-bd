package com.example.demo.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class HttpLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            logRequest(wrappedRequest);
            logResponse(wrappedResponse);
            wrappedResponse.copyBodyToResponse(); // must be called to write response back to client
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String body = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);

        log.info("⬅️ Request: {} {}{}",
                method,
                uri,
                (query != null ? "?" + query : "")
        );
        log.info("Request Body: {}", body);
    }

    private void logResponse(ContentCachingResponseWrapper response) {
        String body = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
        int status = response.getStatus();

        log.info("➡️ Response: HTTP {}", status);
        log.info("Response Body: {}", body);
    }
}
