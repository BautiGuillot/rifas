package com.pescadoresargentinos.rifas.servicio;

import com.pescadoresargentinos.rifas.api.dto.CargarGanadoresRequest;
import com.pescadoresargentinos.rifas.api.dto.CrearRifaRequest;
import com.pescadoresargentinos.rifas.api.dto.DashboardAdminResponse;
import com.pescadoresargentinos.rifas.api.dto.EditarRifaRequest;
import com.pescadoresargentinos.rifas.api.dto.GanadorResponse;
import com.pescadoresargentinos.rifas.api.dto.NumeroResponse;
import com.pescadoresargentinos.rifas.api.dto.PremioResponse;
import com.pescadoresargentinos.rifas.api.dto.RifaDetalleResponse;
import com.pescadoresargentinos.rifas.api.dto.RifaResumenResponse;
import com.pescadoresargentinos.rifas.dominio.AliasCobro;
import com.pescadoresargentinos.rifas.dominio.Compra;
import com.pescadoresargentinos.rifas.dominio.Cliente;
import com.pescadoresargentinos.rifas.dominio.EstadoCliente;
import com.pescadoresargentinos.rifas.dominio.EstadoCompra;
import com.pescadoresargentinos.rifas.dominio.EstadoNumero;
import com.pescadoresargentinos.rifas.dominio.EstadoRifa;
import com.pescadoresargentinos.rifas.dominio.Ganador;
import com.pescadoresargentinos.rifas.dominio.NumeroRifa;
import com.pescadoresargentinos.rifas.dominio.Premio;
import com.pescadoresargentinos.rifas.dominio.Rifa;
import com.pescadoresargentinos.rifas.repositorio.GanadorRepositorio;
import com.pescadoresargentinos.rifas.repositorio.CompraRepositorio;
import com.pescadoresargentinos.rifas.repositorio.ClienteRepositorio;
import com.pescadoresargentinos.rifas.repositorio.NumeroRifaRepositorio;
import com.pescadoresargentinos.rifas.repositorio.PremioRepositorio;
import com.pescadoresargentinos.rifas.repositorio.RifaRepositorio;
import com.pescadoresargentinos.rifas.seguridad.UsuarioActual;
import com.pescadoresargentinos.rifas.util.TelefonoArgentina;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RifaServicio {

    private final RifaRepositorio rifaRepositorio;
    private final AliasCobroServicio aliasCobroServicio;
    private final NumeroRifaRepositorio numeroRifaRepositorio;
    private final PremioRepositorio premioRepositorio;
    private final GanadorRepositorio ganadorRepositorio;
    private final CompraRepositorio compraRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final UsuarioActual usuarioActual;

    public RifaServicio(
            RifaRepositorio rifaRepositorio,
            AliasCobroServicio aliasCobroServicio,
            NumeroRifaRepositorio numeroRifaRepositorio,
            PremioRepositorio premioRepositorio,
            GanadorRepositorio ganadorRepositorio,
            CompraRepositorio compraRepositorio,
            ClienteRepositorio clienteRepositorio,
            UsuarioActual usuarioActual
    ) {
        this.rifaRepositorio = rifaRepositorio;
        this.aliasCobroServicio = aliasCobroServicio;
        this.numeroRifaRepositorio = numeroRifaRepositorio;
        this.premioRepositorio = premioRepositorio;
        this.ganadorRepositorio = ganadorRepositorio;
        this.compraRepositorio = compraRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.usuarioActual = usuarioActual;
    }

    @Transactional
    public RifaDetalleResponse crear(CrearRifaRequest request) {
        Cliente cliente = clienteActualActivo();
        if (rifaRepositorio.existsBySlug(request.slug())) {
            throw new IllegalArgumentException("Ya existe una rifa con ese slug");
        }
        validarPremios(request);

        Rifa rifa = new Rifa();
        rifa.setCliente(cliente);
        rifa.setTitulo(request.titulo());
        rifa.setSlug(request.slug());
        rifa.setDescripcion(request.descripcion());
        rifa.setAclaracionSorteo(normalizarTextoOpcional(request.aclaracionSorteo()));
        rifa.setCantidadNumeros(request.cantidadNumeros());
        rifa.setCantidadFilas(request.cantidadFilas());
        rifa.setCantidadGanadores(request.cantidadGanadores());
        rifa.setValorNumero(request.valorNumero());
        aplicarAliasCobro(rifa, cliente.getId(), request.aliasCobroId(), request.aliasTransferencia());
        rifa.setWhatsappComprobante(normalizarTelefono(request.whatsappComprobante()));
        rifa.setFechaSorteo(request.fechaSorteo());

        request.premios().forEach(premioRequest -> {
            Premio premio = new Premio();
            premio.setRifa(rifa);
            premio.setPosicion(premioRequest.posicion());
            premio.setDescripcion(premioRequest.descripcion());
            premio.setImagenUrl(normalizarTextoOpcional(premioRequest.imagenUrl()));
            rifa.getPremios().add(premio);
        });

        generarNumeros(rifa, request.cantidadNumeros(), request.cantidadFilas());

        return detalle(rifaRepositorio.save(rifa).getId());
    }

    @Transactional
    public RifaDetalleResponse editar(Long id, EditarRifaRequest request) {
        Rifa rifa = buscarRifa(id);
        validarPropiedad(rifa);
        if (rifa.getEstado() != EstadoRifa.BORRADOR) {
            throw new IllegalStateException("Solo se pueden editar rifas en borrador");
        }
        rifaRepositorio.findBySlug(request.slug()).ifPresent(existente -> {
            if (!existente.getId().equals(id)) {
                throw new IllegalArgumentException("Ya existe una rifa con ese slug");
            }
        });
        if (compraRepositorio.existsByRifaId(id)) {
            throw new IllegalStateException("No se puede editar una rifa con compras asociadas");
        }
        validarConfiguracionNumeros(request.cantidadNumeros(), request.cantidadFilas());
        validarPremios(request.cantidadGanadores(), request.premios());

        rifa.setTitulo(request.titulo());
        rifa.setSlug(request.slug());
        rifa.setDescripcion(request.descripcion());
        rifa.setAclaracionSorteo(normalizarTextoOpcional(request.aclaracionSorteo()));
        rifa.setCantidadNumeros(request.cantidadNumeros());
        rifa.setCantidadFilas(request.cantidadFilas());
        rifa.setCantidadGanadores(request.cantidadGanadores());
        rifa.setValorNumero(request.valorNumero());
        aplicarAliasCobro(rifa, rifa.getCliente().getId(), request.aliasCobroId(), request.aliasTransferencia());
        rifa.setWhatsappComprobante(normalizarTelefono(request.whatsappComprobante()));
        rifa.setFechaSorteo(request.fechaSorteo());

        rifa.getPremios().clear();
        rifa.getNumeros().clear();
        rifaRepositorio.flush();

        request.premios().forEach(premioRequest -> {
            Premio premio = new Premio();
            premio.setRifa(rifa);
            premio.setPosicion(premioRequest.posicion());
            premio.setDescripcion(premioRequest.descripcion());
            premio.setImagenUrl(normalizarTextoOpcional(premioRequest.imagenUrl()));
            rifa.getPremios().add(premio);
        });

        generarNumeros(rifa, request.cantidadNumeros(), request.cantidadFilas());

        return detalle(id);
    }

    @Transactional(readOnly = true)
    public List<RifaResumenResponse> listarPublicadas() {
        return rifaRepositorio.findByEstadoOrderByIdDesc(EstadoRifa.PUBLICADA).stream()
                .map(this::aResumen)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RifaResumenResponse> listarFinalizadas() {
        return rifaRepositorio.findDistinctByEstadoInOrderByIdDesc(List.of(EstadoRifa.FINALIZADA)).stream()
                .map(this::aResumen)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RifaResumenResponse> listarTodas() {
        Long clienteId = usuarioActual.clienteId();
        return rifaRepositorio.findByClienteIdOrderByIdDesc(clienteId).stream()
                .map(this::aResumen)
                .toList();
    }

    @Transactional(readOnly = true)
    public RifaDetalleResponse detalle(Long id) {
        Rifa rifa = buscarRifa(id);
        List<NumeroRifa> numeros = numeroRifaRepositorio.findByRifaIdOrderByValorAsc(id);
        List<Ganador> ganadores = ganadorRepositorio.findByRifaIdOrderByPosicionAsc(id);
        return aDetalle(rifa, numeros, ganadores);
    }

    @Transactional(readOnly = true)
    public RifaDetalleResponse detallePublicoPorSlug(String slug) {
        Rifa rifa = rifaRepositorio.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("No existe la rifa " + slug));
        if (rifa.getEstado() != EstadoRifa.PUBLICADA
                && rifa.getEstado() != EstadoRifa.FINALIZADA
                && rifa.getEstado() != EstadoRifa.CANCELADA) {
            throw new IllegalStateException("La rifa todavia no esta publicada");
        }
        return detalle(rifa.getId());
    }

    @Transactional
    public RifaDetalleResponse publicar(Long id) {
        Rifa rifa = buscarRifa(id);
        validarPropiedad(rifa);
        if (rifa.getEstado() != EstadoRifa.BORRADOR) {
            throw new IllegalStateException("Solo se pueden publicar rifas en borrador");
        }
        rifa.setEstado(EstadoRifa.PUBLICADA);
        return detalle(id);
    }

    @Transactional
    public RifaDetalleResponse finalizar(Long id) {
        Rifa rifa = buscarRifa(id);
        validarPropiedad(rifa);
        if (rifa.getEstado() != EstadoRifa.PUBLICADA) {
            throw new IllegalStateException("Solo se pueden finalizar rifas publicadas");
        }
        throw new IllegalStateException("Para finalizar la rifa tenes que cargar los ganadores");
    }

    @Transactional
    public RifaDetalleResponse finalizarConGanadores(Long id, CargarGanadoresRequest request) {
        Rifa rifa = buscarRifa(id);
        validarPropiedad(rifa);
        if (rifa.getEstado() != EstadoRifa.PUBLICADA) {
            throw new IllegalStateException("Solo se pueden finalizar rifas publicadas");
        }
        validarTodosLosNumerosVendidos(rifa);
        validarYGuardarGanadores(rifa, request);
        rifa.setEstado(EstadoRifa.FINALIZADA);
        return detalle(id);
    }

    @Transactional
    public RifaDetalleResponse cancelar(Long id) {
        Rifa rifa = buscarRifa(id);
        validarPropiedad(rifa);
        if (rifa.getEstado() == EstadoRifa.FINALIZADA) {
            throw new IllegalStateException("No se puede cancelar una rifa finalizada");
        }
        if (rifa.getEstado() == EstadoRifa.CANCELADA) {
            return detalle(id);
        }

        List<Compra> comprasPendientes = compraRepositorio.findByRifaIdAndEstado(id, EstadoCompra.PENDIENTE_PAGO);
        comprasPendientes.forEach(compra -> {
            compra.setEstado(EstadoCompra.CANCELADA);
            compra.setFechaResolucion(java.time.LocalDateTime.now());
            compra.getNumeros().forEach(numero -> {
                numero.setEstado(EstadoNumero.DISPONIBLE);
                numero.setCompra(null);
            });
            compra.getNumeros().clear();
        });

        rifa.setEstado(EstadoRifa.CANCELADA);
        return detalle(id);
    }

    @Transactional
    public void eliminar(Long id) {
        Rifa rifa = buscarRifa(id);
        validarPropiedad(rifa);
        if (rifa.getEstado() != EstadoRifa.CANCELADA) {
            throw new IllegalStateException("Solo se pueden eliminar rifas canceladas");
        }

        List<Compra> compras = compraRepositorio.findByRifaId(id);
        compras.forEach(compra -> {
            compra.getNumeros().forEach(numero -> {
                numero.setCompra(null);
                numero.setEstado(EstadoNumero.DISPONIBLE);
            });
            compra.getNumeros().clear();
        });
        compraRepositorio.deleteAll(compras);
        rifaRepositorio.delete(rifa);
    }

    @Transactional(readOnly = true)
    public DashboardAdminResponse dashboard() {
        Long clienteId = usuarioActual.clienteId();
        return new DashboardAdminResponse(
                rifaRepositorio.countByClienteIdAndEstado(clienteId, EstadoRifa.BORRADOR),
                rifaRepositorio.countByClienteIdAndEstado(clienteId, EstadoRifa.PUBLICADA),
                rifaRepositorio.countByClienteIdAndEstado(clienteId, EstadoRifa.FINALIZADA),
                rifaRepositorio.countByClienteIdAndEstado(clienteId, EstadoRifa.CANCELADA),
                compraRepositorio.countByRifaClienteIdAndEstado(clienteId, EstadoCompra.PENDIENTE_PAGO),
                compraRepositorio.countByRifaClienteIdAndEstado(clienteId, EstadoCompra.APROBADA),
                compraRepositorio.countByRifaClienteIdAndEstado(clienteId, EstadoCompra.CANCELADA),
                numeroRifaRepositorio.countByRifaClienteIdAndEstado(clienteId, EstadoNumero.DISPONIBLE),
                numeroRifaRepositorio.countByRifaClienteIdAndEstado(clienteId, EstadoNumero.PENDIENTE),
                numeroRifaRepositorio.countByRifaClienteIdAndEstado(clienteId, EstadoNumero.VENDIDO),
                compraRepositorio.sumarTotalPorClienteYEstado(clienteId, EstadoCompra.APROBADA)
        );
    }

    @Transactional
    public RifaDetalleResponse cargarGanadores(Long id, CargarGanadoresRequest request) {
        Rifa rifa = buscarRifa(id);
        validarPropiedad(rifa);
        if (rifa.getEstado() != EstadoRifa.FINALIZADA) {
            throw new IllegalStateException("La rifa debe estar finalizada para cargar ganadores");
        }
        validarYGuardarGanadores(rifa, request);
        return detalle(id);
    }

    private void validarYGuardarGanadores(Rifa rifa, CargarGanadoresRequest request) {
        Long id = rifa.getId();
        if (request.ganadores().size() != rifa.getCantidadGanadores()) {
            throw new IllegalArgumentException("La cantidad de ganadores debe coincidir con la rifa");
        }

        Set<Integer> posiciones = new HashSet<>();
        Set<Integer> numerosGanadores = new HashSet<>();
        request.ganadores().forEach(ganador -> {
            if (!posiciones.add(ganador.posicion())) {
                throw new IllegalArgumentException("Hay posiciones de ganadores repetidas");
            }
            if (!numerosGanadores.add(ganador.numero())) {
                throw new IllegalArgumentException("Hay numeros ganadores repetidos");
            }
        });

        ganadorRepositorio.deleteByRifaId(id);
        request.ganadores().forEach(ganadorRequest -> {
            Premio premio = premioRepositorio.findByRifaIdAndPosicion(id, ganadorRequest.posicion())
                    .orElseThrow(() -> new IllegalArgumentException("No existe premio para la posicion " + ganadorRequest.posicion()));
            NumeroRifa numero = buscarNumeroGanador(rifa, ganadorRequest.numero());
            if (numero.getEstado() != EstadoNumero.VENDIDO) {
                throw new IllegalArgumentException("El numero ganador debe estar vendido: " + numero.getEtiqueta());
            }

            Ganador ganador = new Ganador();
            ganador.setRifa(rifa);
            ganador.setPosicion(ganadorRequest.posicion());
            ganador.setPremio(premio);
            ganador.setNumero(numero);
            ganadorRepositorio.save(ganador);
        });
    }

    private void validarTodosLosNumerosVendidos(Rifa rifa) {
        boolean hayNoVendidos = numeroRifaRepositorio.findByRifaIdOrderByValorAsc(rifa.getId()).stream()
                .anyMatch(numero -> numero.getEstado() != EstadoNumero.VENDIDO);
        if (hayNoVendidos) {
            throw new IllegalStateException("Para finalizar la rifa todos los numeros deben estar vendidos");
        }
    }

    private void validarPremios(CrearRifaRequest request) {
        validarConfiguracionNumeros(request.cantidadNumeros(), request.cantidadFilas());
        validarPremios(request.cantidadGanadores(), request.premios());
    }

    private NumeroRifa buscarNumeroGanador(Rifa rifa, Integer numeroSorteado) {
        return numeroRifaRepositorio.findByRifaIdOrderByValorAsc(rifa.getId()).stream()
                .filter(numero -> numero.getNumerosIncluidos().stream()
                        .anyMatch(incluido -> Integer.parseInt(incluido) == numeroSorteado))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No existe el numero " + numeroSorteado));
    }

    private void validarConfiguracionNumeros(Integer cantidadNumeros, Integer cantidadFilas) {
        if (cantidadFilas > cantidadNumeros) {
            throw new IllegalArgumentException("La cantidad de filas no puede superar la cantidad de numeros");
        }
        if (cantidadNumeros % cantidadFilas != 0) {
            throw new IllegalArgumentException("La cantidad de numeros debe ser divisible por la cantidad de filas");
        }
    }

    private void generarNumeros(Rifa rifa, int cantidadNumeros, int cantidadFilas) {
        int numerosPorFila = cantidadNumeros / cantidadFilas;
        boolean numerosDirectosDeCero = cantidadNumeros == cantidadFilas;
        int inicio = numerosDirectosDeCero ? 0 : 1;
        int anchoNumero = Math.max(2, String.valueOf(numerosDirectosDeCero ? cantidadNumeros - 1 : cantidadNumeros).length());
        int anchoFila = Math.max(2, String.valueOf(cantidadFilas).length());

        for (int fila = 1; fila <= cantidadFilas; fila++) {
            NumeroRifa numero = new NumeroRifa();
            numero.setRifa(rifa);
            numero.setValor(numerosDirectosDeCero ? fila - 1 : fila);
            for (int columna = 0; columna < numerosPorFila; columna++) {
                int valorSorteo = inicio + fila - 1 + (columna * cantidadFilas);
                numero.getNumerosIncluidos().add(String.format("%0" + anchoNumero + "d", valorSorteo));
            }
            numero.setEtiqueta(numerosDirectosDeCero
                    ? numero.getNumerosIncluidos().getFirst()
                    : String.format("%0" + anchoFila + "d", fila));
            rifa.getNumeros().add(numero);
        }
    }

    private void validarPremios(Integer cantidadGanadores, List<com.pescadoresargentinos.rifas.api.dto.PremioRequest> premios) {
        if (premios.size() != cantidadGanadores) {
            throw new IllegalArgumentException("La cantidad de premios debe coincidir con la cantidad de ganadores");
        }

        Set<Integer> posiciones = new HashSet<>();
        premios.forEach(premio -> {
            if (!posiciones.add(premio.posicion())) {
                throw new IllegalArgumentException("Hay posiciones de premios repetidas");
            }
            if (premio.posicion() > cantidadGanadores) {
                throw new IllegalArgumentException("La posicion del premio excede la cantidad de ganadores");
            }
        });
    }

    private Rifa buscarRifa(Long id) {
        return rifaRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la rifa " + id));
    }

    private Cliente clienteActualActivo() {
        Long clienteId = usuarioActual.clienteId();
        if (clienteId == null) {
            throw new IllegalStateException("El super admin no crea rifas");
        }
        Cliente cliente = clienteRepositorio.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el cliente " + clienteId));
        if (cliente.getEstado() != EstadoCliente.ACTIVO) {
            throw new IllegalStateException("El cliente esta inactivo y no puede crear rifas nuevas");
        }
        return cliente;
    }

    private void validarPropiedad(Rifa rifa) {
        Long clienteId = usuarioActual.clienteId();
        if (clienteId == null || rifa.getCliente() == null || !rifa.getCliente().getId().equals(clienteId)) {
            throw new SecurityException("No tenes permiso para operar esta rifa");
        }
    }

    private RifaResumenResponse aResumen(Rifa rifa) {
        List<Ganador> ganadores = ganadorRepositorio.findByRifaIdOrderByPosicionAsc(rifa.getId());
        AliasCobro aliasCobro = aliasCobroEfectivo(rifa);
        return new RifaResumenResponse(
                rifa.getId(),
                rifa.getTitulo(),
                rifa.getSlug(),
                rifa.getCliente() == null ? null : rifa.getCliente().getId(),
                rifa.getCliente() == null ? null : rifa.getCliente().getNombre(),
                rifa.getCliente() == null ? null : rifa.getCliente().getColorPrincipal(),
                rifa.getCliente() == null ? null : rifa.getCliente().getLogoUrl(),
                rifa.getDescripcion(),
                rifa.getAclaracionSorteo(),
                rifa.getCantidadNumeros(),
                rifa.getCantidadFilas(),
                rifa.getCantidadGanadores(),
                rifa.getValorNumero(),
                aliasCobro == null ? null : aliasCobro.getId(),
                aliasCobro == null ? null : aliasCobro.getNombre(),
                aliasCobro == null ? null : aliasCobro.getEntidad(),
                aliasCobro == null ? null : aliasCobro.getTitular(),
                aliasCobro == null ? null : aliasCobro.getCbuCvu(),
                rifa.getAliasTransferencia(),
                rifa.getEstado(),
                rifa.getFechaCreacion(),
                rifa.getFechaSorteo(),
                premiosOrdenados(rifa),
                ganadores.stream().map(this::aGanadorResponse).toList()
        );
    }

    private RifaDetalleResponse aDetalle(Rifa rifa, List<NumeroRifa> numeros, List<Ganador> ganadores) {
        AliasCobro aliasCobro = aliasCobroEfectivo(rifa);
        return new RifaDetalleResponse(
                rifa.getId(),
                rifa.getTitulo(),
                rifa.getSlug(),
                rifa.getCliente() == null ? null : rifa.getCliente().getId(),
                rifa.getCliente() == null ? null : rifa.getCliente().getNombre(),
                rifa.getCliente() == null ? null : rifa.getCliente().getColorPrincipal(),
                rifa.getCliente() == null ? null : rifa.getCliente().getLogoUrl(),
                rifa.getDescripcion(),
                rifa.getAclaracionSorteo(),
                rifa.getCantidadNumeros(),
                rifa.getCantidadFilas(),
                rifa.getCantidadGanadores(),
                rifa.getValorNumero(),
                aliasCobro == null ? null : aliasCobro.getId(),
                aliasCobro == null ? null : aliasCobro.getNombre(),
                aliasCobro == null ? null : aliasCobro.getEntidad(),
                aliasCobro == null ? null : aliasCobro.getTitular(),
                aliasCobro == null ? null : aliasCobro.getCbuCvu(),
                rifa.getAliasTransferencia(),
                rifa.getWhatsappComprobante(),
                rifa.getEstado(),
                rifa.getFechaCreacion(),
                rifa.getFechaSorteo(),
                premiosOrdenados(rifa),
                numeros.stream().map(numero -> new NumeroResponse(
                        numero.getId(),
                        numero.getValor(),
                        numero.getEtiqueta(),
                        new ArrayList<>(numero.getNumerosIncluidos()),
                        numero.getEstado()
                )).toList(),
                ganadores.stream().map(this::aGanadorResponse).toList()
        );
    }

    private List<PremioResponse> premiosOrdenados(Rifa rifa) {
        return rifa.getPremios().stream()
                .sorted(Comparator.comparing(Premio::getPosicion))
                .map(premio -> new PremioResponse(premio.getId(), premio.getPosicion(), premio.getDescripcion(), premio.getImagenUrl()))
                .toList();
    }

    private String normalizarTextoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private AliasCobro aliasCobroEfectivo(Rifa rifa) {
        if (rifa.getAliasCobro() != null) {
            return rifa.getAliasCobro();
        }
        Long clienteId = rifa.getCliente() == null ? null : rifa.getCliente().getId();
        return aliasCobroServicio.buscarPorAlias(clienteId, rifa.getAliasTransferencia()).orElse(null);
    }

    private void aplicarAliasCobro(Rifa rifa, Long clienteId, Long aliasCobroId, String aliasTransferencia) {
        if (aliasCobroId != null) {
            AliasCobro aliasCobro = aliasCobroServicio.buscarActivoPropio(aliasCobroId, clienteId);
            rifa.setAliasCobro(aliasCobro);
            rifa.setAliasTransferencia(aliasCobro.getAlias());
            return;
        }

        String aliasNormalizado = normalizarTextoOpcional(aliasTransferencia);
        if (aliasNormalizado == null) {
            throw new IllegalArgumentException("Tenes que seleccionar un alias de cobro o ingresar un alias de transferencia");
        }
        rifa.setAliasCobro(null);
        rifa.setAliasTransferencia(aliasNormalizado);
    }

    private GanadorResponse aGanadorResponse(Ganador ganador) {
        return new GanadorResponse(
                ganador.getPosicion(),
                ganador.getNumero().getEtiqueta(),
                ganador.getPremio().getDescripcion(),
                ganador.getNumero().getCompra().getComprador().getNombre(),
                ganador.getNumero().getCompra().getComprador().getTelefono()
        );
    }

    private String normalizarTelefono(String telefono) {
        return TelefonoArgentina.normalizarObligatorio(telefono);
    }
}
