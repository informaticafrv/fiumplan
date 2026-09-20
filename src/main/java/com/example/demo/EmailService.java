package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String remitente;

    // El remitente debe ser uno verificado en SendGrid; se configura con app.mail.from (MAIL_FROM)
    public EmailService(JavaMailSender mailSender, @Value("${app.mail.from}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    @Async
    public void enviarCorreo(String destinatario, String asunto, String cuerpo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(remitente);
        message.setTo(destinatario);
        message.setSubject(asunto);
        message.setText(cuerpo);

        // al ser asíncrono nadie recibiría la excepción: la dejamos en el log
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("No se pudo enviar el correo a {}", destinatario, e);
        }
    }
}
