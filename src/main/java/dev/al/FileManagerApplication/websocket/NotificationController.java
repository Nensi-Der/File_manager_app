package dev.al.FileManagerApplication.websocket;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class NotificationController {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Method to broadcast notifications to all subscribed clients
    public void sendNotification(String message) {
        messagingTemplate.convertAndSend("/topic/updates", message);
    }

    // (Optional) If you want to handle client messages:
    @MessageMapping("/hello")
    public void receiveMessage(String message) {
        System.out.println("Received message from client: " + message);
        sendNotification("Server received: " + message);
    }
}