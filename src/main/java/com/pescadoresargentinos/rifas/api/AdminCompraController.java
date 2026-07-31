package com.pescadoresargentinos.rifas.api;

import com.pescadoresargentinos.rifas.api.dto.ActualizarEstadoCompraRequest;
import com.pescadoresargentinos.rifas.api.dto.CompraResponse;
import com.pescadoresargentinos.rifas.dominio.EstadoCompra;
import com.pescadoresargentinos.rifas.servicio.CompraServicio;
import com.pescadoresargentinos.rifas.servicio.storage.ArchivoSeguro;
import com.pescadoresargentinos.rifas.servicio.storage.ComprobanteArchivo;
import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/compras")
public class AdminCompraController {

    private final CompraServicio compraServicio;

    public AdminCompraController(CompraServicio compraServicio) {
        this.compraServicio = compraServicio;
    }

    @GetMapping
    public List<CompraResponse> listar(@RequestParam(required = false) EstadoCompra estado) {
        return compraServicio.listar(estado);
    }

    @PatchMapping("/{id}/aprobar")
    public CompraResponse aprobar(@PathVariable Long id) {
        return compraServicio.aprobar(id);
    }

    @PatchMapping("/{id}/cancelar")
    public CompraResponse cancelar(@PathVariable Long id) {
        return compraServicio.cancelar(id);
    }

    @PatchMapping("/{id}/estado")
    public CompraResponse actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEstadoCompraRequest request
    ) {
        return compraServicio.actualizarEstado(id, request.estado());
    }

    @GetMapping("/{id}/comprobante")
    public ResponseEntity<?> abrirComprobante(@PathVariable Long id) {
        return compraServicio.urlDescargaComprobante(id)
                .<ResponseEntity<?>>map(uri -> ResponseEntity.status(302).location(uri).build())
                .orElseGet(() -> {
                    ComprobanteArchivo archivo = compraServicio.abrirComprobante(id);
                    boolean imagenSegura = ArchivoSeguro.esImagenRasterSegura(archivo.contentType());
                    MediaType contentType = imagenSegura
                            ? MediaType.parseMediaType(ArchivoSeguro.normalizarContentType(archivo.contentType()))
                            : MediaType.APPLICATION_OCTET_STREAM;
                    return ResponseEntity.ok()
                            .contentType(contentType)
                            .header(
                                    HttpHeaders.CONTENT_DISPOSITION,
                                    (imagenSegura ? "inline" : "attachment") + "; filename=\"comprobante"
                                            + ArchivoSeguro.extension(archivo.contentType()) + "\""
                            )
                            .header("Content-Security-Policy", "sandbox; default-src 'none'")
                            .body(archivo.resource());
                });
    }
}
