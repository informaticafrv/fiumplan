package com.example.demo;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("SECUNDARIA")
public class TareaSecundaria extends Tarea {

    private String categoria;

    @ManyToOne
    @JoinColumn(name = "tarea_padre_id")
    @JsonIgnore
    private TareaPrincipal tareaPadre;

    public TareaSecundaria(){}

    public TareaSecundaria(String categoria, String titulo, String description, boolean estado, int prioridad, LocalDate fechaCreacion) {
        super(titulo, description, estado, prioridad, fechaCreacion);
        this.categoria = categoria;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public TareaPrincipal getTareaPadre(){
        return tareaPadre;
    }

    public void setTareaPadre(TareaPrincipal tareaPadre){
        this.tareaPadre = tareaPadre;
    }

    @Override
    public String toString() {
        return "categoria:" + categoria;
    }
    
}
