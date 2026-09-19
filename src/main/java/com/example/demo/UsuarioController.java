package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.crypto.password.PasswordEncoder;

@Controller
public class UsuarioController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CloudinaryService cloudinaryService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }
    @RequestMapping("/registro")
    public String mostrarRegistro() {
        return "registro";
    }
    

    /*@RequestMapping("/procesarLogin")
    public String procesarLogin(@RequestParam(name = "nombre") String nombre, @RequestParam(name = "password") String password, HttpSession session) {
        //he sustituido todo el método que tenía hecho por que con la base de datos se simplifica mucho
       Usuario usuarioLogeado = usuarioRepository.findByNombreAndPassword(nombre, password);
       if(usuarioLogeado != null){
        session.setAttribute("usuario", usuarioLogeado);
        return "redirect:/";
       }else{
        return "redirect:/registro?error=true";
       }
    }*/
    @PostMapping("/insertarUsuario")
    public String procesarRegistro(@RequestParam(name = "nombre") String nombre, @RequestParam(name = "email") String email, @RequestParam(name = "password") String password, @RequestParam("archivo") MultipartFile archivo, RedirectAttributes redirectAttributes){
        
        Usuario usuarioExistente = usuarioRepository.findByEmail(email);
        if (usuarioExistente != null) {
            // Si ya hay un usuario con ese correo, le devolvemos al formulario con un error
            redirectAttributes.addFlashAttribute("error", "Ese correo electrónico ya está en uso.");
            return "redirect:/registro"; 
        }

        Usuario nuevoUsuario = new Usuario (nombre, password, email, Roles.normal);

        String passEncriptada = passwordEncoder.encode(nuevoUsuario.getPassword());
        nuevoUsuario.setPassword(passEncriptada);

        if (!archivo.isEmpty()) {
            // Si el usuario subió algo, lo mandamos a Cloudinary
            String url = cloudinaryService.subirImagen(archivo);
            nuevoUsuario.setImagenUrl(url);
        } else {
            // Si NO subió nada, ponemos una foto por defecto (esta es una imagen genérica de internet)
            nuevoUsuario.setImagenUrl("https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png");
        }

        usuarioRepository.save(nuevoUsuario);

        // 2. ENVIAR EL CORREO (Añade esto justo aquí)
        // Usamos el email que el usuario escribió en el formulario
        String asunto = "Bienvenido a FiumPlan";
        String mensaje = "Hola " + nuevoUsuario.getNombre() + ",\n\n" +
                         "Tu registro se ha completado con éxito.\n" +
                         "Ya puedes iniciar sesión y crear proyectos.\n\n" +
                         "Un saludo.";
                         
        emailService.enviarCorreo(nuevoUsuario.getEmail(), asunto, mensaje);

        return "redirect:/login";
    }
    
    /*public ArrayList<Usuario> rescatarUsuarios(HttpServletRequest request){

        ArrayList<Usuario> listaUsuarios = new ArrayList<>();
        if(request.getCookies() != null){
            for (Cookie cookie : request.getCookies()) {
                if(cookie.getName().equals("usuarios")){
                    String valor = cookie.getValue();
                    String[] usuariosSeparados = valor.split("\\|");
                    for (String datosU : usuariosSeparados) {
                        String[] partes = datosU.split("_");
                        if(partes.length == 3){
                            listaUsuarios.add(new Usuario(partes[0], partes[1], partes[2], Roles.normal));
                        }
                    }
                }
            }
        }
        return listaUsuarios;
    }*/
    /*@RequestMapping("/cerrarSesion")
    public String cerrarSesion(HttpSession session) {
       session.invalidate();
        return "login";
    }*/
    
}
