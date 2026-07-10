package com.pescadoresargentinos.rifas.servicio;

import com.pescadoresargentinos.rifas.api.dto.AliasCobroRequest;
import com.pescadoresargentinos.rifas.api.dto.AliasCobroDetalleResponse;
import com.pescadoresargentinos.rifas.api.dto.AliasCobroResponse;
import com.pescadoresargentinos.rifas.api.dto.AliasCobroRifaResponse;
import com.pescadoresargentinos.rifas.dominio.AliasCobro;
import com.pescadoresargentinos.rifas.dominio.Cliente;
import com.pescadoresargentinos.rifas.dominio.EstadoCliente;
import com.pescadoresargentinos.rifas.dominio.EstadoCompra;
import com.pescadoresargentinos.rifas.dominio.Rifa;
import com.pescadoresargentinos.rifas.repositorio.AliasCobroRepositorio;
import com.pescadoresargentinos.rifas.repositorio.ClienteRepositorio;
import com.pescadoresargentinos.rifas.repositorio.CompraRepositorio;
import com.pescadoresargentinos.rifas.repositorio.RifaRepositorio;
import com.pescadoresargentinos.rifas.seguridad.UsuarioActual;
import java.math.BigDecimal;
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
    public List<AliasCobroResponse> listar() {
        Long clienteId = clienteActualActivo().getId();
        Map<Long, TotalesAlias> totales = totalesPorAlias(clienteId);
        return aliasCobroRepositorio.findByClienteIdOrderByActivoDescNombreAsc(clienteId).stream()
                .map(alias -> aResponse(alias, totales.get(alias.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AliasCobroResponse> listarActivos() {
        Long clienteId = clienteActualActivo().getId();
        Map<Long, TotalesAlias> totales = totalesPorAlias(clienteId);
        return aliasCobroRepositorio.findByClienteIdAndActivoTrueOrderByNombreAsc(clienteId).stream()
                .map(alias -> aResponse(alias, totales.get(alias.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AliasCobroDetalleResponse detalle(Long id) {
        Long clienteId = clienteActualActivo().getId();
        AliasCobro aliasCobro = buscarPropio(id, clienteId);
        List<Rifa> rifas = rifaRepositorio.findAsignadasAAlias(clienteId, aliasCobro.getId(), aliasCobro.getAlias());
        Map<Long, TotalesRifa> totales = totalesPorRifa(rifas);
        return new AliasCobroDetalleResponse(
                aResponse(aliasCobro, totalesPorAlias(clienteId).get(aliasCobro.getId())),
                rifas.stream()
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
        return aResponse(aliasCobroRepositorio.save(aliasCobro), null);
    }

    @Transactional
    public AliasCobroResponse actualizar(Long id, AliasCobroRequest request) {
        Long clienteId = clienteActualActivo().getId();
        AliasCobro aliasCobro = buscarPropio(id, clienteId);
        String aliasNormalizado = normalizarAlias(request.alias());
        validarAliasDisponible(clienteId, aliasNormalizado, id);
        aplicar(aliasCobro, request, aliasNormalizado);
        return aResponse(aliasCobro, totalesPorAlias(clienteId).get(id));
    }

    @Transactional
    public AliasCobroResponse cambiarEstado(Long id, boolean activo) {
        Long clienteId = clienteActualActivo().getId();
        AliasCobro aliasCobro = buscarPropio(id, clienteId);
        aliasCobro.setActivo(activo);
        return aResponse(aliasCobro, totalesPorAlias(clienteId).get(id));
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

    private Map<Long, TotalesAlias> totalesPorAlias(Long clienteId) {
        Map<Long, TotalesAlias> totales = new HashMap<>();
        compraRepositorio.sumarAprobadasPorAliasCobro(clienteId, EstadoCompra.APROBADA)
                .forEach(row -> totales.put(
                        (Long) row[0],
                        new TotalesAlias((Long) row[1], (BigDecimal) row[2])
                ));
        return totales;
    }

    private AliasCobroResponse aResponse(AliasCobro aliasCobro, TotalesAlias totales) {
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
                datos.comprasAprobadas(),
                datos.recaudacionAprobada()
        );
    }

    private Map<Long, TotalesRifa> totalesPorRifa(List<Rifa> rifas) {
        Map<Long, TotalesRifa> totales = new HashMap<>();
        List<Long> rifaIds = rifas.stream().map(Rifa::getId).toList();
        if (rifaIds.isEmpty()) {
            return totales;
        }
        compraRepositorio.resumirPorRifas(rifaIds).forEach(row -> {
            Long rifaId = (Long) row[0];
            EstadoCompra estado = (EstadoCompra) row[1];
            Long cantidad = (Long) row[2];
            BigDecimal total = (BigDecimal) row[3];
            TotalesRifa actual = totales.getOrDefault(rifaId, TotalesRifa.vacio());
            totales.put(rifaId, actual.con(estado, cantidad, total));
        });
        return totales;
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
