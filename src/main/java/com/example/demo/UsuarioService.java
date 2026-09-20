package com.example.demo;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private static final String IMAGEN_POR_DEFECTO =
            "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png";
    private static final long MAX_BYTES_IMAGEN = 5 * 1024 * 1024;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    // 8+ caracteres, al menos un número y una mayúscula (la misma regla que comprueba el formulario)
    private static final Pattern PASSWORD_SEGURA = Pattern.compile("^(?=.*\\d)(?=.*[A-Z]).{8,}$");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;
    private final EmailService emailService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          CloudinaryService cloudinaryService, EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.cloudinaryService = cloudinaryService;
        this.emailService = emailService;
    }

    /** El usuario que ha iniciado sesión; 401 si no hay sesión o el usuario ya no existe. */
    public Usuario actual(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName());
        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return usuario;
    }

    /** Registra un usuario nuevo. Lanza IllegalArgumentException con un mensaje apto para el usuario si algo no vale. */
    public Usuario registrar(String nombre, String email, String password, MultipartFile archivo) {
        String nombreLimpio = nombre == null ? "" : nombre.trim();
        String emailLimpio = email == null ? "" : email.trim();

        if (nombreLimpio.length() < 3 || nombreLimpio.length() > 100) {
            throw new IllegalArgumentException("El nombre debe tener entre 3 y 100 caracteres.");
        }
        if (emailLimpio.length() > 255 || !EMAIL.matcher(emailLimpio).matches()) {
            throw new IllegalArgumentException("El correo electrónico no es válido.");
        }
        // bcrypt solo usa los primeros 72 bytes, así que más largo no aporta nada
        if (password == null || !PASSWORD_SEGURA.matcher(password).matches()
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener entre 8 y 72 caracteres, con al menos un número y una mayúscula.");
        }
        if (usuarioRepository.findByEmail(emailLimpio) != null) {
            throw new IllegalArgumentException("Ese correo electrónico ya está en uso.");
        }

        Usuario usuario = new Usuario(nombreLimpio, passwordEncoder.encode(password), emailLimpio, Roles.normal);
        usuario.setImagenUrl(subirImagenOPorDefecto(archivo));

        try {
            usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            // doble clic / dos peticiones a la vez: la restricción única del email lo detiene
            throw new IllegalArgumentException("Ese correo electrónico ya está en uso.");
        }

        emailService.enviarCorreo(usuario.getEmail(), "Bienvenido a FiumPlan",
                "Hola " + usuario.getNombre() + ",\n\n"
                + "Tu registro se ha completado con éxito.\n"
                + "Ya puedes iniciar sesión y crear proyectos.\n\n"
                + "Un saludo.");
        return usuario;
    }

    private String subirImagenOPorDefecto(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return IMAGEN_POR_DEFECTO;
        }
        String tipo = archivo.getContentType();
        if (tipo == null || !tipo.startsWith("image/")) {
            throw new IllegalArgumentException("La foto de perfil debe ser una imagen.");
        }
        if (archivo.getSize() > MAX_BYTES_IMAGEN) {
            throw new IllegalArgumentException("La foto de perfil no puede superar los 5 MB.");
        }
        try {
            return cloudinaryService.subirImagen(archivo);
        } catch (RuntimeException e) {
            log.error("No se pudo subir la foto de perfil", e);
            throw new IllegalArgumentException(
                    "No se pudo subir la foto de perfil. Inténtalo de nuevo o regístrate sin foto.");
        }
    }
}
