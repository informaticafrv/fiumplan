package com.example.demo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long>{

    Proyecto findByNombre(String nombre);
    List<Proyecto> findByCreador(Usuario creador);

    List<Proyecto> findByNombreContainingIgnoreCaseAndCreador(String nombre, Usuario creador);

    Proyecto findByNombreAndCreador(String nombre, Usuario creador);

}
