package com.pescadoresargentinos.rifas.servicio;

import com.pescadoresargentinos.rifas.api.dto.AliasCobroRequest;
import com.pescadoresargentinos.rifas.api.dto.AliasCobroDetalleResponse;
import com.pescadoresargentinos.rifas.api.dto.AliasCobroResponse;
import com.pescadoresargentinos.rifas.api.dto.AliasCobroRifaResponse;
import com.pescadoresargentinos.rifas.dominio.AliasCobro;
import com.pescadoresargentinos.rifas.dominio.Cliente;
import com.pescadoresargentinos.rifas.dominio.EstadoCliente;
import com.pescadoresargentinos.rifas.dominio.EstadoCompra;
import com.pescadoresargentinos.rifas.dominio.EstadoRifa;
import com.pescadoresargentinos.rifas.dominio.Rifa;
import com.pescadoresargentinos.rifas.repositorio.AliasCobroRepositorio;
import com.pescadoresargentinos.rifas.repositorio.ClienteRepositorio;
import com.pescadoresargentinos.rifas.repositorio.CompraRepositorio;
import com.pescadoresargentinos.rifas.repositorio.RifaRepositorio;
import com.pescadoresargentinos.rifas.seguridad.UsuarioActual;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AliasCobroServicio {

    private final AliasCobroRepositorio aliasCobroRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final RifaRepositorio rifaRepositorio;
    private final CompraRepositorio compraRepositorio;
    private final UsuarioActual usuarioActual;

    public AliasCobroServicio(
            AliasCobroRepositorio aliasCobroRepositorio,
            ClienteRepositorio clienteRepositorio,
            RifaRepositorio rifaRepositorio,
            CompraRepositorio compraRepositorio,
            UsuarioActual usuarioActual
    ) {
        this.aliasCobroRepositorio = aliasCobroRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.rifaRepositorio = rifaRepositorio;
        this.compraRepositorio = compraRepositorio;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<AliasCobroResponse> listar(LocalDate desde, LocalDate hasta) {
        Long clienteId = clienteActualActivo().getId();
        RangoFechas rango = RangoFechas.crear(desde, hasta);
        Map<Long, TotalesAlias> totales = totalesPorAlias(clienteId, rango);
        return aliasCobroRepositorio.findByClienteIdOrderByActivoDescNombreAsc(clienteId).stream()
                .map(alias -> aResponse(alias, totales.get(alias.getId()), rango))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AliasCobroResponse> listarActivos(LocalDate desde, LocalDate hasta) {
        Long clienteId = clienteActualActivo().getId();
        RangoFechas rango = RangoFechas.crear(desde, hasta);
        Map<Long, TotalesAlias> totales = totalesPorAlias(clienteId, rango);
        return aliasCobroRepositorio.findByClienteIdAndActivoTrueOrderByNombreAsc(clienteId).stream()
                .map(alias -> aResponse(alias, totales.get(alias.getId()), rango))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AliasCobroResponse> listarActivos() {
        return listarActivos(null, null);
    }

    @Transactional(readOnly = true)
    public AliasCobroDetalleResponse detalle(Long id, LocalDate desde, LocalDate hasta) {
        Long clienteId = clienteActualActivo().getId();
        RangoFechas rango = RangoFechas.crear(desde, hasta);
        AliasCobro aliasCobro = buscarPropio(id, clienteId);
        List<Rifa> rifas = rifaRepositorio.findAsignadasAAlias(clienteId, aliasCobro.getId(), aliasCobro.getAlias());
        List<Rifa> rifasVisibles = rango.activo()
                ? rifas.stream()
                        .filter(rifa -> rifa.getEstado() == EstadoRifa.FINALIZADA)
                        .filter(rifa -> rango.incluye(rifa.getFechaFinalizacion()))
                        .toList()
                : rifas;
        Map<Long, TotalesRifa> totales = totalesPorRifa(rifasVisibles, rango);
        return new AliasCobroDetalleResponse(
                aResponse(aliasCobro, totalesPorAlias(clienteId, rango).get(aliasCobro.getId()), rango),
                rifasVisibles.stream()
                        .map(rifa -> aRifaResponse(rifa, totales.get(rifa.getId())))
                        .toList()
        );
    }

    @Transactional
    public AliasCobroResponse crear(AliasCobroRequest request) {
        Cliente cliente = clienteActualActivo();
        String aliasNormalizado = normalizarAlias(request.alias());
        validarAliasDisponible(cliente.getId(), aliasNormalizado, null);

        AliasCobro aliasCobro = new AliasCobro();
        aliasCobro.setCliente(cliente);
        aplicar(aliasCobro, request, aliasNormalizado);
        return aResponse(aliasCobroRepositorio.save(aliasCobro), null, RangoFechas.sinFiltro());
    }

    @Transactional
    public AliasCobroResponse actualizar(Long id, AliasCobroRequest request) {
        Long clienteId = clienteActualActivo().getId();
        AliasCobro aliasCobro = buscarPropio(id, clienteId);
        String aliasNormalizado = normalizarAlias(request.alias());
        validarAliasDisponible(clienteId, aliasNormalizado, id);
        aplicar(aliasCobro, request, aliasNormalizado);
        RangoFechas rango = RangoFechas.sinFiltro();
        return aResponse(aliasCobro, totalesPorAlias(clienteId, rango).get(id), rango);
    }

    @Transactional
    public AliasCobroResponse cambiarEstado(Long id, boolean activo) {
        Long clienteId = clienteActualActivo().getId();
        AliasCobro aliasCobro = buscarPropio(id, clienteId);
        aliasCobro.setActivo(activo);
        RangoFechas rango = RangoFechas.sinFiltro();
        return aResponse(aliasCobro, totalesPorAlias(clienteId, rango).get(id), rango);
    }

    public AliasCobro buscarActivoPropio(Long id, Long clienteId) {
        AliasCobro aliasCobro = buscarPropio(id, clienteId);
        if (!Boolean.TRUE.equals(aliasCobro.getActivo())) {
            throw new IllegalArgumentException("El alias de cobro esta inactivo");
        }
        return aliasCobro;
    }

    @Transactional(readOnly = true)
    public Optional<AliasCobro> buscarPorAlias(Long clienteId, String alias) {
        if (clienteId == null || alias == null || alias.isBlank()) {
            return Optional.empty();
        }
        return aliasCobroRepositorio.findFirstByClienteIdAndAliasIgnoreCase(clienteId, alias.trim());
    }

    private void aplicar(AliasCobro aliasCobro, AliasCobroRequest request, String aliasNormalizado) {
        aliasCobro.setNombre(request.nombre().trim());
        aliasCobro.setAlias(aliasNormalizado);
        aliasCobro.setEntidad(normalizarTextoOpcional(request.entidad()));
        aliasCobro.setTitular(normalizarTextoOpcional(request.titular()));
        aliasCobro.setCbuCvu(normalizarTextoOpcional(request.cbuCvu()));
        aliasCobro.setActivo(request.activo() == null || request.activo());
    }

    private void validarAliasDisponible(Long clienteId, String alias, Long idActual) {
        if (!aliasCobroRepositorio.existsByClienteIdAndAliasIgnoreCase(clienteId, alias)) {
            return;
        }
        boolean correspondeAlActual = idActual != null
                && aliasCobroRepositorio.findByIdAndClienteId(idActual, clienteId)
                        .map(existente -> existente.getAlias().equalsIgnoreCase(alias))
                        .orElse(false);
        if (!correspondeAlActual) {
            throw new IllegalArgumentException("Ya existe un alias de cobro con ese valor");
        }
    }

    private AliasCobro buscarPropio(Long id, Long clienteId) {
        return aliasCobroRepositorio.findByIdAndClienteId(id, clienteId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el alias de cobro " + id));
    }

    private Cliente clienteActualActivo() {
        Long clienteId = usuarioActual.clienteId();
        if (clienteId == null) {
            throw new IllegalStateException("El super admin no administra alias de cobro");
        }
        Cliente cliente = clienteRepositorio.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el cliente " + clienteId));
        if (cliente.getEstado() != EstadoCliente.ACTIVO) {
            throw new IllegalStateException("El cliente esta inactivo");
        }
        return cliente;
    }

    private Map<Long, TotalesAlias> totalesPorAlias(Long clienteId, RangoFechas rango) {
        Map<Long, TotalesAlias> totales = new HashMap<>();
        List<Object[]> resumen = rango.activo()
                ? compraRepositorio.sumarAprobadasPorAliasCobroEntre(
                        clienteId, EstadoCompra.APROBADA, rango.desde(), rango.hastaExclusivo())
                : compraRepositorio.sumarAprobadasPorAliasCobro(clienteId, EstadoCompra.APROBADA);
        resumen
                .forEach(row -> totales.put(
                        (Long) row[0],
                        new TotalesAlias((Long) row[1], (BigDecimal) row[2])
                ));
        return totales;
    }

    private AliasCobroResponse aResponse(AliasCobro aliasCobro, TotalesAlias totales, RangoFechas rango) {
        TotalesAlias datos = totales == null ? new TotalesAlias(0L, BigDecimal.ZERO) : totales;
        return new AliasCobroResponse(
                aliasCobro.getId(),
                aliasCobro.getNombre(),
                aliasCobro.getAlias(),
                aliasCobro.getEntidad(),
                aliasCobro.getTitular(),
                aliasCobro.getCbuCvu(),
                aliasCobro.getActivo(),
                aliasCobro.getFechaCreacion(),
                rifaRepositorio.countAsignadasAAlias(
                        aliasCobro.getCliente().getId(),
                        aliasCobro.getId(),
                        aliasCobro.getAlias()
                ),
                contarRifasFinalizadas(aliasCobro, rango),
                datos.comprasAprobadas(),
                datos.recaudacionAprobada()
        );
    }

    private Map<Long, TotalesRifa> totalesPorRifa(List<Rifa> rifas, RangoFechas rango) {
        Map<Long, TotalesRifa> totales = new HashMap<>();
        List<Long> rifaIds = rifas.stream().map(Rifa::getId).toList();
        if (rifaIds.isEmpty()) {
            return totales;
        }
        List<Object[]> resumen = rango.activo()
                ? compraRepositorio.resumirPorRifasEntre(rifaIds, rango.desde(), rango.hastaExclusivo())
                : compraRepositorio.resumirPorRifas(rifaIds);
        resumen.forEach(row -> {
            Long rifaId = (Long) row[0];
            EstadoCompra estado = (EstadoCompra) row[1];
            Long cantidad = (Long) row[2];
            BigDecimal total = (BigDecimal) row[3];
            TotalesRifa actual = totales.getOrDefault(rifaId, TotalesRifa.vacio());
            totales.put(rifaId, actual.con(estado, cantidad, total));
        });
        return totales;
    }

    private long contarRifasFinalizadas(AliasCobro aliasCobro, RangoFechas rango) {
        Long clienteId = aliasCobro.getCliente().getId();
        return rango.activo()
                ? rifaRepositorio.countFinalizadasAAliasEntre(
                        clienteId,
                        aliasCobro.getId(),
                        aliasCobro.getAlias(),
                        EstadoRifa.FINALIZADA,
                        rango.desde(),
                        rango.hastaExclusivo()
                )
                : rifaRepositorio.countFinalizadasAAlias(
                        clienteId,
                        aliasCobro.getId(),
                        aliasCobro.getAlias(),
                        EstadoRifa.FINALIZADA
                );
    }

    private AliasCobroRifaResponse aRifaResponse(Rifa rifa, TotalesRifa totales) {
        TotalesRifa datos = totales == null ? TotalesRifa.vacio() : totales;
        return new AliasCobroRifaResponse(
                rifa.getId(),
                rifa.getTitulo(),
                rifa.getSlug(),
                rifa.getEstado(),
                rifa.getValorNumero(),
                rifa.getFechaCreacion(),
                rifa.getFechaSorteo(),
                rifa.getFechaFinalizacion(),
                datos.comprasPendientes(),
                datos.comprasAprobadas(),
                datos.comprasCanceladas(),
                datos.recaudacionAprobada()
        );
    }

    private String normalizarAlias(String alias) {
        return alias.trim();
    }

    private String normalizarTextoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private record TotalesAlias(Long comprasAprobadas, BigDecimal recaudacionAprobada) {
    }

    private record RangoFechas(LocalDateTime desde, LocalDateTime hastaExclusivo, boolean activo) {
        private static final LocalDateTime LIMITE_INFERIOR = LocalDate.of(1900, 1, 1).atStartOfDay();
        private static final LocalDateTime LIMITE_SUPERIOR = LocalDate.of(3000, 1, 1).atStartOfDay();

        private static RangoFechas crear(LocalDate desde, LocalDate hasta) {
            if (desde != null && hasta != null && desde.isAfter(hasta)) {
                throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta");
            }
            return new RangoFechas(
                    desde == null ? LIMITE_INFERIOR : desde.atStartOfDay(),
                    hasta == null ? LIMITE_SUPERIOR : hasta.plusDays(1).atStartOfDay(),
                    desde != null || hasta != null
            );
        }

        private static RangoFechas sinFiltro() {
            return new RangoFechas(LIMITE_INFERIOR, LIMITE_SUPERIOR, false);
        }

        private boolean incluye(LocalDateTime fecha) {
            return fecha != null
                    && !fecha.isBefore(desde)
                    && fecha.isBefore(hastaExclusivo);
        }
    }

    private record TotalesRifa(
            Long comprasPendientes,
            Long comprasAprobadas,
            Long comprasCanceladas,
            BigDecimal recaudacionAprobada
    ) {
        private static TotalesRifa vacio() {
            return new TotalesRifa(0L, 0L, 0L, BigDecimal.ZERO);
        }

        private TotalesRifa con(EstadoCompra estado, Long cantidad, BigDecimal total) {
            return switch (estado) {
                case PENDIENTE_PAGO -> new TotalesRifa(cantidad, comprasAprobadas, comprasCanceladas, recaudacionAprobada);
                case APROBADA -> new TotalesRifa(comprasPendientes, cantidad, comprasCanceladas, total);
                case CANCELADA -> new TotalesRifa(comprasPendientes, comprasAprobadas, cantidad, recaudacionAprobada);
            };
        }
    }
}
