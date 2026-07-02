package com.pescadoresargentinos.rifas.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Rifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, unique = true)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(length = 2000)
    private String descripcion;

    @Column(nullable = false)
    private Integer cantidadNumeros;

    @Column(nullable = false)
    private Integer cantidadFilas;

    @Column(nullable = false)
    private Integer cantidadGanadores;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorNumero;

    @Column(nullable = false)
    private String aliasTransferencia;

    @Column(nullable = false)
    private String whatsappComprobante;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    private LocalDateTime fechaSorteo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoRifa estado = EstadoRifa.BORRADOR;

    @OneToMany(mappedBy = "rifa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Premio> premios = new ArrayList<>();

    @OneToMany(mappedBy = "rifa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NumeroRifa> numeros = new ArrayList<>();

    @OneToMany(mappedBy = "rifa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ganador> ganadores = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getCantidadNumeros() {
        return cantidadNumeros;
    }

    public void setCantidadNumeros(Integer cantidadNumeros) {
        this.cantidadNumeros = cantidadNumeros;
    }

    public Integer getCantidadFilas() {
        return cantidadFilas;
    }

    public void setCantidadFilas(Integer cantidadFilas) {
        this.cantidadFilas = cantidadFilas;
    }

    public Integer getCantidadGanadores() {
        return cantidadGanadores;
    }

    public void setCantidadGanadores(Integer cantidadGanadores) {
        this.cantidadGanadores = cantidadGanadores;
    }

    public BigDecimal getValorNumero() {
        return valorNumero;
    }

    public void setValorNumero(BigDecimal valorNumero) {
        this.valorNumero = valorNumero;
    }

    public String getAliasTransferencia() {
        return aliasTransferencia;
    }

    public void setAliasTransferencia(String aliasTransferencia) {
        this.aliasTransferencia = aliasTransferencia;
    }

    public String getWhatsappComprobante() {
        return whatsappComprobante;
    }

    public void setWhatsappComprobante(String whatsappComprobante) {
        this.whatsappComprobante = whatsappComprobante;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaSorteo() {
        return fechaSorteo;
    }

    public void setFechaSorteo(LocalDateTime fechaSorteo) {
        this.fechaSorteo = fechaSorteo;
    }

    public EstadoRifa getEstado() {
        return estado;
    }

    public void setEstado(EstadoRifa estado) {
        this.estado = estado;
    }

    public List<Premio> getPremios() {
        return premios;
    }

    public List<NumeroRifa> getNumeros() {
        return numeros;
    }

    public List<Ganador> getGanadores() {
        return ganadores;
    }
}
