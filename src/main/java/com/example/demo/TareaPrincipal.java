package com.example.demo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("PRINCIPAL")
public class TareaPrincipal extends Tarea{

    @OneToMany(mappedBy = "tareaPadre", cascade = CascadeType.ALL)
    private List<TareaSecundaria> tareasSecundarias = new ArrayList<>();

    public TareaPrincipal(){}
    
    public TareaPrincipal(String titulo, String descripcion, boolean estado, int prioridad, LocalDate fechaCreacion) {
        super(titulo, descripcion, estado, prioridad, fechaCreacion);
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
    // copia ordenada (mayor prioridad primero); no modifica la lista real de la entidad
    public List<TareaSecundaria> getSubtareasPorPrioridad() {
        List<TareaSecundaria> copia = new ArrayList<>(tareasSecundarias);
        copia.sort(Comparator.comparingInt(TareaSecundaria::getPrioridad).reversed());
        return copia;
    }
    
}
