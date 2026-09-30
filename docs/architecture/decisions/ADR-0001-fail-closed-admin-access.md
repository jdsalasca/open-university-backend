# ADR-0001: Acceso administrativo con lista permitida y denegación predeterminada

- **Estado:** Aceptado para la base técnica local
- **Fecha:** 2026-09-29; ajuste de mapeo configurable: 2026-09-30
- **Alcance:** API Spring Boot y sesión React

## Contexto

La plataforma crece por capacidades. Una regla amplia sobre `/api/v1/admin/**` permitiría que un permiso de identidad visual alcanzara endpoints administrativos agregados después. DTIC aún no ha confirmado issuer, audience, nombre de claim, scopes ni grupos UPTC. Por tanto, la base técnica debe admitir la integración sin asignar roles institucionales ficticios.

## Decisión

- El backend recibe access tokens Bearer sin sesión ni cookie. `UPTC_OIDC_ISSUER_URI` y `UPTC_OIDC_AUDIENCE` son obligatorios para validar un token; cuando falta cualquiera, el decoder rechaza todos los tokens.
- `UPTC_OIDC_AUTHORITIES_CLAIM` elige el claim que contiene los valores de rol. `UPTC_OIDC_ROLE_PERMISSION_MAPPING` acepta un objeto JSON con la forma `{"<valor-exacto-del-claim>":["<permiso-interno>"]}`. Su valor vacío predeterminado concede cero permisos.
- Solo se aceptan los valores definidos por `ApplicationPermission`. El parseo rechaza JSON inválido, claves duplicadas, roles vacíos, listas vacías, permisos desconocidos y valores repetidos. Un rol no configurado no concede permisos por nombre parecido, prefijo o convención.
- No hay roles ni grupos de demostración. `.env.example` deja issuer, audience y mapeo vacíos; ningún nombre UPTC productivo se incluye sin aprobación de DTIC.
- Cada método y ruta administrativa se autoriza por su permiso de capacidad. Toda ruta bajo `/api/v1/admin/**` que no esté registrada se deniega.
- `GET /api/v1/me` devuelve solo subject y permisos internos resueltos en servidor y lleva `Cache-Control: no-store`. No replica claims de perfil.
- React usa Authorization Code con PKCE y consulta `/api/v1/me`; no decodifica claims para decidir permisos. La visibilidad de controles es una ayuda de interfaz; el backend aplica la autorización final.
- El navegador mantiene estado y sesión solo para la pestaña. No usa `localStorage`, `offline_access`, renovación silenciosa ni almacena refresh tokens; el usuario guardado reduce los claims del perfil a `sub`.
- Sin configuración OIDC válida, React mantiene el inicio de sesión inactivo y las capacidades administrativas cerradas.

## Consecuencias

- Una ruta nueva permanece cerrada hasta añadir método, ruta, permiso y pruebas.
- La integración UPTC continúa pendiente del contrato aprobado por DTIC y de pruebas en un ambiente institucional autorizado.
- El mecanismo permite cambios de mapeo sin recompilar el cliente, pero los valores de producción deben administrarse como configuración protegida y revisada.
- Una API de estudiantes deberá definir permisos propios por proceso y datos antes de ser expuesta a usuarios.
