package com.example.demo;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final boolean configurado;

    // Con @Value, Spring Boot va al application.properties y busca estas palabras exactas
    public CloudinaryService(
            @Value("${cloudinary.cloud_name}") String cloudName,
            @Value("${cloudinary.api_key}") String apiKey,
            @Value("${cloudinary.api_secret}") String apiSecret) {

        this.configurado = !cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank();
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    /** Sube la imagen y devuelve su URL https. Lanza una excepción si Cloudinary no está configurado o falla. */
    public String subirImagen(MultipartFile archivo) {
        if (!configurado) {
            throw new IllegalStateException("Cloudinary no está configurado (faltan las variables CLOUDINARY_*)");
        }
        try {
            // resource_type "image": Cloudinary rechaza cualquier archivo que no sea una imagen
            Map<?, ?> resultado = cloudinary.uploader().upload(archivo.getBytes(),
                    ObjectUtils.asMap("resource_type", "image", "folder", "fiumplan/perfiles"));
            return resultado.get("secure_url").toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Error al subir la imagen a Cloudinary", e);
        }
    }
}
