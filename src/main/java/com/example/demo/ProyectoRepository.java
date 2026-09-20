package com.example.demo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long>{

    List<Proyecto> findByCreador(Usuario creador);

    List<Proyecto> findByNombreContainingIgnoreCaseAndCreador(String nombre, Usuario creador);

    Optional<Proyecto> findByNombreAndCreador(String nombre, Usuario creador);

}
