package com.example.demo;
import java.util.List;

//usamos el * para importar toda la librería jakarta.persistence (probablemente no sea lo mejor, investigar)
import jakarta.persistence.*;

//convertimos la clase en una entidad
@Entity
public class Usuario {
    //metemos un atributo id para identificar los usuarios
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String password;
    //este campo lo he tenido que poner único para que al darle dos veces antes cargar la siguiente página en registrar no se creen dos registros igules
    @Column(unique = true)
    private String email;
    private String imagenUrl;
    //usamos @enumerated para que no tome el valor del enum como binario sino como string
    @Enumerated(EnumType.STRING)
    private Roles rol;
    //creamos una relación entre la entidad usuarios y la de proyecto
    @OneToMany(mappedBy = "creador", cascade = CascadeType.ALL)
    private List<Proyecto> proyectos;
    //para JPA tiene que haber un constructor vacío
    public Usuario(){}

    public Usuario(String nombre, String password, String email, Roles rol) {
        this.nombre = nombre;
        this.password = password;
        this.email = email;
        this.rol = rol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    public String getPassword(){
        return password;
    }
    public void setPassword(String pasword){
        this.password = pasword;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Roles getRol() {
        return rol;
    }

    public void setRol(Roles rol) {
        this.rol = rol;
    }

    //se crean getters y setters de id y de la list de proyectos del usuario

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Proyecto> getProyectos() {
        return proyectos;
    }

    public void setProyectos(List<Proyecto> proyectos) {
        this.proyectos = proyectos;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    @Override
    public String toString() {
        return "usuario: nombre: " + nombre + ", email: " + email + ", rol: " + rol;
    }
}
