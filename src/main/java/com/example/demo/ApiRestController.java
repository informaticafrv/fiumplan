package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiRestController {

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // GET: Todos
    @GetMapping("/proyectos")
    public List<Proyecto> listarProyectos() {
        return proyectoRepository.findAll();
    }
    
    // GET: por patrón
    @GetMapping("/proyectos/buscar")
    public List<Proyecto> buscarProyectos(@RequestParam String texto) {
        List<Proyecto> todos = proyectoRepository.findAll();
        return todos.stream()
                .filter(p -> p.getNombre().toLowerCase().contains(texto.toLowerCase()))
                .toList();
    }

    // POST: Crear un nuevo proyecto
    @PostMapping("/proyectos")
    public Proyecto crearProyecto(@RequestBody Proyecto proyecto, Authentication authentication) {
        // Le asignamos automáticamente como creador al usuario que ha iniciado sesión en Swagger
        if (authentication != null && authentication.isAuthenticated()) {
            Usuario creador = usuarioRepository.findByEmail(authentication.getName());
            proyecto.setCreador(creador);
        }
        return proyectoRepository.save(proyecto);
    }

    //PUT: Modificar
    @PutMapping("/proyectos/{id}")
    public Proyecto actualizarProyecto(@PathVariable Long id, @RequestBody Proyecto proyectoDatosNuevos) {
        Proyecto proyectoExistente = proyectoRepository.findById(id).orElse(null);
        
        if (proyectoExistente != null) {
            
            // Solo cambiamos el nombre si nos han enviado uno válido
            if (proyectoDatosNuevos.getNombre() != null && !proyectoDatosNuevos.getNombre().trim().isEmpty()) {
                proyectoExistente.setNombre(proyectoDatosNuevos.getNombre());
            }
            
            // Solo cambiamos la descripción si nos han enviado una válida
            if (proyectoDatosNuevos.getDescripcion() != null && !proyectoDatosNuevos.getDescripcion().trim().isEmpty()) {
                proyectoExistente.setDescripcion(proyectoDatosNuevos.getDescripcion());
            }
            
            return proyectoRepository.save(proyectoExistente);
        }
        return null;
    }

    //DELETE: Borrar un proyecto
    @DeleteMapping("/proyectos/{id}")
    public String borrarProyecto(@PathVariable Long id) {
        if (proyectoRepository.existsById(id)) {
            proyectoRepository.deleteById(id);
            return "Proyecto eliminado correctamente";
        }
        return "El proyecto no existe";
    }
}