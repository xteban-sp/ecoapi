# EcoAPI — Servicio de pesajes en ruta (EcoRecicla)

Backend del Examen Parcial U2 (Versión B). Spring Boot 4 + Kotlin, Spring Security con JWT (HS256, 1 h), JPA, Flyway y H2 (base relacional en memoria).

## Ejecución (un solo comando)

```bash
./gradlew bootRun
```

Queda en `http://localhost:8080/api/v1/`. Desde el emulador Android: `http://10.0.2.2:8080/api/v1/`.
Cada arranque recrea el esquema y la semilla (Flyway), así la "ruta del día" siempre corresponde a la fecha actual.

Requisitos: JDK 21.

## Usuarios de prueba (contraseña `Eco2026*`, guardada con BCrypt)

| Usuario | Rol | Uso en la demo |
|---|---|---|
| operario1 | OPERARIO | Dueño de la ruta de hoy y de la de ayer |
| supervisor1 | SUPERVISOR | Anula, corrige todo y cierra la ruta |
| admin | ADMINISTRADOR | No registra pesajes (403); no tiene ruta (404) |
| operario2 | OPERARIO | Autor del pesaje 15, sirve para demostrar RN-02 |
| auditor1 | AUDITOR | Solo lectura (SC-B) |

Semilla: 12 puntos en 3 zonas, 2 rutas (ayer CERRADA → RN-01; hoy ABIERTA con 6 puntos, 3 sin pesar → RN-04), 15 pesajes (2 anulados, 1 de operario2).

## Estructura de paquetes

```
src/main/kotlin/pe/upeu/ecorecicla/
  config/       SecurityConfig.kt (matriz ruta→privilegio), JwtAuthenticationFilter.kt,
                JwtService.kt, ManejadorDeErrores.kt (formato de error común)
  seguridad/    Usuario.kt, Rol.kt, Privilegio.kt, UsuarioRepository.kt, AuthController.kt
  dominio/      Entidades.kt (PuntoAcopio, Ruta, RutaPunto, Pesaje), Excepciones.kt
  repositorio/  Repositorios.kt (Spring Data JPA)
  servicio/     PesajeService (RN-01, RN-02, RN-03), RutaService (RN-04), AutenticacionService, PuntoAcopioService
  web/          Controladores.kt (REST bajo /api/v1)
    dto/        Dtos.kt, Comunes.kt (PaginaResponse, ErrorResponseDto)
  Aplicacion.kt
src/main/resources/db/migration/  V1__esquema.sql, V2__datos_semilla.sql
```

## Contrato del API

Todas las rutas, salvo el login, exigen `Authorization: Bearer <token>`.

| Operación | Verbo y ruta | Privilegio | Éxito | Errores |
|---|---|---|---|---|
| Inicio de sesión | `POST /api/v1/auth/login` | Público | 200 | 400, 401 |
| Usuario actual | `GET /api/v1/auth/me` | Autenticado | 200 | 401 |
| Ruta del día | `GET /api/v1/rutas/del-dia?fecha=AAAA-MM-DD` | PUNTO_LEER | 200 | 401, 403, 404 |
| Pesajes de una ruta | `GET /api/v1/rutas/{id}/pesajes?pagina&tamanio` | PESAJE_LEER | 200 (paginado) | 401, 403, 404 |
| Cerrar ruta | `POST /api/v1/rutas/{id}/cierre` | RUTA_CERRAR | 200 | 403, 404, 409 (RN-04) |
| Omitir punto | `PUT /api/v1/rutas/{id}/puntos/{puntoId}/omision` | PESAJE_CREAR | 200 | 400, 404, 409 |
| Puntos de acopio | `GET /api/v1/puntos?zona&pagina&tamanio` | PUNTO_LEER | 200 (paginado) | 401, 403 |
| Crear punto | `POST /api/v1/puntos` | PUNTO_GESTIONAR | 201 | 400, 403 |
| Pesajes de un punto | `GET /api/v1/puntos/{id}/pesajes?pagina&tamanio` | PESAJE_LEER | 200 (paginado, reciente primero) | 404 |
| Obtener pesaje | `GET /api/v1/pesajes/{id}` | PESAJE_LEER | 200 | 404 |
| Registrar pesaje | `POST /api/v1/pesajes` | PESAJE_CREAR | 201 + Location | 400 (RN-03), 403, 404, 409 (RN-01) |
| Corregir pesaje | `PUT /api/v1/pesajes/{id}` | PESAJE_EDITAR | 200 | 400, 403 (RN-02), 404, 409 (RN-01) |
| Anular pesaje | `DELETE /api/v1/pesajes/{id}?motivo=` | PESAJE_ANULAR | 204 (anulación lógica) | 403, 404, 409 |

Cualquier fallo no controlado → 500 con mensaje genérico.

Registrar pesaje (el cliente **no** envía el neto):
```json
{ "rutaId": 2, "puntoId": 4, "material": "CARTON", "pesoBruto": 120.5, "tara": 8, "observacion": "", "humedadEstimada": 12.5 }
```

Envoltorio de paginación y formato de error común:
```json
{ "contenido": [], "pagina": 0, "tamanio": 20, "totalElementos": 37, "totalPaginas": 2, "ultima": false }
{ "timestamp": "2026-10-27T09:14:22", "status": 400, "error": "Bad Request", "message": "Datos invalidos",
  "path": "/api/v1/pesajes", "validationErrors": { "tara": "La tara debe ser menor que el peso bruto..." } }
```

## Roles y privilegios

La asignación rol→privilegio vive en la base (`rol_privilegio`, semilla V2). El JWT transporta `sub`, `uid`, `nombre`, `rol` y `privilegios`; el filtro convierte los privilegios en authorities y `SecurityConfig` exige `hasAuthority(PRIVILEGIO)` por ruta. Ningún controlador compara nombres de rol.

| Privilegio | OPERARIO | SUPERVISOR | ADMIN | AUDITOR |
|---|---|---|---|---|
| PUNTO_LEER | Sí | Sí | Sí | Sí |
| PESAJE_LEER | Sí | Sí | Sí | Sí |
| PESAJE_CREAR | Sí | Sí | No | No |
| PESAJE_EDITAR | Solo los propios (RN-02) | Todos | No | No |
| PESAJE_EDITAR_TODOS | No | Sí | No | No |
| PESAJE_ANULAR | No | Sí | No | No |
| RUTA_CERRAR | No | Sí | No | No |
| PUNTO_GESTIONAR | No | No | Sí | No |

`PESAJE_EDITAR_TODOS` modela la columna "Todos" de la matriz: así RN-02 se decide por privilegio y no por nombre de rol.

**Agregar un rol nuevo** (SC-B, rol AUDITOR): la migración `V3__rol_auditor.sql` inserta el rol en `rol`, sus privilegios de lectura en `rol_privilegio` y el usuario `auditor1`. No se tocó `SecurityConfig`, el filtro JWT ni ningún controlador: todos verifican privilegios, nunca nombres de rol. Resultado: lecturas 200, cualquier escritura 403.

## Humedad estimada (SC-C)

Campo opcional `humedadEstimada` (%) en `PesajeRequest`, `PesajeCorreccionRequest` y `PesajeResponse`. Se valida entre 0 y 100 (400 con detalle en el campo `humedadEstimada`). Migración `V4__humedad_estimada.sql`: agrega la columna con CHECK y actualiza los datos semilla con la humedad típica de cada material.

## Reglas de negocio (capa servicio/)

- **RN-01** Ruta cerrada: no se registra, corrige ni anula → 409.
- **RN-02** Sin `PESAJE_EDITAR_TODOS`, solo se corrigen pesajes propios y del mismo día → 403.
- **RN-03** Neto = bruto − tara, calculado en el servicio; si ≤ 0 → 400 con detalle en `tara`.
- **RN-04** La ruta no se cierra si algún punto no tiene pesaje vigente ni motivo de omisión → 409 con la cantidad faltante.

## Decisiones de arquitectura

- Autorización en un único lugar (`SecurityConfig`); reglas que dependen de los datos en `servicio/`; controladores sin lógica.
- 401 (token ausente/alterado/vencido) y 403 (sin privilegio) se generan en los manejadores de Spring Security con el mismo formato de error.
- Sin renovación de token: al vencer, el cliente vuelve a iniciar sesión.
- H2 en memoria para levantar con un comando; el SQL de Flyway es estándar y portable a PostgreSQL.

## Pruebas manuales

`EcoAPI.postman_collection.json` incluye login, ejemplos de 401 (sin token y token alterado), 403, 400, 409 y el CRUD completo. Importar en Postman y ejecutar en orden (los tokens se guardan en variables).
