package com.example.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
public class Proyecto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    //se le asigna un valor largo para permitir descripciones largas
    @Column(length = 1000)
    private String descripcion;

    @ManyToOne
    //esta es la clave foránea
    @JoinColumn(name = "usuario_id")
    @JsonIgnore
    private Usuario creador;

    //relación de tareas
    @OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL)
    private List<Tarea> tareas = new ArrayList<>();

    public Proyecto(){}

    public Proyecto(String nombre, String descripcion, Usuario creador) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.creador = creador;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public List<Tarea> getTareas() {
        return tareas;
    }

    public void setTareas(List<Tarea> tareas) {
        this.tareas = tareas;
    }

    public Usuario getCreador() {
        return creador;
    }

    public void setCreador(Usuario creador) {
        this.creador = creador;
    }

    public void insertarTarea(Tarea tarea) {
        tareas.add(tarea);
        tarea.setProyecto(this);
    }

    public void eliminarTarea(Tarea tarea) {
        tareas.remove(tarea);
        tarea.setProyecto(null);
    }

    public double getPorcentajeCompletado() {

        if(tareas.isEmpty()) return 0;

        double completadas = 0;

        for (Tarea tarea : tareas) {
            if (tarea.isEstado()) {
                completadas++;
            }
        }
        return (completadas / tareas.size()) *100;
    }

    public List<Tarea> ordenarPorPrioridad() {

            Collections.sort(tareas);

        return tareas;
    }

    public List<TareaPrincipal> getSoloTareasPrincipales() {
    List<TareaPrincipal> listaFiltrada = new ArrayList<>();
    
    if (this.tareas != null) {
        for (Tarea t : this.tareas) {
            if (t instanceof TareaPrincipal) {
                listaFiltrada.add((TareaPrincipal) t);
            }
        }
    }
    return listaFiltrada;
}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Proyecto: nombre: " + nombre + ", descripcion: " + descripcion + ", tareas: " + tareas + ", creador: "
                + creador;
    }

}
