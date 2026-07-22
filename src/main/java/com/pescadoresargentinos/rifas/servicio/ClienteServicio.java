package com.pescadoresargentinos.rifas.servicio;

import com.pescadoresargentinos.rifas.api.dto.ActualizarClienteRequest;
import com.pescadoresargentinos.rifas.api.dto.ActualizarEstadoClienteRequest;
import com.pescadoresargentinos.rifas.api.dto.ActualizarMarcaClienteRequest;
import com.pescadoresargentinos.rifas.api.dto.ClienteResponse;
import com.pescadoresargentinos.rifas.api.dto.CrearClienteRequest;
import com.pescadoresargentinos.rifas.dominio.Cliente;
import com.pescadoresargentinos.rifas.dominio.EstadoCliente;
import com.pescadoresargentinos.rifas.dominio.RolUsuario;
import com.pescadoresargentinos.rifas.dominio.Usuario;
import com.pescadoresargentinos.rifas.repositorio.ClienteRepositorio;
import com.pescadoresargentinos.rifas.repositorio.UsuarioRepositorio;
import com.pescadoresargentinos.rifas.seguridad.UsuarioActual;
import com.pescadoresargentinos.rifas.servicio.storage.MediaGuardado;
import com.pescadoresargentinos.rifas.servicio.storage.MediaStorage;
import com.pescadoresargentinos.rifas.util.TelefonoArgentina;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteServicio {

    private final ClienteRepositorio clienteRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioActual usuarioActual;
    private final MediaStorage mediaStorage;
    private final JdbcTemplate jdbcTemplate;

    public ClienteServicio(
            ClienteRepositorio clienteRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            PasswordEncoder passwordEncoder,
            UsuarioActual usuarioActual,
            MediaStorage mediaStorage,
            JdbcTemplate jdbcTemplate
    ) {
        this.clienteRepositorio = clienteRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.usuarioActual = usuarioActual;
        this.mediaStorage = mediaStorage;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ClienteResponse crear(CrearClienteRequest request) {
        if (clienteRepositorio.existsBySlug(request.slug())) {
            throw new IllegalArgumentException("Ya existe un cliente con ese slug");
        }
        if (usuarioRepositorio.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese username");
        }

        Cliente cliente = new Cliente();
        cliente.setNombre(request.nombre());
        cliente.setSlug(request.slug());
        cliente.setColorPrincipal(normalizarColor(request.colorPrincipal()));
        cliente.setLogoUrl(normalizarTextoOpcional(request.logoUrl()));
        aplicarConfiguracionWhatsapp(cliente, request.twilioWhatsappHabilitado(), request.twilioWhatsappFrom(),
                request.twilioMessagingServiceSid(), request.twilioContentSid(), request.whatsappConsultas());
        cliente.setEstado(EstadoCliente.ACTIVO);
        cliente = clienteRepositorio.save(cliente);

        Usuario usuario = new Usuario();
        usuario.setUsername(request.username());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setRol(RolUsuario.CLIENTE_ADMIN);
        usuario.setCliente(cliente);
        usuarioRepositorio.save(usuario);

        return aResponse(cliente, usuario);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clienteRepositorio.findAll().stream()
                .map(cliente -> {
                    Usuario usuario = usuarioRepositorio.findByClienteId(cliente.getId()).orElse(null);
                    return aResponse(cliente, usuario);
                })
                .toList();
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ActualizarClienteRequest request) {
        Cliente cliente = buscarCliente(id);
        Usuario usuario = usuarioRepositorio.findByClienteId(cliente.getId())
                .orElseThrow(() -> new IllegalArgumentException("El cliente no tiene usuario asociado"));

        clienteRepositorio.findBySlug(request.slug()).ifPresent(existente -> {
            if (!existente.getId().equals(id)) {
                throw new IllegalArgumentException("Ya existe un cliente con ese slug");
            }
        });
        usuarioRepositorio.findByUsername(request.username()).ifPresent(existente -> {
            if (!existente.getId().equals(usuario.getId())) {
                throw new IllegalArgumentException("Ya existe un usuario con ese username");
            }
        });

        cliente.setNombre(request.nombre());
        cliente.setSlug(request.slug());
        cliente.setColorPrincipal(normalizarColor(request.colorPrincipal()));
        cliente.setLogoUrl(normalizarTextoOpcional(request.logoUrl()));
        aplicarConfiguracionWhatsapp(cliente, request.twilioWhatsappHabilitado(), request.twilioWhatsappFrom(),
                request.twilioMessagingServiceSid(), request.twilioContentSid(), request.whatsappConsultas());
        usuario.setUsername(request.username());
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.password()));
        }

        return aResponse(cliente, usuario);
    }

    @Transactional
    public ClienteResponse actualizarEstado(Long id, ActualizarEstadoClienteRequest request) {
        Cliente cliente = buscarCliente(id);
        cliente.setEstado(request.estado());
        Usuario usuario = usuarioRepositorio.findByClienteId(cliente.getId()).orElse(null);
        return aResponse(cliente, usuario);
    }

    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = buscarCliente(id);
        Usuario usuario = usuarioRepositorio.findByClienteId(id).orElse(null);
        String username = usuario == null ? null : usuario.getUsername();

        List<Long> compradoresIds = jdbcTemplate.queryForList("""
                select distinct c.comprador_id
                from compra c
                join rifa r on r.id = c.rifa_id
                where r.cliente_id = ?
                """, Long.class, id);

        jdbcTemplate.update("delete from ganador where rifa_id in (select id from rifa where cliente_id = ?)", id);
        jdbcTemplate.update("""
                delete from numero_rifa_numeros_incluidos
                where numero_rifa_id in (
                    select nr.id from numero_rifa nr
                    join rifa r on r.id = nr.rifa_id
                    where r.cliente_id = ?
                )
                """, id);
        jdbcTemplate.update("""
                delete from compra_etiquetas_numeros
                where compra_id in (
                    select c.id from compra c
                    join rifa r on r.id = c.rifa_id
                    where r.cliente_id = ?
                )
                """, id);
        jdbcTemplate.update("""
                update numero_rifa set compra_id = null
                where rifa_id in (select id from rifa where cliente_id = ?)
                """, id);
        jdbcTemplate.update("delete from compra where rifa_id in (select id from rifa where cliente_id = ?)", id);
        compradoresIds.forEach(compradorId -> jdbcTemplate.update(
                "delete from comprador where id = ? and not exists (select 1 from compra where comprador_id = ?)",
                compradorId,
                compradorId
        ));
        jdbcTemplate.update("delete from numero_rifa where rifa_id in (select id from rifa where cliente_id = ?)", id);
        jdbcTemplate.update("delete from premio where rifa_id in (select id from rifa where cliente_id = ?)", id);
        jdbcTemplate.update("delete from rifa where cliente_id = ?", id);
        jdbcTemplate.update("delete from alias_cobro where cliente_id = ?", id);
        if (username != null) {
            jdbcTemplate.update("delete from refresh_token where username = ?", username);
        }
        jdbcTemplate.update("delete from usuario where cliente_id = ?", id);
        jdbcTemplate.update("delete from cliente where id = ?", cliente.getId());
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtenerClienteActual() {
        Cliente cliente = buscarClienteActual();
        Usuario usuario = usuarioRepositorio.findByClienteId(cliente.getId()).orElse(null);
        return aResponse(cliente, usuario);
    }

    @Transactional
    public ClienteResponse actualizarMarcaClienteActual(ActualizarMarcaClienteRequest request) {
        Cliente cliente = buscarClienteActual();
        cliente.setColorPrincipal(normalizarColor(request.colorPrincipal()));
        cliente.setLogoUrl(normalizarTextoOpcional(request.logoUrl()));
        aplicarConfiguracionWhatsapp(cliente, request.twilioWhatsappHabilitado(), request.twilioWhatsappFrom(),
                request.twilioMessagingServiceSid(), request.twilioContentSid(), request.whatsappConsultas());
        Usuario usuario = usuarioRepositorio.findByClienteId(cliente.getId()).orElse(null);
        return aResponse(cliente, usuario);
    }

    @Transactional
    public ClienteResponse subirLogoClienteActual(org.springframework.web.multipart.MultipartFile archivo) {
        Cliente cliente = buscarClienteActual();
        MediaGuardado guardado = mediaStorage.guardarImagen("logos", cliente.getId(), archivo);
        cliente.setLogoUrl(guardado.url());
        Usuario usuario = usuarioRepositorio.findByClienteId(cliente.getId()).orElse(null);
        return aResponse(cliente, usuario);
    }

    @Transactional
    public ClienteResponse subirLogoCliente(Long id, org.springframework.web.multipart.MultipartFile archivo) {
        Cliente cliente = buscarCliente(id);
        MediaGuardado guardado = mediaStorage.guardarImagen("logos", cliente.getId(), archivo);
        cliente.setLogoUrl(guardado.url());
        Usuario usuario = usuarioRepositorio.findByClienteId(cliente.getId()).orElse(null);
        return aResponse(cliente, usuario);
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe el cliente " + id));
    }

    private Cliente buscarClienteActual() {
        Long clienteId = usuarioActual.clienteId();
        if (clienteId == null) {
            throw new SecurityException("No tenes un cliente asociado");
        }
        return buscarCliente(clienteId);
    }

    private ClienteResponse aResponse(Cliente cliente, Usuario usuario) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getSlug(),
                cliente.getColorPrincipal(),
                cliente.getLogoUrl(),
                cliente.getTwilioWhatsappHabilitado(),
                cliente.getTwilioWhatsappFrom(),
                cliente.getTwilioMessagingServiceSid(),
                cliente.getTwilioContentSid(),
                cliente.getWhatsappConsultas(),
                cliente.getEstado(),
                usuario == null ? null : usuario.getUsername(),
                cliente.getFechaAlta()
        );
    }

    private void aplicarConfiguracionWhatsapp(
            Cliente cliente,
            Boolean habilitado,
            String from,
            String messagingServiceSid,
            String contentSid,
            String consultas
    ) {
        cliente.setTwilioWhatsappHabilitado(Boolean.TRUE.equals(habilitado));
        cliente.setTwilioWhatsappFrom(normalizarTelefonoWhatsapp(from));
        cliente.setTwilioMessagingServiceSid(normalizarTextoOpcional(messagingServiceSid));
        cliente.setTwilioContentSid(normalizarTextoOpcional(contentSid));
        cliente.setWhatsappConsultas(TelefonoArgentina.normalizarOpcional(consultas));
    }

    private String normalizarColor(String colorPrincipal) {
        return colorPrincipal == null || colorPrincipal.isBlank() ? "#082d50" : colorPrincipal.trim();
    }

    private String normalizarTextoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String normalizarTelefonoWhatsapp(String telefono) {
        String valor = normalizarTextoOpcional(telefono);
        if (valor == null) {
            return null;
        }
        String normalizado = valor.replace("whatsapp:", "").replace("+", "").replaceAll("\\D", "");
        return normalizado.isBlank() ? null : normalizado;
    }
}
