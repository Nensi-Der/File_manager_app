package dev.al.FileManagerApplication.service;

import dev.al.FileManagerApplication.model.Notification;

import java.util.List;

public interface NotificationService {
    void notifyUser(String username, String message);
    List<Notification> getUnreadNotifications(String username);
    void markAsRead(Long notificationId);
}