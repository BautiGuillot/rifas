package com.pescadoresargentinos.rifas.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Rifa rifa;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Comprador comprador;

    @OneToMany(mappedBy = "compra")
    private List<NumeroRifa> numeros = new ArrayList<>();

    @ElementCollection
    private List<String> etiquetasNumeros = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCompra estado = EstadoCompra.PENDIENTE_PAGO;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime fechaExpiracion;

    private LocalDateTime fechaResolucion;

    private String comprobanteArchivo;

    private String comprobanteNombreOriginal;

    private String comprobanteContentType;

    @Column(nullable = false)
    private Boolean comprobanteWhatsapp = false;

    public Long getId() {
        return id;
    }

    public Rifa getRifa() {
        return rifa;
    }

    public void setRifa(Rifa rifa) {
        this.rifa = rifa;
    }

    public Comprador getComprador() {
        return comprador;
    }

    public void setComprador(Comprador comprador) {
        this.comprador = comprador;
    }

    public List<NumeroRifa> getNumeros() {
        return numeros;
    }

    public List<String> getEtiquetasNumeros() {
        return etiquetasNumeros;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public EstadoCompra getEstado() {
        return estado;
    }

    public void setEstado(EstadoCompra estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }

    public LocalDateTime getFechaResolucion() {
        return fechaResolucion;
    }

    public void setFechaResolucion(LocalDateTime fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }

    public String getComprobanteArchivo() {
        return comprobanteArchivo;
    }

    public void setComprobanteArchivo(String comprobanteArchivo) {
        this.comprobanteArchivo = comprobanteArchivo;
    }

    public String getComprobanteNombreOriginal() {
        return comprobanteNombreOriginal;
    }

    public void setComprobanteNombreOriginal(String comprobanteNombreOriginal) {
        this.comprobanteNombreOriginal = comprobanteNombreOriginal;
    }

    public String getComprobanteContentType() {
        return comprobanteContentType;
    }

    public void setComprobanteContentType(String comprobanteContentType) {
        this.comprobanteContentType = comprobanteContentType;
    }

    public Boolean getComprobanteWhatsapp() {
        return comprobanteWhatsapp;
    }

    public void setComprobanteWhatsapp(Boolean comprobanteWhatsapp) {
        this.comprobanteWhatsapp = comprobanteWhatsapp;
    }
}
