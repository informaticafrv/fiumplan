package com.example.demo;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Service
public class PdfService {

    public void exportarProyecto(HttpServletResponse response, Proyecto proyecto) throws IOException {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();
        
        // Título
        Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        fuenteTitulo.setSize(18);
        Paragraph titulo = new Paragraph("Detalles del Proyecto: " + proyecto.getNombre(), fuenteTitulo);
        titulo.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(titulo);
        
        document.add(new Paragraph(" ")); // Espacio vacío

        // Descripción
        document.add(new Paragraph("Descripción: " + proyecto.getDescripcion()));
        document.add(new Paragraph("Creador: " + proyecto.getCreador().getNombre()));
        document.add(new Paragraph("Progreso: " + proyecto.getPorcentajeCompletado() + "%"));
        
        document.add(new Paragraph(" ")); 

        // Lista de Tareas
        Font fuenteTareas = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        document.add(new Paragraph("Lista de Tareas:", fuenteTareas));

        for (Tarea tarea : proyecto.getTareas()) {
            String estado = tarea.isEstado() ? "[X]" : "[ ]";
            document.add(new Paragraph(estado + " " + tarea.getTitulo() + " (Prioridad: " + tarea.getPrioridad() + ")"));
        }

        document.close();
    }
}
