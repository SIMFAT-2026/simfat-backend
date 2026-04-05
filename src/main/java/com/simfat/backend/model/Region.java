package com.simfat.backend.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "regions")
public class Region {

    @Id
    private String id;

    @NotBlank(message = "El nombre de la region es obligatorio")
    @Size(max = 120, message = "El nombre de la region no puede exceder 120 caracteres")
    private String nombre;

    @NotBlank(message = "El codigo de la region es obligatorio")
    @Size(max = 20, message = "El codigo de la region no puede exceder 20 caracteres")
    private String codigo;

    @NotBlank(message = "La zona de la region es obligatoria")
    @Size(max = 50, message = "La zona no puede exceder 50 caracteres")
    private String zona;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) {
        this.zona = zona;
    }
}

