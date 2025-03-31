package com.example.githubstats.service;

import com.example.githubstats.entity.ErrorLog;
import com.example.githubstats.repository.ErrorLogRepository;
import org.kohsuke.github.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ErrorLoggingService {

    private static final Logger log = LoggerFactory.getLogger(ErrorLoggingService.class);
    private final ErrorLogRepository errorLogRepository;


    public ErrorLoggingService(ErrorLogRepository errorLogRepository) {
        this.errorLogRepository = errorLogRepository;
    }

    // Run in a new transaction to ensure errors are saved even if the main process rolls back
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(String filterCriteria, String context, Throwable throwable) {
        try {
            String errorType = "RUNTIME";
            Integer statusCode = null;
            String message = throwable.getMessage();

            // Extract more specific info if possible
            Throwable cause = throwable.getCause();
            Throwable effectiveThrowable = cause != null ? cause : throwable; // Prefer cause if available

            if (effectiveThrowable instanceof HttpException httpEx) {
                errorType = "HTTP";
                statusCode = httpEx.getResponseCode();
                message = String.format("HTTP %d: %s", statusCode, httpEx.getMessage());
            } else if (effectiveThrowable instanceof java.io.IOException) {
                errorType = "IO";
            } else if (effectiveThrowable instanceof org.kohsuke.github.GHFileNotFoundException) {
                errorType = "NOT_FOUND";
                message = String.format("GitHub resource not found: %s", effectiveThrowable.getMessage());
            }
            // Add more specific types if needed

            // Truncate message if too long, or rely on DB truncation/error
            String finalMessage = (message != null && message.length() > 1000) ? message.substring(0, 1000) + "..." : message;
            if (finalMessage == null) {
                finalMessage = effectiveThrowable.getClass().getSimpleName() + " (no message)";
            }

            ErrorLog errorLog = new ErrorLog(errorType, finalMessage, statusCode, context, filterCriteria);
            errorLogRepository.save(errorLog);
            log.debug("Saved error log entry for filter '{}', context '{}'", filterCriteria, context);

        } catch (Exception e) {
            // Log desperately if saving the error itself fails
            log.error("CRITICAL: Failed to save error log entry! Original error context: Filter='{}', Context='{}', OriginalException='{}', LoggingException='{}'",
                    filterCriteria, context, throwable.getMessage(), e.getMessage());
        }
    }

}
