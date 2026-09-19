package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>{

    Usuario findByNombreAndPassword(String nombre, String password);
    Usuario findByEmail(String email);

}
