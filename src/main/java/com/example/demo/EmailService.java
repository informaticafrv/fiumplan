package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void enviarCorreo(String destinatario, String asunto, String cuerpo) {
        SimpleMailMessage message = new SimpleMailMessage();
        
        // ESTE EMAIL DEBE SER EL QUE VERIFICASTE EN EL PASO 2
        // Si pones otro, SendGrid bloqueará el correo.
        message.setFrom("informaticafrv1@gmail.com"); 
        
        message.setTo(destinatario);
        message.setSubject(asunto);
        message.setText(cuerpo);

        mailSender.send(message);
    }
}
