package com.example.demo;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_tarea")
public class Tarea implements Comparable<Tarea> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String description;
    private boolean estado;
    private int prioridad;
    private LocalDate fechaCreacion;

    @ManyToOne
    @JoinColumn(name = "proyecto_id")
    @JsonIgnore
    private Proyecto proyecto;
    
    public Tarea(){}

    public Tarea(String titulo, String description, boolean estado, int prioridad, LocalDate fechaCreacion) {
        this.titulo = titulo;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public void marcarDesmarcar() {
        if (this.estado) {
            this.estado = false;
        } else {
            this.estado = true;
        }
    }

    @Override
    public String toString() {
        return "Tarea: titulo: " + titulo + ", description: " + description + ", estado: " + estado + ", prioridad: "
                + prioridad + ", fechaCreacion: " + fechaCreacion;
    }

    @Override
    public int compareTo(Tarea o) {
       if (this.prioridad < o.prioridad) return -1;
        if (this.prioridad > o.prioridad) return 1;
        return 0;
    }

}
