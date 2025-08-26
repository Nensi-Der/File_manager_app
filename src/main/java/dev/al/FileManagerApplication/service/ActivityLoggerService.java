package dev.al.FileManagerApplication.service;

public interface ActivityLoggerService {
    void logEvent(String userId, String eventType, String description);
}