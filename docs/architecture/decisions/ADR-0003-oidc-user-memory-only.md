# ADR-0003: Mantener el usuario OIDC únicamente en memoria

- **Estado:** Aceptado para el cliente React actual
- **Fecha:** 2026-10-01
- **Alcance:** almacenamiento local del cliente OIDC

## Contexto

El cliente React usa Authorization Code + PKCE y envía un access token Bearer a la API. La primera versión guardaba el usuario OIDC en `sessionStorage`. Aunque limita la persistencia al ciclo de vida de la pestaña, cualquier JavaScript que se ejecute en el mismo origen puede leer ese token. El producto todavía no tiene proveedor OIDC institucional configurado ni procesa expedientes personales reales.

## Decisión

- Mantener el `access_token`, el `id_token` y el usuario OIDC en un `StateStore` basado en memoria.
- Conservar en `sessionStorage` solo el estado temporal de la transacción OIDC necesario para validar y completar el callback tras volver del proveedor.
- Mantener PKCE, no solicitar `offline_access`, no habilitar renovación silenciosa y no persistir refresh tokens ni claims de perfil innecesarios.
- Una recarga de página pierde la sesión local y requiere iniciar sesión otra vez; el proveedor puede reconocer su sesión institucional si el usuario ya la tiene abierta.
- No asumir que esta decisión elimina el riesgo de XSS: el token permanece accesible al JavaScript legítimo mientras vive la página.
- Antes de habilitar datos personales estudiantiles, aprobar con DTIC el diseño de sesión de producción, vigencia de access tokens, estrategia de CSRF si se adopta cookie `HttpOnly`, CSP y demás headers del punto de entrada.

## Consecuencias

- Reduce la exposición de tokens recuperables desde Web Storage y evita restaurarlos al abrir otra página o sesión.
- La experiencia puede requerir autenticación de nuevo después de recargar; esa pérdida de persistencia es deliberada en esta etapa.
- La integración de producción queda bloqueada hasta validar el contrato OIDC y la estrategia de defensa del navegador y del edge; los permisos administrativos continúan cerrados mientras falte issuer, audience y mapeo autorizado.

## Verificación

- Las pruebas de React confirman que el usuario OIDC no se escribe en `sessionStorage` y que el estado transaccional sigue disponible para el callback.
- El backend valida firma, issuer y audience del JWT y mantiene la autorización en el servidor, con denegación por defecto.
