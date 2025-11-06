package com.adoption.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String defaultFrom;

    @Value("${mail.from:}")
    private String customFrom;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPlainText(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        // 一些服务商（如 QQ 邮箱）要求 From 与账号一致
        String from = (customFrom != null && !customFrom.isEmpty()) ? customFrom : defaultFrom;
        message.setFrom(from);
        mailSender.send(message);
    }
}
