package com.pescadoresargentinos.rifas.servicio;

import com.pescadoresargentinos.rifas.api.dto.CompraResponse;
import com.pescadoresargentinos.rifas.api.dto.CompraSeguimientoResponse;
import com.pescadoresargentinos.rifas.api.dto.CrearCompraRequest;
import com.pescadoresargentinos.rifas.dominio.Compra;
import com.pescadoresargentinos.rifas.dominio.Comprador;
import com.pescadoresargentinos.rifas.dominio.EstadoCompra;
import com.pescadoresargentinos.rifas.dominio.EstadoNumero;
import com.pescadoresargentinos.rifas.dominio.EstadoRifa;
import com.pescadoresargentinos.rifas.dominio.NumeroRifa;
import com.pescadoresargentinos.rifas.dominio.Rifa;
import com.pescadoresargentinos.rifas.repositorio.CompraRepositorio;
import com.pescadoresargentinos.rifas.repositorio.NumeroRifaRepositorio;
import com.pescadoresargentinos.rifas.repositorio.RifaRepositorio;
import com.pescadoresargentinos.rifas.seguridad.UsuarioActual;
import com.pescadoresargentinos.rifas.servicio.storage.ComprobanteArchivo;
import com.pescadoresargentinos.rifas.servicio.storage.ComprobanteGuardado;
import com.pescadoresargentinos.rifas.servicio.storage.ComprobanteStorage;
import com.pescadoresargentinos.rifas.servicio.twilio.TwilioEnvioResultado;
import com.pescadoresargentinos.rifas.servicio.twilio.TwilioWhatsappServicio;
import com.pescadoresargentinos.rifas.util.TelefonoArgentina;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CompraServicio {

    private final CompraRepositorio compraRepositorio;
    private final RifaRepositorio rifaRepositorio;
    private final NumeroRifaRepositorio numeroRifaRepositorio;
    private final UsuarioActual usuarioActual;
    private final ComprobanteStorage comprobanteStorage;
    private final TwilioWhatsappServicio twilioWhatsappServicio;

    public CompraServicio(
            CompraRepositorio compraRepositorio,
            RifaRepositorio rifaRepositorio,
            NumeroRifaRepositorio numeroRifaRepositorio,
            UsuarioActual usuarioActual,
            ComprobanteStorage comprobanteStorage,
            TwilioWhatsappServicio twilioWhatsappServicio
    ) {
        this.compraRepositorio = compraRepositorio;
        this.rifaRepositorio = rifaRepositorio;
        this.numeroRifaRepositorio = numeroRifaRepositorio;
        this.usuarioActual = usuarioActual;
        this.comprobanteStorage = comprobanteStorage;
        this.twilioWhatsappServicio = twilioWhatsappServicio;
    }

    @Transactional
    public CompraResponse crear(Long rifaId, CrearCompraRequest request) {
        Rifa rifa = rifaRepositorio.findById(rifaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la rifa " + rifaId));
        if (rifa.getEstado() != EstadoRifa.PUBLICADA) {
            throw new IllegalStateException("La rifa no esta disponible para compras");
        }

        List<Integer> valores = request.numeros().stream().distinct().sorted().toList();
        if (valores.size() != request.numeros().size()) {
            throw new IllegalArgumentException("Hay numeros repetidos en la compra");
        }
        validarRango(rifa, valores);

        List<NumeroRifa> numeros = numeroRifaRepositorio.findByRifaIdAndValorIn(rifaId, valores);
        if (numeros.size() != valores.size()) {
            throw new IllegalArgumentException("Uno o mas numeros no existen");
        }
        numeros.forEach(numero -> {
            if (numero.getEstado() != EstadoNumero.DISPONIBLE) {
                throw new IllegalStateException("El numero " + numero.getEtiqueta() + " ya no esta disponible");
            }
        });

        Comprador comprador = new Comprador();
        comprador.setNombre(request.nombre());
        comprador.setTelefono(normalizarTelefonoComprador(request.telefono()));

        Compra compra = new Compra();
        compra.setRifa(rifa);
        compra.setComprador(comprador);
        compra.setTotal(rifa.getValorNumero().multiply(java.math.BigDecimal.valueOf(numeros.size())));
        compra.setFechaExpiracion(LocalDateTime.now().plusMinutes(5));
        compra.setTokenSeguimiento(UUID.randomUUID().toString());
        compra = compraRepositorio.save(compra);

        for (NumeroRifa numero : numeros) {
            numero.setEstado(EstadoNumero.PENDIENTE);
            numero.setCompra(compra);
            compra.getNumeros().add(numero);
            compra.getEtiquetasNumeros().add(etiquetaCompra(numero));
        }

        registrarEnvioWhatsappAutomatico(compra);

        return aResponse(compra);
    }

    @Transactional
    public CompraResponse crearPorSlug(String slug, CrearCompraRequest request) {
        Rifa rifa = rifaRepositorio.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("No existe la rifa " + slug));
        return crear(rifa.getId(), request);
    }

    @Transactional(readOnly = true)
    public List<CompraResponse> listar(EstadoCompra estado) {
        Long clienteId = usuarioActual.clienteId();
        List<Compra> compras = estado == null
                ? compraRepositorio.findByRifaClienteIdOrderByFechaCreacionDesc(clienteId)
                : compraRepositorio.findByRifaClienteIdAndEstadoOrderByFechaCreacionDesc(clienteId, estado);
        return compras.stream().map(this::aResponse).toList();
    }

    @Transactional
    public CompraResponse aprobar(Long compraId) {
        Compra compra = buscarCompra(compraId);
        validarPropiedad(compra);
        if (compra.getEstado() != EstadoCompra.PENDIENTE_PAGO) {
            throw new IllegalStateException("Solo se pueden aprobar compras pendientes");
        }
        compra.setEstado(EstadoCompra.APROBADA);
        compra.setFechaResolucion(LocalDateTime.now());
        compra.getNumeros().forEach(numero -> numero.setEstado(EstadoNumero.VENDIDO));
        return aResponse(compra);
    }

    @Transactional
    public CompraResponse cancelar(Long compraId) {
        Compra compra = buscarCompra(compraId);
        validarPropiedad(compra);
        if (compra.getEstado() != EstadoCompra.PENDIENTE_PAGO) {
            throw new IllegalStateException("Solo se pueden cancelar compras pendientes");
        }
        cancelarPendiente(compra);
        return aResponse(compra);
    }

    @Transactional
    public CompraResponse cargarComprobante(Long compraId, MultipartFile archivo) {
        Compra compra = buscarCompra(compraId);
        cancelarSiVencidaSinComprobante(compra);
        if (compra.getEstado() != EstadoCompra.PENDIENTE_PAGO) {
            throw new IllegalStateException("Solo se puede cargar comprobante en compras pendientes");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El comprobante es obligatorio");
        }

        validarArchivoComprobante(archivo);
        ComprobanteGuardado comprobante = comprobanteStorage.guardar(compraId, archivo);
        compra.setComprobanteArchivo(comprobante.referencia());
        compra.setComprobanteNombreOriginal(comprobante.nombreOriginal());
        compra.setComprobanteContentType(comprobante.contentType());
        return aResponse(compra);
    }

    @Transactional
    public CompraResponse marcarComprobanteEnviadoPorWhatsapp(Long compraId) {
        Compra compra = buscarCompra(compraId);
        cancelarSiVencidaSinComprobante(compra);

        // La pantalla publica puede quedar unos segundos desactualizada mientras
        // llega el comprobante por el webhook o el admin aprueba la compra.
        // Informar el comprobante debe ser seguro de repetir en esos casos.
        if (compra.getEstado() == EstadoCompra.APROBADA) {
            return aResponse(compra);
        }
        if (compra.getEstado() != EstadoCompra.PENDIENTE_PAGO) {
            throw new IllegalStateException("Solo se puede informar comprobante en compras pendientes");
        }
        if (compra.getComprobanteArchivo() != null || Boolean.TRUE.equals(compra.getComprobanteWhatsapp())) {
            return aResponse(compra);
        }
        compra.setComprobanteWhatsapp(true);
        return aResponse(compra);
    }

    @Transactional
    public boolean registrarComprobanteDesdeWhatsapp(Long compraId, String nombreOriginal, String contentType, byte[] contenido) {
        Compra compra = buscarCompra(compraId);
        cancelarSiVencidaSinComprobante(compra);
        if (compra.getEstado() != EstadoCompra.PENDIENTE_PAGO) {
            return false;
        }
        validarArchivoComprobante(contentType, contenido == null ? 0 : contenido.length);
        ComprobanteGuardado comprobante = comprobanteStorage.guardar(compraId, nombreOriginal, contentType, contenido);
        compra.setComprobanteArchivo(comprobante.referencia());
        compra.setComprobanteNombreOriginal(comprobante.nombreOriginal());
        compra.setComprobanteContentType(comprobante.contentType());
        compra.setComprobanteWhatsapp(true);
        return true;
    }

    @Transactional
    public Optional<Compra> buscarCompraPendientePorWhatsapp(String twilioFrom, String compradorTelefono) {
        return compraRepositorio.findFirstByRifaClienteTwilioWhatsappFromAndCompradorTelefonoAndEstadoOrderByFechaCreacionDesc(
                normalizarTelefonoWhatsapp(twilioFrom),
                normalizarTelefonoWhatsapp(compradorTelefono),
                EstadoCompra.PENDIENTE_PAGO
        );
    }

    @Transactional(readOnly = true)
    public Optional<Compra> buscarCompraPendientePorId(Long compraId) {
        return compraRepositorio.findById(compraId)
                .filter(compra -> compra.getEstado() == EstadoCompra.PENDIENTE_PAGO);
    }

    @Transactional
    public CompraResponse expirarSiVencida(Long compraId) {
        Compra compra = buscarCompra(compraId);
        cancelarSiVencidaSinComprobante(compra);
        return aResponse(compra);
    }

    @Transactional
    public CompraSeguimientoResponse seguimientoPublico(Long compraId, String tokenSeguimiento) {
        Compra compra = buscarCompra(compraId);
        if (tokenSeguimiento == null || !tokenSeguimiento.equals(compra.getTokenSeguimiento())) {
            throw new SecurityException("No tenes permiso para consultar esta compra");
        }
        cancelarSiVencidaSinComprobante(compra);
        return new CompraSeguimientoResponse(
                compra.getEstado(),
                compra.getComprobanteArchivo() != null || Boolean.TRUE.equals(compra.getComprobanteWhatsapp())
        );
    }

    @Transactional(readOnly = true)
    public Optional<URI> urlDescargaComprobante(Long compraId) {
        Compra compra = buscarCompra(compraId);
        validarPropiedad(compra);
        validarTieneComprobanteArchivo(compra);
        return comprobanteStorage.urlDescarga(compra.getComprobanteArchivo(), compra.getComprobanteNombreOriginal());
    }

    @Transactional(readOnly = true)
    public ComprobanteArchivo abrirComprobante(Long compraId) {
        Compra compra = buscarCompra(compraId);
        validarPropiedad(compra);
        validarTieneComprobanteArchivo(compra);
        return comprobanteStorage.abrir(
                        compra.getComprobanteArchivo(),
                        compra.getComprobanteNombreOriginal(),
                        compra.getComprobanteContentType()
                )
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el comprobante"));
    }

    @Scheduled(fixedDelayString = "${app.compras.expiracion-check-ms:30000}")
    @Transactional
    public void cancelarPendientesVencidas() {
        compraRepositorio.findByEstadoAndComprobanteArchivoIsNullAndComprobanteWhatsappFalseAndFechaExpiracionBefore(EstadoCompra.PENDIENTE_PAGO, LocalDateTime.now())
                .forEach(this::cancelarPendiente);
    }

    private void validarRango(Rifa rifa, List<Integer> valores) {
        int minimo = rifa.getNumeroInicial();
        int maximo = minimo + rifa.getCantidadFilas() - 1;
        valores.forEach(valor -> {
            if (valor < minimo || valor > maximo) {
                throw new IllegalArgumentException("El numero " + valor + " esta fuera del rango de la rifa");
            }
        });
    }

    private void cancelarSiVencidaSinComprobante(Compra compra) {
        if (compra.getEstado() == EstadoCompra.PENDIENTE_PAGO
                && compra.getComprobanteArchivo() == null
                && !Boolean.TRUE.equals(compra.getComprobanteWhatsapp())
                && compra.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            cancelarPendiente(compra);
        }
    }

    private void cancelarPendiente(Compra compra) {
        compra.setEstado(EstadoCompra.CANCELADA);
        compra.setFechaResolucion(LocalDateTime.now());
        compra.getNumeros().forEach(numero -> {
            numero.setEstado(EstadoNumero.DISPONIBLE);
            numero.setCompra(null);
        });
        compra.getNumeros().clear();
    }

    private Compra buscarCompra(Long compraId) {
        return compraRepositorio.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la compra " + compraId));
    }

    private void validarArchivoComprobante(MultipartFile archivo) {
        String contentType = archivo.getContentType() == null ? "" : archivo.getContentType();
        validarArchivoComprobante(contentType, archivo.getSize());
    }

    private void validarArchivoComprobante(String contentType, long size) {
        String tipo = contentType == null ? "" : contentType;
        if (!tipo.startsWith("image/") && !tipo.equals("application/pdf")) {
            throw new IllegalArgumentException("El comprobante debe ser una imagen o PDF");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("El comprobante es obligatorio");
        }
        if (size > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("El comprobante no puede superar 5 MB");
        }
    }

    private void validarTieneComprobanteArchivo(Compra compra) {
        if (compra.getComprobanteArchivo() == null || compra.getComprobanteArchivo().isBlank()) {
            throw new IllegalArgumentException("La compra no tiene comprobante cargado");
        }
    }

    private void validarPropiedad(Compra compra) {
        Long clienteId = usuarioActual.clienteId();
        if (clienteId == null || compra.getRifa().getCliente() == null || !compra.getRifa().getCliente().getId().equals(clienteId)) {
            throw new SecurityException("No tenes permiso para operar esta compra");
        }
    }

    private CompraResponse aResponse(Compra compra) {
        List<String> numeros = compra.getNumeros().stream()
                .sorted(Comparator.comparing(NumeroRifa::getValor))
                .map(this::etiquetaCompra)
                .toList();
        if (numeros.isEmpty()) {
            numeros = compra.getEtiquetasNumeros().stream().sorted().toList();
        }

        return new CompraResponse(
                compra.getId(),
                compra.getRifa().getId(),
                compra.getRifa().getTitulo(),
                compra.getComprador().getNombre(),
                compra.getComprador().getTelefono(),
                numeros,
                compra.getTotal(),
                compra.getEstado(),
                compra.getFechaCreacion(),
                compra.getFechaExpiracion(),
                compra.getTokenSeguimiento(),
                compra.getComprobanteArchivo(),
                compra.getComprobanteWhatsapp(),
                compra.getTwilioMensajeSid(),
                compra.getWhatsappAutomaticoEstado(),
                compra.getWhatsappAutomaticoError(),
                compra.getFechaWhatsappAutomatico(),
                compra.getRifa().getAliasCobro() == null ? null : compra.getRifa().getAliasCobro().getId(),
                compra.getRifa().getAliasCobro() == null ? null : compra.getRifa().getAliasCobro().getNombre(),
                compra.getRifa().getAliasCobro() == null ? null : compra.getRifa().getAliasCobro().getEntidad(),
                compra.getRifa().getAliasCobro() == null ? null : compra.getRifa().getAliasCobro().getTitular(),
                compra.getRifa().getAliasCobro() == null ? null : compra.getRifa().getAliasCobro().getCbuCvu(),
                compra.getRifa().getAliasTransferencia(),
                compra.getRifa().getWhatsappComprobante()
        );
    }

    private String etiquetaCompra(NumeroRifa numero) {
        if (numero.getNumerosIncluidos().size() <= 1) {
            return numero.getEtiqueta();
        }
        return numero.getEtiqueta() + " (" + String.join("-", numero.getNumerosIncluidos()) + ")";
    }

    private String normalizarTelefonoComprador(String telefono) {
        return TelefonoArgentina.normalizarObligatorio(telefono);
    }

    private String normalizarTelefonoWhatsapp(String telefono) {
        return telefono == null ? "" : telefono.replace("whatsapp:", "").replace("+", "").replaceAll("\\D", "");
    }

    private void registrarEnvioWhatsappAutomatico(Compra compra) {
        TwilioEnvioResultado resultado = twilioWhatsappServicio.enviarMensajeCompra(compra);
        compra.setWhatsappAutomaticoEstado(resultado.estado());
        compra.setTwilioMensajeSid(resultado.messageSid());
        compra.setWhatsappAutomaticoError(resultado.error());
        compra.setFechaWhatsappAutomatico(LocalDateTime.now());
    }
}
