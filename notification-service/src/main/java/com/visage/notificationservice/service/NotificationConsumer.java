package com.visage.notificationservice.service;

import com.visage.notificationservice.model.FileUploadEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {

    private final JavaMailSender mailSender;

    @KafkaListener(topics = "file-upload-topic", groupId = "notification-group")
    public void consume(FileUploadEvent event) {
        log.info("Received file upload event: {}", event);
        if (event.getSize() > 500 * 1024) { // > 500KB
            sendEmail(event);
        }
    }

    private void sendEmail(FileUploadEvent event) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("nsky9623@gmail.com");
            message.setTo(event.getUsername()); // Assuming username is the email
            message.setSubject("Large File Uploaded");
            message.setText("Здравствуйте " + event.getUsername() + ",\n\n Вы успешно загрузили большой файл: "
                            + event.getFilename() + " (" + event.getSize() + " байтов).");
            
            mailSender.send(message);
            log.info("Notification email sent to {}", event.getUsername());
        } catch (Exception e) {
            log.error("Failed to send email to {}", event.getUsername(), e);
        }
    }
}