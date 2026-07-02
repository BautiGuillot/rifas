package com.pescadoresargentinos.rifas.api;

import com.pescadoresargentinos.rifas.api.dto.CompraResponse;
import com.pescadoresargentinos.rifas.api.dto.CrearCompraRequest;
import com.pescadoresargentinos.rifas.api.dto.RifaDetalleResponse;
import com.pescadoresargentinos.rifas.api.dto.RifaResumenResponse;
import com.pescadoresargentinos.rifas.servicio.CompraServicio;
import com.pescadoresargentinos.rifas.servicio.RifaServicio;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/rifas")
public class RifaPublicaController {

    private final RifaServicio rifaServicio;
    private final CompraServicio compraServicio;

    public RifaPublicaController(RifaServicio rifaServicio, CompraServicio compraServicio) {
        this.rifaServicio = rifaServicio;
        this.compraServicio = compraServicio;
    }

    @GetMapping
    public List<RifaResumenResponse> listarPublicadas() {
        return rifaServicio.listarPublicadas();
    }

    @GetMapping("/finalizadas")
    public List<RifaResumenResponse> listarFinalizadas() {
        return rifaServicio.listarFinalizadas();
    }

    @GetMapping("/{id}")
    public RifaDetalleResponse detalle(@PathVariable Long id) {
        return rifaServicio.detalle(id);
    }

    @GetMapping("/slug/{slug}")
    public RifaDetalleResponse detallePorSlug(@PathVariable String slug) {
        return rifaServicio.detallePublicoPorSlug(slug);
    }

    @PostMapping("/{id}/compras")
    public CompraResponse comprar(@PathVariable Long id, @Valid @RequestBody CrearCompraRequest request) {
        return compraServicio.crear(id, request);
    }

    @PostMapping("/slug/{slug}/compras")
    public CompraResponse comprarPorSlug(@PathVariable String slug, @Valid @RequestBody CrearCompraRequest request) {
        return compraServicio.crearPorSlug(slug, request);
    }

    @PostMapping("/compras/{id}/comprobante")
    public CompraResponse cargarComprobante(@PathVariable Long id, @RequestPart("archivo") MultipartFile archivo) {
        return compraServicio.cargarComprobante(id, archivo);
    }

    @PostMapping("/compras/{id}/comprobante-whatsapp")
    public CompraResponse marcarComprobanteWhatsapp(@PathVariable Long id) {
        return compraServicio.marcarComprobanteEnviadoPorWhatsapp(id);
    }

    @PostMapping("/compras/{id}/expirar")
    public CompraResponse expirarCompra(@PathVariable Long id) {
        return compraServicio.expirarSiVencida(id);
    }
}
