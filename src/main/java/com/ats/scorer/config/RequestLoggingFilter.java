package com.ats.scorer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String REQUEST_ID = "requestId";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        String previousRequestId = MDC.get(REQUEST_ID);
        long startTime = System.nanoTime();
        MDC.put(REQUEST_ID, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        boolean failed = false;
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            failed = true;
            throw exception;
        } finally {
            long durationMillis = (System.nanoTime() - startTime) / 1_000_000;
            int status = response.getStatus();
            if (failed && status < HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
                status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }

            if (status >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
                logger.error("Request completed method={} path={} status={} durationMs={}",
                        request.getMethod(), request.getRequestURI(), status, durationMillis);
            } else if (status >= HttpServletResponse.SC_BAD_REQUEST) {
                logger.warn("Request completed method={} path={} status={} durationMs={}",
                        request.getMethod(), request.getRequestURI(), status, durationMillis);
            } else {
                logger.info("Request completed method={} path={} status={} durationMs={}",
                        request.getMethod(), request.getRequestURI(), status, durationMillis);
            }

            if (previousRequestId == null) {
                MDC.remove(REQUEST_ID);
            } else {
                MDC.put(REQUEST_ID, previousRequestId);
            }
        }
    }
}