package com.pescadoresargentinos.rifas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
class RifaFlujoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void superAdminEliminaClienteConTodosSusDatos() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "borrar-" + suffix;
        String superToken = login();
        String clienteResponse = mockMvc.perform(post("/api/super-admin/clientes")
                        .header("Authorization", "Bearer " + superToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Cliente a borrar",
                                  "slug": "cliente-borrar-%s",
                                  "username": "%s",
                                  "password": "admin123"
                                }
                                """.formatted(suffix, username)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer clienteId = com.jayway.jsonpath.JsonPath.read(clienteResponse, "$.id");
        String clienteToken = login(username, "admin123");

        String rifaResponse = mockMvc.perform(post("/api/admin/rifas")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Rifa a borrar",
                                  "slug": "rifa-borrar-%s",
                                  "cantidadNumeros": 10,
                                  "numerosPorFila": 1,
                                  "numeroInicial": 0,
                                  "cantidadGanadores": 1,
                                  "valorNumero": 1000,
                                  "aliasTransferencia": "borrar.alias",
                                  "whatsappComprobante": "5491112345678",
                                  "premios": [{"posicion": 1, "descripcion": "Premio"}]
                                }
                                """.formatted(suffix)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer rifaId = com.jayway.jsonpath.JsonPath.read(rifaResponse, "$.id");

        mockMvc.perform(patch("/api/admin/rifas/{id}/publicar", rifaId)
                        .header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/rifas/{id}/compras", rifaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Comprador a borrar", "telefono": "1133334444", "numeros": [0]}
                                """))
                .andExpect(status().isOk());

        Long compradorId = jdbcTemplate.queryForObject(
                "select comprador_id from compra where rifa_id = ?", Long.class, rifaId);
        mockMvc.perform(delete("/api/super-admin/clientes/{id}", clienteId)
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk());

        assertEquals(0, jdbcTemplate.queryForObject("select count(*) from cliente where id = ?", Integer.class, clienteId));
        assertEquals(0, jdbcTemplate.queryForObject("select count(*) from usuario where cliente_id = ?", Integer.class, clienteId));
        assertEquals(0, jdbcTemplate.queryForObject("select count(*) from refresh_token where username = ?", Integer.class, username));
        assertEquals(0, jdbcTemplate.queryForObject("select count(*) from rifa where cliente_id = ?", Integer.class, clienteId));
        assertEquals(0, jdbcTemplate.queryForObject("select count(*) from alias_cobro where cliente_id = ?", Integer.class, clienteId));
        assertEquals(0, jdbcTemplate.queryForObject("select count(*) from comprador where id = ?", Integer.class, compradorId));
    }

    @Test
    void completaFlujoPrincipalDeRifa() throws Exception {
        String token = crearClienteYLogin("flujo");
        String rifaJson = """
                {
                  "titulo": "Rifa Pescadores Argentinos",
                  "slug": "rifa-flujo-%s",
                  "descripcion": "Premios de prueba",
                  "cantidadNumeros": 100,
                  "numerosPorFila": 1,
                  "numeroInicial": 0,
                  "cantidadGanadores": 2,
                  "valorNumero": 1500,
                  "aliasTransferencia": "pescadores.alias",
                  "whatsappComprobante": "5491112345678",
                  "premios": [
                    {"posicion": 1, "descripcion": "Primer premio"},
                    {"posicion": 2, "descripcion": "Segundo premio"}
                  ]
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        Integer rifaId = mockMvc.perform(post("/api/admin/rifas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rifaJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeros", hasSize(100)))
                .andExpect(jsonPath("$.numeros[0].etiqueta").value("00"))
                .andExpect(jsonPath("$.numeros[99].etiqueta").value("99"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .lines()
                .findFirst()
                .map(body -> com.jayway.jsonpath.JsonPath.<Integer>read(body, "$.id"))
                .orElseThrow();

        String rifaEditadaJson = """
                {
                  "titulo": "Rifa Pescadores Argentinos Editada",
                  "slug": "rifa-flujo-editada-%s",
                  "descripcion": "Premios de prueba editados",
                  "cantidadNumeros": 100,
                  "numerosPorFila": 1,
                  "numeroInicial": 0,
                  "cantidadGanadores": 2,
                  "valorNumero": 1500,
                  "aliasTransferencia": "pescadores.alias",
                  "whatsappComprobante": "5491112345678",
                  "premios": [
                    {"posicion": 1, "descripcion": "Primer premio"},
                    {"posicion": 2, "descripcion": "Segundo premio"}
                  ]
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        mockMvc.perform(put("/api/admin/rifas/{id}", rifaId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rifaEditadaJson))
                .andExpect(status().isOk())
                        .andExpect(jsonPath("$.titulo").value("Rifa Pescadores Argentinos Editada"));
        String slugEditado = mockMvc.perform(get("/api/rifas/{id}", rifaId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .lines()
                .findFirst()
                .map(body -> com.jayway.jsonpath.JsonPath.<String>read(body, "$.slug"))
                .orElseThrow();

        mockMvc.perform(patch("/api/admin/rifas/{id}/publicar", rifaId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PUBLICADA"));

        mockMvc.perform(get("/api/rifas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(rifaId));

        String compraJson = """
                {
                  "nombre": "Juan Perez",
                  "telefono": "1133334444",
                  "numeros": [0, 1]
                }
                """;

        Integer compraId = mockMvc.perform(post("/api/rifas/slug/{slug}/compras", slugEditado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(compraJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"))
                .andExpect(jsonPath("$.numeros[0]").value("00"))
                .andExpect(jsonPath("$.numeros[1]").value("01"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .lines()
                .findFirst()
                .map(body -> com.jayway.jsonpath.JsonPath.<Integer>read(body, "$.id"))
                .orElseThrow();

        mockMvc.perform(get("/api/rifas/{id}", rifaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeros[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.numeros[1].estado").value("PENDIENTE"));

        mockMvc.perform(patch("/api/admin/compras/{id}/aprobar", compraId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));

        mockMvc.perform(get("/api/admin/rifas/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comprasAprobadas").value(1))
                .andExpect(jsonPath("$.numerosVendidos").value(2))
                .andExpect(jsonPath("$.recaudacionAprobada").value(3000.0));

        mockMvc.perform(patch("/api/admin/rifas/{id}/finalizar", rifaId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADA"));

        String ganadoresJson = """
                {
                  "ganadores": [
                    {"posicion": 1, "numero": 0},
                    {"posicion": 2, "numero": 1}
                  ]
                }
                """;

        mockMvc.perform(post("/api/admin/rifas/{id}/ganadores", rifaId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ganadoresJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ganadores[0].numero").value("00"))
                .andExpect(jsonPath("$.ganadores[0].nombreComprador").value("Juan Perez"));
    }

    @Test
    void generaFilasDesdeCeroOUnoSegunLaConfiguracion() throws Exception {
        String token = crearClienteYLogin("numeracion");

        verificarNumeracion(token, 100, 1, 0, 100,
                new String[]{"00"}, new String[]{"99"});
        verificarNumeracion(token, 100, 2, 0, 50,
                new String[]{"00", "50"}, new String[]{"49", "99"});
        verificarNumeracion(token, 99, 3, 1, 33,
                new String[]{"01", "34", "67"}, new String[]{"33", "66", "99"});
    }

    @Test
    void cancelaRifaPublicada() throws Exception {
        String token = crearClienteYLogin("cancelar");
        String rifaJson = """
                {
                  "titulo": "Rifa a cancelar",
                  "slug": "rifa-cancelar-%s",
                  "descripcion": "Prueba",
                  "cantidadNumeros": 10,
                  "numerosPorFila": 1,
                  "numeroInicial": 0,
                  "cantidadGanadores": 1,
                  "valorNumero": 1000,
                  "aliasTransferencia": "cancelar.alias",
                  "whatsappComprobante": "5491112345678",
                  "premios": [
                    {"posicion": 1, "descripcion": "Premio"}
                  ]
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        Integer rifaId = mockMvc.perform(post("/api/admin/rifas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rifaJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .lines()
                .findFirst()
                .map(body -> com.jayway.jsonpath.JsonPath.<Integer>read(body, "$.id"))
                .orElseThrow();

        mockMvc.perform(patch("/api/admin/rifas/{id}/publicar", rifaId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/rifas/{id}/cancelar", rifaId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    @Test
    void noPermiteCompraConcurrenteDelMismoNumero() throws Exception {
        String token = crearClienteYLogin("concurrente");
        String slug = "rifa-concurrente-" + UUID.randomUUID().toString().substring(0, 8);
        String rifaJson = """
                {
                  "titulo": "Rifa concurrente",
                  "slug": "%s",
                  "descripcion": "Prueba",
                  "cantidadNumeros": 10,
                  "numerosPorFila": 1,
                  "numeroInicial": 0,
                  "cantidadGanadores": 1,
                  "valorNumero": 1000,
                  "aliasTransferencia": "concurrente.alias",
                  "whatsappComprobante": "5491112345678",
                  "premios": [
                    {"posicion": 1, "descripcion": "Premio"}
                  ]
                }
                """.formatted(slug);

        Integer rifaId = mockMvc.perform(post("/api/admin/rifas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rifaJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .lines()
                .findFirst()
                .map(body -> com.jayway.jsonpath.JsonPath.<Integer>read(body, "$.id"))
                .orElseThrow();
        mockMvc.perform(patch("/api/admin/rifas/{id}/publicar", rifaId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        CountDownLatch inicio = new CountDownLatch(1);
        AtomicInteger exitosas = new AtomicInteger();
        AtomicInteger rechazadas = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(2);

        Runnable intentoCompra = () -> {
            try {
                inicio.await();
                String compraJson = """
                        {
                          "nombre": "Cliente",
                          "telefono": "1133334444",
                          "numeros": [0]
                        }
                        """;
                int status = mockMvc.perform(post("/api/rifas/slug/{slug}/compras", slug)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(compraJson))
                        .andReturn()
                        .getResponse()
                        .getStatus();
                if (status == 200) {
                    exitosas.incrementAndGet();
                } else {
                    rechazadas.incrementAndGet();
                }
            } catch (Exception exception) {
                rechazadas.incrementAndGet();
            }
        };

        executor.submit(intentoCompra);
        executor.submit(intentoCompra);
        inicio.countDown();
        executor.shutdown();
        org.assertj.core.api.Assertions.assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        org.assertj.core.api.Assertions.assertThat(exitosas.get()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(rechazadas.get()).isEqualTo(1);
        mockMvc.perform(get("/api/rifas/slug/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeros[0].estado").value("PENDIENTE"));
    }

    @Test
    void renuevaTokenConRefreshTokenYRevocaElAnterior() throws Exception {
        String loginJson = """
                {
                  "username": "superadmin",
                  "password": "admin123"
                }
                """;
        String loginBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken = com.jayway.jsonpath.JsonPath.read(loginBody, "$.refreshToken");
        String refreshJson = """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isUnauthorized());
    }

    private String login() throws Exception {
        return login("superadmin", "admin123");
    }

    private void verificarNumeracion(
            String token,
            int cantidadNumeros,
            int numerosPorFila,
            int numeroInicial,
            int cantidadFilas,
            String[] primeraFila,
            String[] ultimaFila
    ) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String request = """
                {
                  "titulo": "Prueba numeracion",
                  "slug": "numeracion-%s",
                  "descripcion": "Prueba",
                  "cantidadNumeros": %d,
                  "numerosPorFila": %d,
                  "numeroInicial": %d,
                  "cantidadGanadores": 1,
                  "valorNumero": 1000,
                  "aliasTransferencia": "prueba.alias",
                  "whatsappComprobante": "5491112345678",
                  "premios": [{"posicion": 1, "descripcion": "Premio"}]
                }
                """.formatted(suffix, cantidadNumeros, numerosPorFila, numeroInicial);

        String response = mockMvc.perform(post("/api/admin/rifas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadFilas").value(cantidadFilas))
                .andExpect(jsonPath("$.numeros", hasSize(cantidadFilas)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(java.util.List.of(primeraFila),
                com.jayway.jsonpath.JsonPath.<java.util.List<String>>read(response, "$.numeros[0].numerosIncluidos"));
        assertEquals(java.util.List.of(ultimaFila),
                com.jayway.jsonpath.JsonPath.<java.util.List<String>>read(response, "$.numeros[" + (cantidadFilas - 1) + "].numerosIncluidos"));

        Integer rifaId = com.jayway.jsonpath.JsonPath.read(response, "$.id");
        String slug = com.jayway.jsonpath.JsonPath.read(response, "$.slug");
        mockMvc.perform(patch("/api/admin/rifas/{id}/publicar", rifaId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String compra = """
                {
                  "nombre": "Comprador inicial",
                  "telefono": "1133334444",
                  "numeros": [%d]
                }
                """.formatted(numeroInicial);
        mockMvc.perform(post("/api/rifas/slug/{slug}/compras", slug)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(compra))
                .andExpect(status().isOk());
    }

    private String crearClienteYLogin(String prefijo) throws Exception {
        String superToken = login();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = prefijo + "-" + suffix;
        String crearClienteJson = """
                {
                  "nombre": "Cliente %s",
                  "slug": "cliente-%s",
                  "username": "%s",
                  "password": "admin123"
                }
                """.formatted(prefijo, suffix, username);
        mockMvc.perform(post("/api/super-admin/clientes")
                        .header("Authorization", "Bearer " + superToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearClienteJson))
                .andExpect(status().isOk());
        return login(username, "admin123");
    }

    private String login(String username, String password) throws Exception {
        String loginJson = """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);

        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .lines()
                .findFirst()
                .map(body -> com.jayway.jsonpath.JsonPath.<String>read(body, "$.token"))
                .orElseThrow();
    }
}
