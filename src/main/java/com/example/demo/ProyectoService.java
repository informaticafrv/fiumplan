package com.example.demo;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lógica de proyectos y tareas. Toda operación recibe el usuario que la pide y solo
 * trabaja con datos suyos: si algo no existe o es de otro usuario, responde 404
 * (así tampoco se revela qué ids existen).
 */
@Service
@Transactional
public class ProyectoService {

    private static final int MAX_TEXTO = 255;
    private static final int MAX_DESCRIPCION_PROYECTO = 1000;
    private static final int PRIORIDAD_MIN = 1;
    private static final int PRIORIDAD_MAX = 10;

    private final ProyectoRepository proyectoRepository;
    private final TareaRepository tareaRepository;

    public ProyectoService(ProyectoRepository proyectoRepository, TareaRepository tareaRepository) {
        this.proyectoRepository = proyectoRepository;
        this.tareaRepository = tareaRepository;
    }

    // ---------- Proyectos ----------

    @Transactional(readOnly = true)
    public List<Proyecto> listar(Usuario usuario, String buscar) {
        if (buscar != null && !buscar.isBlank()) {
            return proyectoRepository.findByNombreContainingIgnoreCaseAndCreador(buscar.trim(), usuario);
        }
        return proyectoRepository.findByCreador(usuario);
    }

    @Transactional(readOnly = true)
    public Proyecto obtenerPropio(Long id, Usuario usuario) {
        return proyectoRepository.findById(id)
                .filter(p -> esPropietario(p, usuario))
                .orElseThrow(ProyectoService::noEncontrado);
    }

    /** Crea (id == null) o edita un proyecto. */
    public Proyecto guardar(Long id, String nombre, String descripcion, Usuario usuario) {
        String nombreLimpio = limpiar(nombre, "El nombre", MAX_TEXTO, true);
        String descripcionLimpia = limpiar(descripcion, "La descripción", MAX_DESCRIPCION_PROYECTO, true);

        Proyecto proyecto = (id == null) ? new Proyecto() : obtenerPropio(id, usuario);

        // se comprueba antes de modificar la entidad para no provocar un flush con el nombre ya repetido
        proyectoRepository.findByNombreAndCreador(nombreLimpio, usuario)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> { throw new NombreDuplicadoException(nombreLimpio); });

        proyecto.setNombre(nombreLimpio);
        proyecto.setDescripcion(descripcionLimpia);
        proyecto.setCreador(usuario);
        try {
            return proyectoRepository.saveAndFlush(proyecto);
        } catch (DataIntegrityViolationException e) {
            // dos peticiones a la vez con el mismo nombre: la restricción única de la BD lo detiene
            throw new NombreDuplicadoException(nombreLimpio);
        }
    }

    public void borrar(Long id, Usuario usuario) {
        proyectoRepository.delete(obtenerPropio(id, usuario));
    }

    /** Tareas principales del proyecto; si se pide, de mayor a menor prioridad. */
    @Transactional(readOnly = true)
    public List<TareaPrincipal> tareasPrincipales(Proyecto proyecto, boolean porPrioridad) {
        List<TareaPrincipal> tareas = proyecto.getSoloTareasPrincipales();
        if (porPrioridad) {
            tareas.sort(Comparator.comparingInt(Tarea::getPrioridad).reversed());
        }
        return tareas;
    }

    // ---------- Tareas (todas devuelven el id del proyecto para poder redirigir) ----------

    public Long crearTareaPrincipal(Long idProyecto, String titulo, String descripcion, int prioridad, Usuario usuario) {
        Proyecto proyecto = obtenerPropio(idProyecto, usuario);
        String tituloLimpio = limpiar(titulo, "El título", MAX_TEXTO, true);
        String descripcionLimpia = limpiar(descripcion, "La descripción", MAX_TEXTO, true);
        validarPrioridad(prioridad);

        proyecto.insertarTarea(new TareaPrincipal(tituloLimpio, descripcionLimpia, false, prioridad, LocalDate.now()));
        proyectoRepository.save(proyecto);
        return proyecto.getId();
    }

    public Long crearTareaSecundaria(Long idTareaPadre, String titulo, String descripcion, int prioridad,
                                     String categoria, Usuario usuario) {
        if (!(tareaPropia(idTareaPadre, usuario) instanceof TareaPrincipal padre)) {
            throw new IllegalArgumentException("Solo se pueden añadir subtareas a una tarea principal.");
        }
        String tituloLimpio = limpiar(titulo, "El título", MAX_TEXTO, true);
        String descripcionLimpia = limpiar(descripcion, "La descripción", MAX_TEXTO, true);
        String categoriaLimpia = limpiar(categoria, "La categoría", MAX_TEXTO, false);
        validarPrioridad(prioridad);

        TareaSecundaria subtarea = new TareaSecundaria(categoriaLimpia, tituloLimpio, descripcionLimpia, false,
                prioridad, LocalDate.now());
        // si la tarea principal estaba completada, con una subtarea nueva deja de estarlo
        padre.setEstado(false);
        padre.insertarSubtarea(subtarea);
        padre.getProyecto().insertarTarea(subtarea);
        tareaRepository.save(subtarea);
        return padre.getProyecto().getId();
    }

    /** Marca/desmarca una tarea; si es principal, también todas sus subtareas. */
    public Long cambiarEstado(Long idTarea, boolean estado, Usuario usuario) {
        Tarea tarea = tareaPropia(idTarea, usuario);
        tarea.setEstado(estado);
        if (tarea instanceof TareaPrincipal principal) {
            for (TareaSecundaria sub : principal.getTareasSecundarias()) {
                sub.setEstado(estado);
            }
        }
        tareaRepository.save(tarea);
        return tarea.getProyecto().getId();
    }

    public Long borrarTarea(Long idTarea, Usuario usuario) {
        Tarea tarea = tareaPropia(idTarea, usuario);
        Proyecto proyecto = tarea.getProyecto();

        // se saca de las colecciones del proyecto/padre para que el borrado no se "re-guarde" por cascada
        if (tarea instanceof TareaPrincipal principal) {
            proyecto.getTareas().removeAll(principal.getTareasSecundarias());
        } else if (tarea instanceof TareaSecundaria sub && sub.getTareaPadre() != null) {
            sub.getTareaPadre().eliminarSubtarea(sub);
        }
        proyecto.getTareas().remove(tarea);
        tareaRepository.delete(tarea);
        return proyecto.getId();
    }

    // ---------- Auxiliares ----------

    private Tarea tareaPropia(Long id, Usuario usuario) {
        return tareaRepository.findById(id)
                .filter(t -> t.getProyecto() != null && esPropietario(t.getProyecto(), usuario))
                .orElseThrow(ProyectoService::noEncontrado);
    }

    private static boolean esPropietario(Proyecto proyecto, Usuario usuario) {
        return proyecto.getCreador() != null && proyecto.getCreador().getId().equals(usuario.getId());
    }

    private static ResponseStatusException noEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encuentra el recurso");
    }

    private static void validarPrioridad(int prioridad) {
        if (prioridad < PRIORIDAD_MIN || prioridad > PRIORIDAD_MAX) {
            throw new IllegalArgumentException(
                    "La prioridad debe estar entre " + PRIORIDAD_MIN + " y " + PRIORIDAD_MAX + ".");
        }
    }

    /** Quita espacios sobrantes y valida obligatoriedad y longitud. Un campo opcional vacío devuelve null. */
    private static String limpiar(String valor, String campo, int max, boolean obligatorio) {
        String limpio = (valor == null) ? "" : valor.trim();
        if (limpio.isEmpty()) {
            if (obligatorio) {
                throw new IllegalArgumentException(campo + " es obligatorio.");
            }
            return null;
        }
        if (limpio.length() > max) {
            throw new IllegalArgumentException(campo + " es demasiado largo (máximo " + max + " caracteres).");
        }
        return limpio;
    }
}
