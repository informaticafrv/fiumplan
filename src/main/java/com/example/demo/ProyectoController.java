package com.example.demo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Pantallas de proyectos y tareas. Todo lo que modifica datos es POST (con token CSRF)
 * y se identifica por id; las comprobaciones de propiedad viven en ProyectoService.
 */
@Controller
public class ProyectoController {

    private final ProyectoService proyectoService;
    private final UsuarioService usuarioService;
    private final PdfService pdfService;

    public ProyectoController(ProyectoService proyectoService, UsuarioService usuarioService, PdfService pdfService) {
        this.proyectoService = proyectoService;
        this.usuarioService = usuarioService;
        this.pdfService = pdfService;
    }

    @GetMapping("/")
    public String mostrarInicio(Model model, Authentication authentication,
                                @RequestParam(name = "buscar", required = false) String buscar) {
        Usuario usuario = usuarioService.actual(authentication);

        if (buscar != null && !buscar.isBlank()) {
            // dejamos la palabra buscada escrita en la barra de búsqueda
            model.addAttribute("textoBusqueda", buscar);
        }
        model.addAttribute("listaProyectos", proyectoService.listar(usuario, buscar));
        model.addAttribute("usuario", usuario);
        return "index";
    }

    @GetMapping("/crearProyecto")
    public String mostrarFormularioCrear(Model model) {
        model.addAttribute("proyecto", new Proyecto());
        return "crearProyecto";
    }

    @GetMapping("/editarProyecto")
    public String editarProyecto(@RequestParam("id") Long id, Model model, Authentication authentication) {
        Usuario usuario = usuarioService.actual(authentication);
        model.addAttribute("proyecto", proyectoService.obtenerPropio(id, usuario));
        return "crearProyecto";
    }

    @PostMapping("/guardarProyecto")
    public String guardarProyecto(@RequestParam(name = "id", required = false) Long id,
                                  @RequestParam("nombre") String nombre,
                                  @RequestParam("descripcion") String descripcion,
                                  Authentication authentication,
                                  Model model) {
        Usuario usuario = usuarioService.actual(authentication);
        try {
            proyectoService.guardar(id, nombre, descripcion, usuario);
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            // devolvemos el formulario con lo que el usuario había escrito y el motivo del error
            Proyecto proyectoFallido = new Proyecto(nombre, descripcion, usuario);
            proyectoFallido.setId(id);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("proyecto", proyectoFallido);
            return "crearProyecto";
        }
    }

    @GetMapping("/verProyecto")
    public String verProyecto(@RequestParam("id") Long id,
                              @RequestParam(name = "orden", required = false) String orden,
                              Model model, Authentication authentication) {
        Usuario usuario = usuarioService.actual(authentication);
        Proyecto proyecto = proyectoService.obtenerPropio(id, usuario);
        boolean porPrioridad = "prioridad".equals(orden);

        model.addAttribute("proyecto", proyecto);
        model.addAttribute("tareasPrincipales", proyectoService.tareasPrincipales(proyecto, porPrioridad));
        model.addAttribute("porPrioridad", porPrioridad);
        return "proyecto";
    }

    @PostMapping("/borrarProyecto")
    public String borrarProyecto(@RequestParam("id") Long id, Authentication authentication) {
        proyectoService.borrar(id, usuarioService.actual(authentication));
        return "redirect:/";
    }

    @PostMapping("/insertarTareaPrincipal")
    public String insertarTareaPrincipal(@RequestParam("idProyecto") Long idProyecto,
                                         @RequestParam("titulo") String titulo,
                                         @RequestParam("descripcion") String descripcion,
                                         @RequestParam("prioridad") int prioridad,
                                         Authentication authentication,
                                         RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioService.actual(authentication);
        try {
            proyectoService.crearTareaPrincipal(idProyecto, titulo, descripcion, prioridad, usuario);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return volverAlProyecto(idProyecto);
    }

    @PostMapping("/insertarTareaSecundaria")
    public String insertarTareaSecundaria(@RequestParam("idProyecto") Long idProyecto,
                                          @RequestParam("idTareaPadre") Long idTareaPadre,
                                          @RequestParam("tituloSecundaria") String titulo,
                                          @RequestParam("descripcion") String descripcion,
                                          @RequestParam("prioridad") int prioridad,
                                          @RequestParam(name = "categoria", required = false) String categoria,
                                          Authentication authentication,
                                          RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioService.actual(authentication);
        try {
            proyectoService.crearTareaSecundaria(idTareaPadre, titulo, descripcion, prioridad, categoria, usuario);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return volverAlProyecto(idProyecto);
    }

    @PostMapping("/cambiarEstadoTarea")
    public String cambiarEstadoTarea(@RequestParam("idTarea") Long idTarea,
                                     // un checkbox desmarcado no envía el parámetro: ausente = false
                                     @RequestParam(name = "estado", required = false) Boolean estado,
                                     Authentication authentication) {
        Long idProyecto = proyectoService.cambiarEstado(idTarea, estado != null, usuarioService.actual(authentication));
        return volverAlProyecto(idProyecto);
    }

    @PostMapping("/borrarTarea")
    public String borrarTarea(@RequestParam("id") Long id, Authentication authentication) {
        Long idProyecto = proyectoService.borrarTarea(id, usuarioService.actual(authentication));
        return volverAlProyecto(idProyecto);
    }

    @GetMapping("/descargarPdf")
    public void descargarPdf(@RequestParam("id") Long id, Authentication authentication,
                             HttpServletResponse response) throws IOException {
        Proyecto proyecto = proyectoService.obtenerPropio(id, usuarioService.actual(authentication));

        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        // ContentDisposition escapa/codifica el nombre (tildes, comillas...) para que no rompa la cabecera
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename("proyecto_" + proyecto.getNombre() + ".pdf", StandardCharsets.UTF_8)
                .build().toString());
        pdfService.exportarProyecto(response, proyecto);
    }

    private String volverAlProyecto(Long idProyecto) {
        return "redirect:/verProyecto?id=" + idProyecto;
    }
}
