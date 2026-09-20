package com.example.demo;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * API REST de proyectos. Requiere autenticación (HTTP Basic con email y contraseña)
 * y cada usuario solo ve y modifica sus propios proyectos.
 */
@RestController
@RequestMapping("/api")
public class ApiRestController {

    /** Lo único que el cliente puede enviar: así no puede fijar el id, el creador ni las tareas. */
    public record ProyectoRequest(String nombre, String descripcion) {}

    private final ProyectoService proyectoService;
    private final UsuarioService usuarioService;

    public ApiRestController(ProyectoService proyectoService, UsuarioService usuarioService) {
        this.proyectoService = proyectoService;
        this.usuarioService = usuarioService;
    }

    // GET: Todos los del usuario
    @GetMapping("/proyectos")
    public List<Proyecto> listarProyectos(Authentication authentication) {
        return proyectoService.listar(usuarioService.actual(authentication), null);
    }

    // GET: por patrón
    @GetMapping("/proyectos/buscar")
    public List<Proyecto> buscarProyectos(@RequestParam String texto, Authentication authentication) {
        return proyectoService.listar(usuarioService.actual(authentication), texto);
    }

    // GET: uno
    @GetMapping("/proyectos/{id}")
    public Proyecto obtenerProyecto(@PathVariable Long id, Authentication authentication) {
        return proyectoService.obtenerPropio(id, usuarioService.actual(authentication));
    }

    // POST: Crear un nuevo proyecto (el creador es siempre el usuario autenticado)
    @PostMapping("/proyectos")
    public ResponseEntity<Proyecto> crearProyecto(@RequestBody ProyectoRequest datos, Authentication authentication) {
        Proyecto creado = proyectoService.guardar(null, datos.nombre(), datos.descripcion(),
                usuarioService.actual(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // PUT: Modificar. Los campos que no se envían (o vienen en blanco) se dejan como estaban
    @PutMapping("/proyectos/{id}")
    public Proyecto actualizarProyecto(@PathVariable Long id, @RequestBody ProyectoRequest datos,
                                       Authentication authentication) {
        Usuario usuario = usuarioService.actual(authentication);
        Proyecto existente = proyectoService.obtenerPropio(id, usuario);
        String nombre = esVacio(datos.nombre()) ? existente.getNombre() : datos.nombre();
        String descripcion = esVacio(datos.descripcion()) ? existente.getDescripcion() : datos.descripcion();
        return proyectoService.guardar(id, nombre, descripcion, usuario);
    }

    // DELETE: Borrar un proyecto
    @DeleteMapping("/proyectos/{id}")
    public ResponseEntity<Void> borrarProyecto(@PathVariable Long id, Authentication authentication) {
        proyectoService.borrar(id, usuarioService.actual(authentication));
        return ResponseEntity.noContent().build();
    }

    // Nombre repetido -> 409; datos no válidos -> 400 (con el motivo en el cuerpo)
    @ExceptionHandler(NombreDuplicadoException.class)
    public ResponseEntity<String> nombreDuplicado(NombreDuplicadoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> datosNoValidos(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    private static boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
