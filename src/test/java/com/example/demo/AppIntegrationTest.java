package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba la aplicación entera (seguridad + controladores + BD H2 en memoria).
 * Cada test se ejecuta en una transacción que se deshace al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AppIntegrationTest {

    private static final String CLAVE = "Clave1234";

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired ProyectoRepository proyectoRepository;
    @Autowired TareaRepository tareaRepository;
    @Autowired PasswordEncoder passwordEncoder;

    // servicios externos: no queremos llamar a Cloudinary ni enviar correos de verdad
    @MockitoBean EmailService emailService;
    @MockitoBean CloudinaryService cloudinaryService;

    Usuario ana;
    Usuario beto;
    Proyecto proyectoDeAna;

    @BeforeEach
    void datos() {
        ana = usuarioRepository.save(new Usuario("Ana", passwordEncoder.encode(CLAVE), "ana@test.com", Roles.normal));
        beto = usuarioRepository.save(new Usuario("Beto", passwordEncoder.encode(CLAVE), "beto@test.com", Roles.normal));
        proyectoDeAna = proyectoRepository.save(new Proyecto("Proyecto de Ana", "secreto", ana));
    }

    // ---------- Acceso ----------

    @Test
    void sinSesionLaPaginaPrincipalRedirigeAlLogin() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void laApiExigeAutenticacion() throws Exception {
        mvc.perform(get("/api/proyectos")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/proyectos/" + proyectoDeAna.getId())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/proyectos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"x\",\"descripcion\":\"y\"}")).andExpect(status().isUnauthorized());
        assertThat(proyectoRepository.existsById(proyectoDeAna.getId())).isTrue();
    }

    @Test
    void apiConCredencialesDevuelveSoloProyectosPropios() throws Exception {
        proyectoRepository.save(new Proyecto("Proyecto de Beto", "otro", beto));

        mvc.perform(get("/api/proyectos").with(httpBasic("ana@test.com", CLAVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Proyecto de Ana"));
    }

    // ---------- Un usuario no puede tocar lo de otro ----------

    @Test
    void otroUsuarioNoPuedeBorrarEditarNiVerMiProyecto() throws Exception {
        Long id = proyectoDeAna.getId();

        mvc.perform(post("/borrarProyecto").param("id", id.toString()).with(user("beto@test.com")).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/editarProyecto").param("id", id.toString()).with(user("beto@test.com")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/verProyecto").param("id", id.toString()).with(user("beto@test.com")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/descargarPdf").param("id", id.toString()).with(user("beto@test.com")))
                .andExpect(status().isNotFound());
        mvc.perform(post("/guardarProyecto").param("id", id.toString()).param("nombre", "hackeado")
                .param("descripcion", "x").with(user("beto@test.com")).with(csrf()))
                .andExpect(status().isNotFound());

        assertThat(proyectoRepository.findById(id)).get().extracting(Proyecto::getNombre).isEqualTo("Proyecto de Ana");
    }

    @Test
    void otroUsuarioNoPuedeTocarMisTareas() throws Exception {
        TareaPrincipal tarea = tareaPrincipalDeAna();

        mvc.perform(post("/borrarTarea").param("id", tarea.getId().toString()).with(user("beto@test.com")).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/cambiarEstadoTarea").param("idTarea", tarea.getId().toString()).param("estado", "true")
                .with(user("beto@test.com")).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/insertarTareaSecundaria").param("idProyecto", proyectoDeAna.getId().toString())
                .param("idTareaPadre", tarea.getId().toString()).param("tituloSecundaria", "t")
                .param("descripcion", "d").param("prioridad", "1")
                .with(user("beto@test.com")).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/insertarTareaPrincipal").param("idProyecto", proyectoDeAna.getId().toString())
                .param("titulo", "t").param("descripcion", "d").param("prioridad", "1")
                .with(user("beto@test.com")).with(csrf())).andExpect(status().isNotFound());

        assertThat(tareaRepository.findById(tarea.getId())).get().extracting(Tarea::isEstado).isEqualTo(false);
        assertThat(tareaRepository.count()).isEqualTo(1);
    }

    @Test
    void laApiTampocoDejaTocarProyectosAjenos() throws Exception {
        String id = proyectoDeAna.getId().toString();

        mvc.perform(get("/api/proyectos/" + id).with(httpBasic("beto@test.com", CLAVE))).andExpect(status().isNotFound());
        mvc.perform(put("/api/proyectos/" + id).with(httpBasic("beto@test.com", CLAVE))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"hackeado\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/proyectos/" + id).with(httpBasic("beto@test.com", CLAVE))).andExpect(status().isNotFound());
        assertThat(proyectoRepository.existsById(proyectoDeAna.getId())).isTrue();
    }

    @Test
    void apiNoPermiteFijarElIdNiElCreador() throws Exception {
        // si el JSON trae el id de un proyecto ajeno, se ignora: se crea uno nuevo del usuario que llama
        mvc.perform(post("/api/proyectos").with(httpBasic("beto@test.com", CLAVE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + proyectoDeAna.getId() + ",\"nombre\":\"Mio\",\"descripcion\":\"d\"}"))
                .andExpect(status().isCreated());

        assertThat(proyectoRepository.findById(proyectoDeAna.getId())).get()
                .extracting(Proyecto::getNombre).isEqualTo("Proyecto de Ana");
        assertThat(proyectoRepository.findByCreador(beto)).extracting(Proyecto::getNombre).containsExactly("Mio");
    }

    @Test
    void apiCrearActualizarYBorrarPropio() throws Exception {
        mvc.perform(post("/api/proyectos").with(httpBasic("ana@test.com", CLAVE))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Nuevo\",\"descripcion\":\"d\"}"))
                .andExpect(status().isCreated());
        Proyecto nuevo = proyectoRepository.findByNombreAndCreador("Nuevo", ana).orElseThrow();

        // nombre repetido -> 409
        mvc.perform(post("/api/proyectos").with(httpBasic("ana@test.com", CLAVE))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Nuevo\",\"descripcion\":\"d\"}"))
                .andExpect(status().isConflict());
        // datos vacíos -> 400
        mvc.perform(post("/api/proyectos").with(httpBasic("ana@test.com", CLAVE))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"  \",\"descripcion\":\"d\"}"))
                .andExpect(status().isBadRequest());
        // PUT parcial: solo cambia la descripción
        mvc.perform(put("/api/proyectos/" + nuevo.getId()).with(httpBasic("ana@test.com", CLAVE))
                .contentType(MediaType.APPLICATION_JSON).content("{\"descripcion\":\"cambiada\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Nuevo"))
                .andExpect(jsonPath("$.descripcion").value("cambiada"));
        mvc.perform(delete("/api/proyectos/" + nuevo.getId()).with(httpBasic("ana@test.com", CLAVE)))
                .andExpect(status().isNoContent());
        assertThat(proyectoRepository.existsById(nuevo.getId())).isFalse();
    }

    // ---------- CSRF y métodos ----------

    @Test
    void borrarNoSePuedeHacerConGet() throws Exception {
        mvc.perform(get("/borrarProyecto").param("id", proyectoDeAna.getId().toString()).with(user("ana@test.com")))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/borrarTarea").param("id", "1").with(user("ana@test.com")))
                .andExpect(status().isMethodNotAllowed());
        assertThat(proyectoRepository.existsById(proyectoDeAna.getId())).isTrue();
    }

    @Test
    void borrarSinTokenCsrfEsRechazado() throws Exception {
        mvc.perform(post("/borrarProyecto").param("id", proyectoDeAna.getId().toString()).with(user("ana@test.com")))
                .andExpect(status().isForbidden());
        assertThat(proyectoRepository.existsById(proyectoDeAna.getId())).isTrue();
    }

    @Test
    void logoutPorGetNoCierraLaSesion() throws Exception {
        // GET /logout ya no es un logout: cae en la regla general y, al no haber sesión, pide login
        mvc.perform(get("/logout")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/logout").with(user("ana@test.com")).with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void soloUnAdminEntraEnElPanelDeAdmin() throws Exception {
        mvc.perform(get("/admin/dashboard").with(user("ana@test.com").authorities(() -> "normal")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/dashboard").with(user("root@test.com").authorities(() -> "admin")))
                .andExpect(status().isOk());
        mvc.perform(post("/admin/borrarProyecto").param("id", proyectoDeAna.getId().toString())
                .with(user("ana@test.com").authorities(() -> "normal")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void lasPaginasIncluyenElTokenCsrfEnLogoutYBorrado() throws Exception {
        // sin este token los botones "Cerrar sesión" y "Sí, borrar" darían 403 en el navegador
        for (String pagina : new String[] {"/", "/verProyecto?id=" + proyectoDeAna.getId()}) {
            String html = mvc.perform(get(pagina).with(user("ana@test.com")))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(html).contains("id=\"formBorrado\"");
            assertThat(html.split("name=\"_csrf\"", -1).length - 1).as("tokens CSRF en " + pagina).isGreaterThanOrEqualTo(1);
        }
        String inicio = mvc.perform(get("/").with(user("ana@test.com"))).andReturn().getResponse().getContentAsString();
        assertThat(inicio.split("name=\"_csrf\"", -1).length - 1).isGreaterThanOrEqualTo(2); // logout + modal
    }

    // ---------- Flujo normal de proyectos y tareas ----------

    @Test
    void flujoCompletoDeProyectoYTareas() throws Exception {
        Long idProyecto = proyectoDeAna.getId();

        // crear proyecto por el formulario
        mvc.perform(post("/guardarProyecto").param("nombre", "  Web  ").param("descripcion", "d")
                .with(user("ana@test.com")).with(csrf())).andExpect(redirectedUrl("/"));
        assertThat(proyectoRepository.findByNombreAndCreador("Web", ana)).isPresent();

        // nombre repetido: vuelve al formulario con error y no crea otro
        mvc.perform(post("/guardarProyecto").param("nombre", "Web").param("descripcion", "d")
                .with(user("ana@test.com")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ya tienes un proyecto llamado")));
        assertThat(proyectoRepository.findByCreador(ana)).hasSize(2);

        // tarea principal + subtarea
        mvc.perform(post("/insertarTareaPrincipal").param("idProyecto", idProyecto.toString())
                .param("titulo", "Principal").param("descripcion", "d").param("prioridad", "3")
                .with(user("ana@test.com")).with(csrf())).andExpect(redirectedUrl("/verProyecto?id=" + idProyecto));
        TareaPrincipal principal = tareaRepository.findAll().stream()
                .filter(TareaPrincipal.class::isInstance).map(TareaPrincipal.class::cast).findFirst().orElseThrow();
        mvc.perform(post("/insertarTareaSecundaria").param("idProyecto", idProyecto.toString())
                .param("idTareaPadre", principal.getId().toString()).param("tituloSecundaria", "Sub")
                .param("descripcion", "d").param("prioridad", "5").param("categoria", "bug")
                .with(user("ana@test.com")).with(csrf())).andExpect(redirectedUrl("/verProyecto?id=" + idProyecto));
        assertThat(tareaRepository.count()).isEqualTo(2);

        // prioridad fuera de rango: no se crea y se avisa
        mvc.perform(post("/insertarTareaPrincipal").param("idProyecto", idProyecto.toString())
                .param("titulo", "x").param("descripcion", "d").param("prioridad", "99")
                .with(user("ana@test.com")).with(csrf())).andExpect(redirectedUrl("/verProyecto?id=" + idProyecto));
        assertThat(tareaRepository.count()).isEqualTo(2);

        // la página del proyecto se pinta (también ordenada por prioridad) y el PDF se genera
        mvc.perform(get("/verProyecto").param("id", idProyecto.toString()).with(user("ana@test.com")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Sub")));
        mvc.perform(get("/verProyecto").param("id", idProyecto.toString()).param("orden", "prioridad")
                .with(user("ana@test.com"))).andExpect(status().isOk());
        mvc.perform(get("/descargarPdf").param("id", idProyecto.toString()).with(user("ana@test.com")))
                .andExpect(status().isOk()).andExpect(header().string("Content-Type", "application/pdf"));

        // marcar la principal completa también la subtarea
        mvc.perform(post("/cambiarEstadoTarea").param("idTarea", principal.getId().toString()).param("estado", "true")
                .with(user("ana@test.com")).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(tareaRepository.findAll()).allMatch(Tarea::isEstado);

        // borrar la principal borra también la subtarea
        mvc.perform(post("/borrarTarea").param("id", principal.getId().toString())
                .with(user("ana@test.com")).with(csrf())).andExpect(redirectedUrl("/verProyecto?id=" + idProyecto));
        assertThat(tareaRepository.count()).isZero();

        // y borrar el proyecto
        mvc.perform(post("/borrarProyecto").param("id", idProyecto.toString()).with(user("ana@test.com")).with(csrf()))
                .andExpect(redirectedUrl("/"));
        assertThat(proyectoRepository.existsById(idProyecto)).isFalse();
    }

    @Test
    void nombresConCaracteresEspecialesFuncionanPorqueSeUsaElId() throws Exception {
        Proyecto raro = proyectoRepository.save(new Proyecto("Mates & Física #1", "d", ana));
        mvc.perform(post("/insertarTareaPrincipal").param("idProyecto", raro.getId().toString())
                .param("titulo", "t").param("descripcion", "d").param("prioridad", "1")
                .with(user("ana@test.com")).with(csrf())).andExpect(redirectedUrl("/verProyecto?id=" + raro.getId()));
        mvc.perform(get("/verProyecto").param("id", raro.getId().toString()).with(user("ana@test.com")))
                .andExpect(status().isOk());
    }

    // ---------- Registro ----------

    @Test
    void registroValidoCreaUsuarioConContrasenaCifradaYEnviaCorreo() throws Exception {
        mvc.perform(post("/insertarUsuario").param("nombre", "Carla").param("email", "carla@test.com")
                .param("password", "Segura123").with(csrf())).andExpect(redirectedUrl("/login"));

        Usuario carla = usuarioRepository.findByEmail("carla@test.com");
        assertThat(carla).isNotNull();
        assertThat(carla.getRol()).isEqualTo(Roles.normal);
        assertThat(carla.getPassword()).isNotEqualTo("Segura123");
        assertThat(passwordEncoder.matches("Segura123", carla.getPassword())).isTrue();
        verify(emailService).enviarCorreo(anyString(), anyString(), anyString());
    }

    @Test
    void registroRechazaDatosNoValidos() throws Exception {
        // contraseña débil, email inválido, nombre corto, email repetido
        registroFallido("Carla", "carla@test.com", "debil");
        registroFallido("Carla", "no-es-un-email", "Segura123");
        registroFallido("Ca", "carla@test.com", "Segura123");
        registroFallido("Otra Ana", "ana@test.com", "Segura123");
        assertThat(usuarioRepository.count()).isEqualTo(2);
    }

    @Test
    void registroRechazaArchivosQueNoSonImagenes() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("archivo", "virus.exe", "application/octet-stream", new byte[] {1, 2});
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/insertarUsuario")
                .file(pdf).param("nombre", "Carla").param("email", "carla@test.com").param("password", "Segura123")
                .with(csrf())).andExpect(redirectedUrl("/registro"));
        assertThat(usuarioRepository.findByEmail("carla@test.com")).isNull();
        org.mockito.Mockito.verifyNoInteractions(cloudinaryService);
    }

    @Test
    void siFallaCloudinaryElRegistroAvisaEnLugarDeDarError500() throws Exception {
        org.mockito.Mockito.when(cloudinaryService.subirImagen(any())).thenThrow(new IllegalStateException("caído"));
        MockMultipartFile foto = new MockMultipartFile("archivo", "yo.png", "image/png", new byte[] {1, 2});

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/insertarUsuario")
                .file(foto).param("nombre", "Carla").param("email", "carla@test.com").param("password", "Segura123")
                .with(csrf())).andExpect(redirectedUrl("/registro"));
        assertThat(usuarioRepository.findByEmail("carla@test.com")).isNull();
    }

    // ---------- Auxiliares ----------

    private void registroFallido(String nombre, String email, String password) throws Exception {
        mvc.perform(post("/insertarUsuario").param("nombre", nombre).param("email", email)
                .param("password", password).with(csrf())).andExpect(redirectedUrl("/registro"));
    }

    private TareaPrincipal tareaPrincipalDeAna() {
        TareaPrincipal tarea = new TareaPrincipal("Principal", "d", false, 1, java.time.LocalDate.now());
        proyectoDeAna.insertarTarea(tarea);
        return tareaRepository.saveAndFlush(tarea);
    }
}
