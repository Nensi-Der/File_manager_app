package dev.al.FileManagerApplication.service.impl;

import dev.al.FileManagerApplication.model.UserActivityLog;
import dev.al.FileManagerApplication.repository.UserActivityLogRepository;
import dev.al.FileManagerApplication.service.ActivityLoggerService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ActivityLoggerServiceImpl implements ActivityLoggerService {

    private final UserActivityLogRepository userActivityLogRepository;

    public ActivityLoggerServiceImpl(UserActivityLogRepository userActivityLogRepository) {
        this.userActivityLogRepository = userActivityLogRepository;
    }

    @Override
    public void logEvent(String userId, String eventType, String description) {
        UserActivityLog event = new UserActivityLog();
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setDescription(description);
        event.setEventTime(LocalDateTime.now());
        userActivityLogRepository.save(event);
    }
}