package com.example.demo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;


@Controller
public class ProyectoControler {

    @Autowired
    private PdfService pdfService;

    @Autowired
    private TareaRepository tareaRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @GetMapping("/")
    public String mostrarInicio(Model model, Authentication authentication, @RequestParam(name = "buscar", required = false) String buscar) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        // 1. Obtenemos el email del usuario logueado desde Spring Security
        String email = authentication.getName();

        // 2. Buscamos el objeto Usuario completo en la base de datos
        Usuario usuario = usuarioRepository.findByEmail(email);

        // 3. Buscamos sus proyectos
        List<Proyecto> proyectos;

    // 👇 LÓGICA DEL BUSCADOR 👇
    if (buscar != null && !buscar.trim().isEmpty()) {
        // Si el usuario ha escrito algo, buscamos por ese patrón
        proyectos = proyectoRepository.findByNombreContainingIgnoreCaseAndCreador(buscar, usuario);
        // Guardamos la palabra buscada para dejarla escrita en la barra de búsqueda visualmente
        model.addAttribute("textoBusqueda", buscar); 
    } else {
        // Si no ha buscado nada, mostramos todos sus proyectos normalmente
        proyectos = proyectoRepository.findByCreador(usuario);
    }

        model.addAttribute("listaProyectos", proyectos);
        // También pasamos el usuario a la vista por si quieres mostrar su nombre o foto
        model.addAttribute("usuario", usuario); 
        
        return "index";
    }
    
   @PostMapping("/guardarProyecto")
    public String guardarProyecto(@RequestParam(name="id", required=false) Long id,
                                  @RequestParam("nombre") String nombre, 
                                  @RequestParam("descripcion") String descripcion, 
                                  Authentication authentication,
                                  Model model) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email);
        if (usuario == null) return "redirect:/login";
        Proyecto proyectoExistente = proyectoRepository.findByNombreAndCreador(nombre, usuario);
        if (proyectoExistente != null) {
            if (id == null || !proyectoExistente.getId().equals(id)) {
                model.addAttribute("error", "Ya tienes un proyecto llamado '" + nombre + "'. Elige otro nombre.");
                Proyecto proyectoFallido = new Proyecto(nombre, descripcion, usuario);
                if (id != null) proyectoFallido.setId(id);
                
                model.addAttribute("proyecto", proyectoFallido);
                
                return "crearProyecto";
            }
        }
        Proyecto proyecto;

        if (id != null) {
            // EDICIÓN
            proyecto = proyectoRepository.findById(id).orElse(null);
            if (proyecto != null) {
                if (proyecto.getCreador().getId().equals(usuario.getId())) {
                    proyecto.setNombre(nombre);
                    proyecto.setDescripcion(descripcion);
                } else {
                    return "redirect:/?error=No tienes permiso";
                }
            } else {
                return "redirect:/";
            }
        } else {
            proyecto = new Proyecto(nombre, descripcion, usuario);
        }
        proyectoRepository.save(proyecto);
        return "redirect:/";
    }

    @GetMapping("/crearProyecto")
    public String mostrarFormularioCrear(Model model) {
        model.addAttribute("proyecto", new Proyecto());
        return "crearProyecto";
    }
    
    @GetMapping("/verProyecto")
    public String verProyecto(@RequestParam String nombre, Model model, Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email);
        Proyecto proyecto = proyectoRepository.findByNombreAndCreador(nombre, usuario);
        if (proyecto != null) {
            model.addAttribute("proyecto", proyecto);
            return "proyecto";
        } else {
            return "redirect:/?error=No se encuentra el proyecto";
        }
    }
    @RequestMapping("/insertarTareaPrincipal")
    public String insertarTareaPrincipal(@RequestParam("idProyecto") Long idProyecto, 
                                         @RequestParam("titulo") String titulo, 
                                         @RequestParam("descripcion") String descripcion, 
                                         @RequestParam("prioridad") int prioridad) {
        Proyecto proyecto = proyectoRepository.findById(idProyecto).orElse(null);

        if (proyecto != null) {
            TareaPrincipal tareaPrincipal = new TareaPrincipal(titulo, descripcion, false, prioridad, LocalDate.now());
            tareaPrincipal.setProyecto(proyecto); 
            proyecto.insertarTarea(tareaPrincipal);
            proyectoRepository.save(proyecto);
            return "redirect:/verProyecto?nombre=" + proyecto.getNombre();
        }
        
        return "redirect:/";
    }

    @GetMapping("/insertarTareaSecundaria")
    public String insertarTareaSecundaria(@RequestParam Long idProyecto,
                                          @RequestParam Long idTareaPadre, 
                                          @RequestParam String tituloSecundaria, 
                                          @RequestParam String descripcion, 
                                          @RequestParam int prioridad,
                                          @RequestParam String categoria) {
        
        Proyecto proyecto = proyectoRepository.findById(idProyecto).orElse(null);
        Tarea tareaPosiblePadre = tareaRepository.findById(idTareaPadre).orElse(null);
        if (proyecto != null && tareaPosiblePadre instanceof TareaPrincipal) {
            TareaPrincipal tareaPadre = (TareaPrincipal) tareaPosiblePadre;

            TareaSecundaria subtarea = new TareaSecundaria(); 
            subtarea.setTitulo(tituloSecundaria);
            subtarea.setDescription(descripcion);
            subtarea.setPrioridad(prioridad);
            subtarea.setCategoria(categoria);
            subtarea.setEstado(false);
            subtarea.setFechaCreacion(java.time.LocalDate.now());

            if (tareaPadre.isEstado()) {
                tareaPadre.setEstado(false); 
                // Guardamos el padre con el nuevo estado
                tareaRepository.save(tareaPadre);
            }
            
            subtarea.setProyecto(proyecto);
            subtarea.setTareaPadre(tareaPadre); 
            
            tareaRepository.save(subtarea);
            
            return "redirect:/verProyecto?nombre=" + proyecto.getNombre();
        }
        
        return "redirect:/";
    }
    
    @GetMapping("/proyecto/ordenarPrioridad")
    public String ordenarTareasPorPrioridad (@RequestParam("nombre") String nombreProyecto, Model model) {
        Proyecto proyecto = proyectoRepository.findByNombre(nombreProyecto);
        if(proyecto != null){
            proyecto.getTareas().sort((t2, t1) -> t1.getPrioridad() - t2.getPrioridad());

            for (Tarea t  : proyecto.getTareas()) {
                if(t instanceof TareaPrincipal tareaPrincipal){
                    tareaPrincipal.getTareasSecundarias().sort((s2, s1) -> s1.getPrioridad() - s2.getPrioridad());
                }
            }
            model.addAttribute("proyecto", proyecto);
            return "proyecto";
        }else{
            return "redirect:/";
        }
    }
    @GetMapping("/cambiarEstadoTarea")
    public String cambiarEstadoTarea(@RequestParam("idTarea") Long idTarea, 
                                     @RequestParam(value = "estado", required = false) Boolean estado) {
        
        Tarea tarea = tareaRepository.findById(idTarea).orElse(null);
        
        if (tarea != null) {
            boolean nuevoEstado = (estado != null);
            tarea.setEstado(nuevoEstado);
            if (tarea instanceof TareaPrincipal) {
                TareaPrincipal principal = (TareaPrincipal) tarea;
                if (principal.getTareasSecundarias() != null) {
                    for (TareaSecundaria sub : principal.getTareasSecundarias()) {
                        sub.setEstado(nuevoEstado);
                    }
                }
            }
            tareaRepository.save(tarea);
            
            String nombreProyecto = tarea.getProyecto().getNombre();
            return "redirect:/verProyecto?nombre=" + nombreProyecto;
        }
        
        return "redirect:/";
    }
    @GetMapping("/borrarProyecto")
    public String borrarProyecto(@RequestParam("id") Long id) {
        proyectoRepository.deleteById(id);
        return "redirect:/";
    }
    @GetMapping("/editarProyecto")
    public String editarProyecto(@RequestParam("id") Long id, Model model) {
        Proyecto proyecto = proyectoRepository.findById(id).orElse(null);
        model.addAttribute("proyecto", proyecto);
        return "crearProyecto";
    }

    @GetMapping("/borrarTarea")
        public String borrarTarea(@RequestParam("id") Long id, @RequestParam("nombreProyecto") String nombreProyecto) {
            tareaRepository.deleteById(id);
            return "redirect:/verProyecto?nombre=" + nombreProyecto;
        }
    @GetMapping("/descargarPdf")
    public void descargarPdf(@RequestParam("id") Long id, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        Proyecto proyecto = proyectoRepository.findById(id).orElse(null);
        
        if(proyecto != null){
            response.setContentType("application/pdf");
            // Esta línea hace que el navegador te pregunte dónde guardar el archivo
            String headerKey = "Content-Disposition";
            String headerValue = "attachment; filename=proyecto_" + proyecto.getNombre() + ".pdf";
            response.setHeader(headerKey, headerValue);
            
            pdfService.exportarProyecto(response, proyecto);
        }
    }

}
