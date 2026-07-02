# Rifas Backend

Backend Spring Boot para el sistema SaaS de rifas.

## Desarrollo

```bash
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

Base de datos de desarrollo:

- H2 en memoria
- Consola: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:rifas`
- Usuario: `sa`
- Password: vacio

Super admin inicial de desarrollo:

- Usuario: `superadmin`
- Password: configurar en `SUPER_ADMIN_PASSWORD` o en `application-local.properties`

Los endpoints `/api/admin/**` usan JWT. Los endpoints publicos de rifas y compra no requieren autenticacion.

Login admin:

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "superadmin",
  "password": "<password>"
}
```

Usar el `token` devuelto en endpoints admin:

```http
Authorization: Bearer <token>
```

Renovar sesion:

```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "<refreshToken>"
}
```

El refresh token es de un solo uso: al renovarlo se revoca el anterior y se entrega uno nuevo.

Cerrar sesion:

```http
POST /api/auth/logout
Content-Type: application/json

{
  "refreshToken": "<refreshToken>"
}
```

Swagger UI:

- `http://localhost:8080/swagger-ui.html`

## PostgreSQL

Perfil local:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Config local por defecto:

- Copiar `src/main/resources/application-local.example.properties` como `application-local.properties`.
- Completar credenciales de PostgreSQL, JWT, super admin y Cloudflare R2.
- `application-local.properties` esta ignorado por Git.

Perfil dev:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Variables soportadas en dev:

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `JPA_DDL_AUTO`
- `SUPER_ADMIN_USERNAME`
- `SUPER_ADMIN_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_MINUTES`
- `JWT_REFRESH_EXPIRATION_DAYS`
- `R2_ENDPOINT`
- `R2_BUCKET`
- `R2_ACCESS_KEY`
- `R2_SECRET_KEY`
- `R2_REGION`
- `MEDIA_PUBLIC_BASE_URL`

## Migraciones

El esquema se administra con Liquibase:

- Changelog principal: `src/main/resources/db/changelog/db.changelog-master.xml`
- Hibernate corre con `ddl-auto=validate`.

Para cambios de modelo, agregar un nuevo `changeSet`; no usar `ddl-auto=update` salvo para pruebas descartables.

## Flujo principal

1. El super admin crea un cliente y su unico usuario.
2. El cliente admin crea una rifa en estado `BORRADOR`, siempre asociada a su cliente.
3. El cliente admin publica la rifa.
4. El comprador entra por el link publico `/r/{slug}` y reserva uno o mas numeros.
5. Los numeros reservados pasan a `PENDIENTE`.
6. El cliente admin aprueba la compra y los numeros pasan a `VENDIDO`, o cancela la compra y vuelven a `DISPONIBLE`.
7. El cliente admin finaliza la rifa.
8. El cliente admin carga ganadores manualmente segun el resultado externo de la quiniela.

Si un cliente queda `INACTIVO`, sus rifas publicadas siguen visibles, pero no puede crear rifas nuevas.

Los numeros arrancan en `00` y se muestran con ceros a la izquierda. Para una rifa de 100 numeros se generan `00` a `99`.

## Endpoints publicos

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/rifas`
- `GET /api/rifas/finalizadas`
- `GET /api/rifas/{id}`
- `GET /api/rifas/slug/{slug}`
- `POST /api/rifas/{id}/compras`
- `POST /api/rifas/slug/{slug}/compras`

## Endpoints super admin

- `GET /api/super-admin/clientes`
- `POST /api/super-admin/clientes`
- `PATCH /api/super-admin/clientes/{id}/estado`

## Endpoints admin

- `GET /api/admin/rifas`
- `GET /api/admin/rifas/dashboard`
- `POST /api/admin/rifas`
- `PUT /api/admin/rifas/{id}`
- `PATCH /api/admin/rifas/{id}/publicar`
- `PATCH /api/admin/rifas/{id}/finalizar`
- `PATCH /api/admin/rifas/{id}/cancelar`
- `POST /api/admin/rifas/{id}/ganadores`
- `GET /api/admin/compras`
- `PATCH /api/admin/compras/{id}/aprobar`
- `PATCH /api/admin/compras/{id}/cancelar`

## Tests

```bash
mvn test
```
