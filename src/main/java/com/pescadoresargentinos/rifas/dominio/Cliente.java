package com.pescadoresargentinos.rifas.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String colorPrincipal = "#082d50";

    private String logoUrl;

    @Column(nullable = false)
    private Boolean twilioWhatsappHabilitado = false;

    private String twilioWhatsappFrom;

    private String twilioMessagingServiceSid;

    private String twilioContentSid;

    private String whatsappConsultas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCliente estado = EstadoCliente.ACTIVO;

    @Column(nullable = false)
    private LocalDateTime fechaAlta = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getColorPrincipal() {
        return colorPrincipal;
    }

    public void setColorPrincipal(String colorPrincipal) {
        this.colorPrincipal = colorPrincipal;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public Boolean getTwilioWhatsappHabilitado() {
        return twilioWhatsappHabilitado;
    }

    public void setTwilioWhatsappHabilitado(Boolean twilioWhatsappHabilitado) {
        this.twilioWhatsappHabilitado = twilioWhatsappHabilitado;
    }

    public String getTwilioWhatsappFrom() {
        return twilioWhatsappFrom;
    }

    public void setTwilioWhatsappFrom(String twilioWhatsappFrom) {
        this.twilioWhatsappFrom = twilioWhatsappFrom;
    }

    public String getTwilioMessagingServiceSid() {
        return twilioMessagingServiceSid;
    }

    public void setTwilioMessagingServiceSid(String twilioMessagingServiceSid) {
        this.twilioMessagingServiceSid = twilioMessagingServiceSid;
    }

    public String getTwilioContentSid() {
        return twilioContentSid;
    }

    public void setTwilioContentSid(String twilioContentSid) {
        this.twilioContentSid = twilioContentSid;
    }

    public String getWhatsappConsultas() {
        return whatsappConsultas;
    }

    public void setWhatsappConsultas(String whatsappConsultas) {
        this.whatsappConsultas = whatsappConsultas;
    }

    public EstadoCliente getEstado() {
        return estado;
    }

    public void setEstado(EstadoCliente estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaAlta() {
        return fechaAlta;
    }
}
