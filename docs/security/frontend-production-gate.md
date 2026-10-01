# Gate de seguridad web antes de producción

**Estado:** pendiente. La configuración disponible ejecuta una vista previa local; no define alojamiento productivo ni autoriza el tratamiento de datos personales.
**Revisión de código:** 2026-10-01. Lectura estática de React/TypeScript, identidad OIDC, Compose y controles HTTP visibles. No es una prueba de penetración ni una auditoría exhaustiva del backend Java.

## Evidencia revisada

- `frontend/src/features/identity/oidcConfiguration.ts` exige un issuer HTTPS salvo loopback local y fija los callback de autenticación y cierre de sesión al origen/ruta exactos de la aplicación. `IdentityProvider.tsx` restaura solo fragmentos de ruta conocidos.
- `frontend/src/features/identity/identitySessionManager.ts` deja el estado transaccional OIDC en `sessionStorage`, conserva el usuario en memoria, no habilita renovación silenciosa y mantiene PKCE. Los valores con prefijo `VITE_` son configuración pública del cliente; no se deben usar para secretos.
- La búsqueda estática en `frontend/src` no encontró `dangerouslySetInnerHTML`, sinks `innerHTML`/`document.write`, ejecución dinámica con `eval`/`new Function`, ni manejadores `postMessage`.
- `backend/src/main/java/co/edu/uptc/universiry/security/SecurityConfiguration.java` usa tokens Bearer, sesiones sin estado, verificación de issuer/audience y `denyAll` para rutas no declaradas. La desactivación de CSRF está ligada al contrato actual sin autenticación por cookies; si el contrato cambia, debe reevaluarse.
- `BrandAssetController.java` sirve imágenes ya validadas con `nosniff` y CSP aislada. Ese encabezado protege los recursos de imagen y no configura la política de la aplicación React.
- `compose.yaml` enlaza los puertos de preview a `127.0.0.1` y las imágenes de frontend/backend usan Dockerfiles de desarrollo. No se encontró configuración de hosting, proxy o despliegue público que demuestre cabeceras para el documento React.

## Condiciones previas a cualquier publicación

1. Acordar con DTIC el dominio HTTPS, terminación TLS, origen del frontend, topología de API/OIDC y proxy confiable. El Compose de desarrollo no define esos límites.
2. Configurar en el servidor o proxy una CSP para el HTML y validar primero su modo `Content-Security-Policy-Report-Only` en el build y flujo de autenticación reales. La política final debe permitir solo los orígenes aprobados, impedir objetos y framing, y no ampliar permisos con `unsafe-eval`, comodines o scripts de terceros sin una necesidad revisada. `frame-ancestors` debe llegar como encabezado HTTP.
3. Configurar y comprobar también `X-Content-Type-Options: nosniff` y una política `Referrer-Policy` apropiada en las respuestas de aplicación. Si frontend y API quedan en orígenes distintos, aprobar una lista CORS exacta; no inferirla del proxy local.
4. Verificar sobre el hostname final las respuestas HTTP de HTML, JavaScript, API y errores; probar login, callback, logout, expiración, 401/403 y navegación con CSP aplicada. No basta con revisar `index.html` o que el build termine.
5. Mantener issuer, audience, callback, claims y permisos aprobados por DTIC; ningún secreto puede ir en variables `VITE_` ni en el bundle. La configuración sin valores OIDC permanece cerrada para operaciones administrativas.
6. Antes de datos reales, aprobar la arquitectura productiva de almacenamiento de imágenes, copias, restauración, permisos y conciliación. El volumen local de desarrollo no es un repositorio durable multiinstancia; ver [operación de imágenes institucionales](brand-assets.md).

Hasta cumplir estos puntos, el estado correcto es **preview local y datos vacíos/sintéticos**, no una plataforma lista para producción.

## Referencias técnicas

- [Vite: variables de entorno y modos](https://vite.dev/guide/env-and-mode): las variables `VITE_` se exponen en el bundle del cliente.
- [MDN: implementación de Content Security Policy](https://developer.mozilla.org/en-US/docs/Web/Security/Practical_implementation_guides/CSP): recomienda validar la política en modo report-only antes de aplicarla.
- [MDN: guía de Content Security Policy](https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CSP): describe la CSP como encabezado de respuesta y sus directivas para recursos y framing.
