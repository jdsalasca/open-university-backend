# ADR-0001: Acceso administrativo con lista permitida y denegación predeterminada

- **Estado:** Aceptado para la base técnica local
- **Fecha:** 2026-09-29
- **Alcance:** API del monolito Spring Boot

## Contexto

La plataforma crecerá por capacidades. Una regla amplia sobre `/api/v1/admin/**` haría que un permiso creado para identidad visual también alcanzara endpoints administrativos agregados después. Los grupos y claims reales de la UPTC aún deben ser confirmados por los responsables institucionales.

## Decisión

- Validar access tokens como Bearer sin sesión/cookie; el issuer y audience son obligatorios. Si faltan, el decoder rechaza el token.
- Leer el claim de autoridades con nombre configurable y aceptar únicamente las autoridades que el backend incluya expresamente en su lista permitida.
- Autorizar pares de método/ruta por capacidad; las solicitudes bajo `/api/v1/admin/**` que no estén registradas se deniegan.
- Actualmente, `BRAND_ADMIN` e `INSTITUTIONAL_ADMIN` pueden usar `GET` y `PUT /api/v1/admin/branding`, `POST /api/v1/admin/branding/rollback` y `POST /api/v1/admin/branding/assets`.
- Probar acceso permitido y denegado en el servidor. La visibilidad de controles del frontend es solo una ayuda de experiencia.
- No fijar el nombre real del claim ni afirmar que los dos nombres de autoridad coinciden con grupos productivos UPTC hasta recibir el contrato del proveedor institucional.

## Consecuencias

- Un endpoint nuevo queda cerrado hasta añadir explícitamente método, ruta, autoridad y pruebas.
- La integración del proveedor institucional sigue pendiente de issuer, audience, claim y mapeo de roles aprobados.
- Una API de estudiantes deberá definir permisos propios y separar lectura, operación y administración según el propietario institucional del proceso.
