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

    public ClienteServicio(
            ClienteRepositorio clienteRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            PasswordEncoder passwordEncoder,
            UsuarioActual usuarioActual,
            MediaStorage mediaStorage
    ) {
        this.clienteRepositorio = clienteRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.usuarioActual = usuarioActual;
        this.mediaStorage = mediaStorage;
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
