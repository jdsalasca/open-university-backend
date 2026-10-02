# Sesión de desarrollador para preview local

**Estado:** autorizado para desarrollo local por el patrocinador; no habilita acceso ni datos institucionales.
**Fecha:** 2 de octubre de 2026.

## Objetivo

Permitir revisar y ejercitar las consolas ya implementadas en Docker Compose sin esperar la integración OIDC institucional. El preview ofrece una identidad sintética de desarrollador únicamente en el perfil Spring `local-preview` y en el servidor Vite de desarrollo.

## Decisiones

- Compose activa explícitamente `local-preview`; las demás configuraciones Spring no registran sus controladores, beans ni conversores de sesión local.
- La API local emite un bearer opaco aleatorio de 256 bits, con vencimiento de cuatro horas. Solo existe en memoria del proceso, admite hasta 16 sesiones concurrentes y se revoca al cerrar sesión. No es un token OIDC ni se acepta fuera del decoder local.
- El principal sintético usa el issuer fijo `https://local-preview.universiry.invalid` y el subject `local-preview-developer`. `/api/v1/me` reutiliza el contrato actual y crea, si hace falta, su identidad canónica mínima en la base local. No crea asignaciones ni perfiles de usuario en la base de datos.
- El conversor de autoridades local reconoce solo ese principal y concede los permisos actualmente allowlisted por `ApplicationPermission`. La autoridad no se puede presentar en OIDC ni cambiar desde el cliente.
- React ofrece «Entrar al preview local» solo durante Vite DEV cuando OIDC está sin configurar. Consulta `/api/v1/me` para permisos, conserva el bearer solo en memoria, muestra una identificación visual permanente del modo local y revoca al salir.
- Los secretos, tokens, identidades, grupos y datos UPTC no se agregan al código ni a archivos `.env`. Los datos que se creen quedan en el MySQL local que ya conserva Compose.
- Las imágenes de despliegue normal no activan `local-preview`; producción conserva la validación OIDC fail-closed. El build frontend de producción excluye tanto el cliente HTTP local como su lógica de sesión.

## Respuestas observables

- Sin `local-preview`, crear sesión local no autentica ni abre rutas administrativas; los valores vacíos de issuer/audience siguen rechazando tokens.
- Con `local-preview`, crear una sesión devuelve `accessToken` y `expiresAt`, con `Cache-Control: no-store` y sin cookies. Usar ese bearer en `/api/v1/me` devuelve el subject sintético y permisos de servidor.
- Bearers desconocidos, vencidos o revocados fallan con 401. Salir elimina la sesión del proceso y limpia el token de React.
- Al recargar el navegador no se restaura la sesión: la persona vuelve a pulsar el acceso local.

## Límites

Este rol técnico existe solo para preview y pruebas con datos sintéticos. No es un perfil funcional UPTC, no prueba MFA, revocación institucional, ámbitos, provisión de personal ni aceptación de negocio; no autoriza acceso a entornos institucionales.
