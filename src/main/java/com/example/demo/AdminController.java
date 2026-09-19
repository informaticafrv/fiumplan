package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

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
            Usuario u = usuarioRepository.findById(usuarioId).orElse(null);
            model.addAttribute("proyectos", proyectoRepository.findByCreador(u));
            model.addAttribute("titulo", "Proyectos de " + (u != null ? u.getNombre() : "Usuario"));
        } else {
            model.addAttribute("proyectos", proyectoRepository.findAll());
            model.addAttribute("titulo", "Todos los Proyectos del Sistema");
        }
        return "admin_proyectos";
    }

    // 3. BORRAR PROYECTO: Lógica de borrado y vuelta al panel
    @GetMapping("/borrarProyecto")
    public String borrarProyectoAdmin(@RequestParam("id") Long id) {
        proyectoRepository.deleteById(id);
        return "redirect:/admin/proyectos"; 
    }
}