package com.example.demo;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final ProyectoRepository proyectoRepository;

    public AdminController(UsuarioRepository usuarioRepository, ProyectoRepository proyectoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.proyectoRepository = proyectoRepository;
    }

    // 1. DASHBOARD: Muestra todos los usuarios
    @GetMapping("/dashboard")
    public String panelAdmin(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAll());
        return "admin_dashboard";
    }

    // 2. VER PROYECTOS: Filtra por usuario o muestra todos
    @GetMapping("/proyectos")
    public String verProyectosAdmin(@RequestParam(required = false) Long usuarioId, Model model) {
        if (usuarioId != null) {
            Usuario u = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
            model.addAttribute("proyectos", proyectoRepository.findByCreador(u));
            model.addAttribute("titulo", "Proyectos de " + u.getNombre());
        } else {
            model.addAttribute("proyectos", proyectoRepository.findAll());
            model.addAttribute("titulo", "Todos los Proyectos del Sistema");
        }
        return "admin_proyectos";
    }

    // 3. BORRAR PROYECTO: Lógica de borrado y vuelta al panel (POST: borrar no puede hacerse con un simple enlace)
    @PostMapping("/borrarProyecto")
    public String borrarProyectoAdmin(@RequestParam("id") Long id) {
        proyectoRepository.deleteById(id);
        return "redirect:/admin/proyectos";
    }
}
