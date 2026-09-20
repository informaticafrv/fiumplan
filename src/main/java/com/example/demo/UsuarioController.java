package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/registro")
    public String mostrarRegistro() {
        return "registro";
    }

    @PostMapping("/insertarUsuario")
    public String procesarRegistro(@RequestParam("nombre") String nombre,
                                   @RequestParam("email") String email,
                                   @RequestParam("password") String password,
                                   @RequestParam(name = "archivo", required = false) MultipartFile archivo,
                                   RedirectAttributes redirectAttributes) {
        try {
            usuarioService.registrar(nombre, email, password, archivo);
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            // volvemos al formulario con el motivo del error
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/registro";
        }
    }
}
