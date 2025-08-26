package dev.al.FileManagerApplication.service.impl;

import dev.al.FileManagerApplication.model.Notification;
import dev.al.FileManagerApplication.model.UserEntity;
import dev.al.FileManagerApplication.repository.NotificationRepository;
import dev.al.FileManagerApplication.repository.UserRepository;
import dev.al.FileManagerApplication.service.NotificationService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MessageSource messageSource;
    private final JavaMailSender mailSender;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserRepository userRepository,
                                   MessageSource messageSource, JavaMailSender mailsender, SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messageSource = messageSource;
        this.mailSender = mailsender;
        this.messagingTemplate = messagingTemplate;
    }


    @Override
    public void notifyUser(String username, String message) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(messageSource.getMessage("user.notfound", null, Locale.getDefault())));

        Notification notification = new Notification();
        notification.setMessage(message);
        notification.setRecipient(user);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);

        // Send WebSocket notification
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", message);

        // Send Email notification (simple example)
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(user.getEmail());
        mailMessage.setSubject("New Notification");
        mailMessage.setText(message);
        mailSender.send(mailMessage);
    }


    @Override
    public List<Notification> getUnreadNotifications(String username) {
        return notificationRepository.findByRecipientUsernameAndReadFalse(username);
    }

    @Override
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("object.notfound", new Object[]{"Notification"}, Locale.getDefault())
                ));
        notification.setRead(true);
        notificationRepository.save(notification);
    }
}