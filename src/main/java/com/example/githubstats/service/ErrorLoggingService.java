package com.example.githubstats.service;

import com.example.githubstats.entity.ErrorLog;
import com.example.githubstats.repository.ErrorLogRepository;
import org.kohsuke.github.HttpException; // Keep for GitHub
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException; // For RestTemplate
import org.springframework.web.client.HttpStatusCodeException; // General class

@Service
public class ErrorLoggingService {

    private static final Logger log = LoggerFactory.getLogger(ErrorLoggingService.class);
    private final ErrorLogRepository errorLogRepository;

    public ErrorLoggingService(ErrorLogRepository errorLogRepository) {
        this.errorLogRepository = errorLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(String source, String filterCriteria, String context, Throwable throwable) {
        try {
            String errorType = "RUNTIME";
            Integer statusCode = null;
            String message = throwable.getMessage();
            String responseBody = ""; // Capture response body if available

            // Dig for nested exceptions which might have more info
            Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;

            if (cause instanceof HttpStatusCodeException httpEx) { // Catches RestClientException too
                errorType = "HTTP";
                statusCode = httpEx.getStatusCode().value();
                responseBody = httpEx.getResponseBodyAsString();
                message = String.format("HTTP %d: %s - %s", statusCode, httpEx.getMessage(), responseBody);
            } else if (cause instanceof HttpException ghHttpEx) { // GitHub specific
                errorType = "HTTP";
                statusCode = ghHttpEx.getResponseCode();
                message = String.format("GitHub HTTP %d: %s", statusCode, ghHttpEx.getMessage());
            } else if (cause instanceof java.net.UnknownHostException) {
                errorType = "NETWORK";
                message = "Unknown host: " + cause.getMessage();
            } else if (cause instanceof java.net.SocketTimeoutException) {
                errorType = "TIMEOUT";
            } else if (cause instanceof java.io.IOException) {
                errorType = "IO";
            } // Add more specific checks if needed

            String finalMessage = (message != null && message.length() > 2000)
                    ? message.substring(0, 2000) + "..." // Truncate long messages/bodies
                    : message;
            if (finalMessage == null) {
                finalMessage = cause.getClass().getSimpleName() + " (no message)";
            }

            ErrorLog errorLog = new ErrorLog(source, errorType, finalMessage, statusCode, context, filterCriteria);
            errorLogRepository.save(errorLog);
            log.debug("Saved error log entry: Source='{}', Filter='{}', Context='{}', Type='{}'", source,
                    filterCriteria, context, errorType);

        } catch (Exception e) {
            log.error(
                    "CRITICAL: Failed to save error log entry! Original error context: Source='{}', Filter='{}', Context='{}', OriginalException='{}', LoggingException='{}'",
                    source, filterCriteria, context, throwable.getMessage(), e.getMessage());
        }
    }
}