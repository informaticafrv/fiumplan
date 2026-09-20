package com.example.demo;

public class NombreDuplicadoException extends IllegalArgumentException {

    public NombreDuplicadoException(String nombre) {
        super("Ya tienes un proyecto llamado '" + nombre + "'. Elige otro nombre.");
    }
}
