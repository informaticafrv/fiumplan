package com.example.demo;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_tarea")
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    // el campo se llama descripcion en Java pero mantiene la columna "description" para no romper la BD existente
    @Column(name = "description")
    private String descripcion;
    private boolean estado;
    private int prioridad;
    private LocalDate fechaCreacion;

    @ManyToOne
    @JoinColumn(name = "proyecto_id")
    @JsonIgnore
    private Proyecto proyecto;
    
    public Tarea(){}

    public Tarea(String titulo, String descripcion, boolean estado, int prioridad, LocalDate fechaCreacion) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.estado = estado;
        this.prioridad = prioridad;
        this.fechaCreacion = fechaCreacion;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(int prioridad) {
        this.prioridad = prioridad;
    }

    public LocalDate getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDate fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
    public Proyecto getProyecto(){
        return proyecto;
    }
    public void setProyecto(Proyecto proyecto){
        this.proyecto = proyecto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Tarea: titulo: " + titulo + ", descripcion: " + descripcion + ", estado: " + estado + ", prioridad: "
                + prioridad + ", fechaCreacion: " + fechaCreacion;
    }

}
