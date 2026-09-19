package com.example.demo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("PRINCIPAL")
public class TareaPrincipal extends Tarea{

    @OneToMany(mappedBy = "tareaPadre", cascade = CascadeType.ALL)
    private List<TareaSecundaria> tareasSecundarias = new ArrayList<>();

    public TareaPrincipal(){}
    
    public TareaPrincipal(String titulo, String description, boolean estado, int prioridad, LocalDate fechaCreacion) {
        super(titulo, description, estado, prioridad, fechaCreacion);
    }
    public List<TareaSecundaria> getTareasSecundarias() {
        return tareasSecundarias;
    }
    public void setTareasSecundarias(List<TareaSecundaria> tareasSecundarias) {
        this.tareasSecundarias = tareasSecundarias;
    }
    public void insertarSubtarea(TareaSecundaria tarea){
        tarea.setTareaPadre(this);
        tareasSecundarias.add(tarea);
    }
    public void eliminarSubtarea(TareaSecundaria tarea){
        tareasSecundarias.remove(tarea);
    }
    public boolean tareasCompletadas(){
        for (TareaSecundaria tarea : tareasSecundarias) {
            if(!tarea.isEstado()){
                return false;
            }
        }
        return true;
    }
    
}
