package com.example.demo;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class ManejadorErrores {

    // Spring rechaza el archivo antes de llegar al controlador, así que se captura aquí
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String archivoDemasiadoGrande(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "La foto de perfil no puede superar los 5 MB.");
        return "redirect:/registro";
    }
}
