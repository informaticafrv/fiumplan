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
        try {
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            // Título
            Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
            fuenteTitulo.setSize(18);
            Paragraph titulo = new Paragraph("Detalles del Proyecto: " + proyecto.getNombre(), fuenteTitulo);
            titulo.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(titulo);

            document.add(new Paragraph(" ")); // Espacio vacío

            document.add(new Paragraph("Descripción: " + proyecto.getDescripcion()));
            if (proyecto.getCreador() != null) {
                document.add(new Paragraph("Creador: " + proyecto.getCreador().getNombre()));
            }
            document.add(new Paragraph(String.format("Progreso: %.0f%%", proyecto.getPorcentajeCompletado())));

            document.add(new Paragraph(" "));

            // Lista de tareas, con las subtareas indentadas debajo de su tarea principal
            Font fuenteTareas = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
            document.add(new Paragraph("Lista de Tareas:", fuenteTareas));

            for (TareaPrincipal tarea : proyecto.getSoloTareasPrincipales()) {
                document.add(new Paragraph(lineaTarea(tarea)));
                for (TareaSecundaria sub : tarea.getTareasSecundarias()) {
                    Paragraph linea = new Paragraph(lineaTarea(sub));
                    linea.setIndentationLeft(20);
                    document.add(linea);
                }
            }
        } catch (DocumentException e) {
            throw new IOException("No se pudo generar el PDF", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private String lineaTarea(Tarea tarea) {
        String estado = tarea.isEstado() ? "[X]" : "[ ]";
        return estado + " " + tarea.getTitulo() + " (Prioridad: " + tarea.getPrioridad() + ")";
    }
}
