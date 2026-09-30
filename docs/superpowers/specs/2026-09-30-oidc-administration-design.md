# Acceso administrativo federado — diseño

**Estado:** mecanismo genérico implementado en `develop`; issuer, audiencia, grupos y permisos UPTC siguen pendientes de confirmación institucional.
**Fecha:** 30 de septiembre de 2026.
**Datos personales:** no se incorporan datos de estudiantes ni atributos personales de identidad a la aplicación.

## Contexto y objetivo

Como línea base, el backend validaba tokens OIDC como resource server y fallaba cerrado mientras emisor y audiencia estuvieran vacíos. La API `/api/v1/me` devuelve el identificador del principal y los permisos calculados en el servidor. Esta implementación conecta el flujo federado y la consulta de permisos para que la institución pueda habilitarlo cuando DTIC confirme sus valores.

El objetivo es permitir que una persona se autentique con OIDC y que el servidor conceda únicamente permisos internos explícitamente configurados. Sin issuer, audience, claim y asignaciones aprobadas, el acceso administrativo sigue desactivado.

## Restricciones

- Mantener ambos monolitos, los dos repositorios y el backend resource server sin sesión/cookie.
- Usar Authorization Code con PKCE para cliente público; no habilitar implicit grant, password grant ni `client_secret` en React.
- Guardar estado de navegación OIDC en `sessionStorage`; mantener el usuario/access token solo durante la pestaña, y no habilitar renovación silenciosa ni persistencia en `localStorage`.
- Leer permisos solo desde `GET /api/v1/me`; React no interpreta grupos ni claims para autorizar acciones.
- Backend valida issuer y audience exactos y convierte el claim de autoridades únicamente mediante un mapa explícito configurado por servidor. Mapa vacío significa cero permisos; permisos desconocidos o configuración inválida impiden el arranque.
- El mapa de grupos UPTC, issuer, audience, claim, scopes, URI de retorno, duración de token y despliegue son desconocidos y no se inventan en el repositorio.
- Las variables `VITE_*` son públicas. Ningún secreto del proveedor puede usarse en el frontend.
- No se crean cuentas, tokens ni grupos de demostración.

## Opciones

1. **SPA React con Authorization Code + PKCE (elegida):** corresponde al backend resource server ya existente, evita secretos de cliente y mantiene las aplicaciones separadas. Los tokens se limitan a la pestaña y el backend conserva el control final. El proveedor debe permitir el flujo público y publicar metadata/endpoints compatibles con navegador.
2. **BFF con sesión HttpOnly en Spring:** reduciría exposición de tokens a JavaScript, pero cambiaría el contrato bearer sin estado, requeriría protección CSRF y una topología de proxy/dominio común que aún no está definida.
3. **Identidad local o token de desarrollo:** facilitaría demostraciones pero crearla ahora haría más probable confundir un usuario de prueba con autorización UPTC; se descarta.

## Diseño

### Navegador

- Usar `oidc-client-ts` para Authorization Code + PKCE. Construir el `UserManager` solo cuando `VITE_OIDC_AUTHORITY`, `VITE_OIDC_CLIENT_ID` y el retorno permitido estén completos y sean válidos. El retorno debe estar en el mismo origen.
- Usar `response_type=code`, scope mínimo `openid` más el scope de API configurado, y `sessionStorage` para el estado temporal y la sesión de pestaña. No solicitar email/perfil si el proveedor no lo exige. No habilitar refresh-token ni iframe silent renew en este incremento.
- Procesar el retorno OIDC, restaurar solo hashes internos conocidos (`#inicio`, `#programas`, `#academia`) y limpiar el código/estado de la URL después del intercambio.
- Una sesión autenticada llama `/api/v1/me` con Bearer y recibe `{subject, permissions}`. Los errores de red o formato dejan permisos vacíos; 401 elimina la sesión local; 403 nunca habilita controles.
- Mostrar estados sin configuración, cargando, sin sesión, autenticado, vencido y error. Inicio/cierre de sesión no muestra ni registra tokens o claims.

### Backend

- Mantener el decoder actual con validación por `UPTC_OIDC_ISSUER_URI` y `UPTC_OIDC_AUDIENCE`; si falta cualquiera, no se acepta ningún token.
- Mantener configurable `UPTC_OIDC_AUTHORITIES_CLAIM`. Agregar `UPTC_OIDC_ROLE_PERMISSION_MAPPING` como objeto JSON `valor-exacto-del-claim -> lista-de-permisos-internos`; su valor por defecto es `{}`.
- Aceptar solo permisos presentes en `ApplicationPermission`. Rechazar configuración malformada, rol duplicado, rol vacío, colección vacía o permiso desconocido; no admitir comodines ni inferir nombres de grupos.
- `/api/v1/me` sigue siendo el único contrato para descubrimiento de autorización del cliente y no-cachea identidad ni permisos.
- Las rutas REST existentes siguen aplicando permisos del lado servidor. La ausencia de un control visual nunca concede acceso.

### Integración React

- Proveer un contrato de sesión inyectable para que las pruebas no dependan de un IdP real.
- La navegación muestra iniciar sesión solo si el cliente OIDC está configurado; sin config explica que el acceso institucional está pendiente.
- Pasar el access token y los permisos resueltos por `/api/v1/me` al Centro de Identidad Visual, al catálogo curricular y a la vista académica, manteniendo cada capacidad separada.
- En `/#academia`, `academic:period:read` habilita la consulta administrativa de todos los estados. `academic:period:write` habilita confirmar apertura de periodos `APPROVED` y cierre de periodos `OPEN`; la API conserva la autorización final.
- Cada transición solo cambia el estado del periodo académico. No publica oferta de asignaturas ni habilita inscripción o matrícula; el periodo académico real es distinto al semestre de una malla.
- Si la autorización de lectura se pierde mientras hay una respuesta administrativa cargada, la interfaz debe ocultar esos datos en el mismo render, mientras solicita de nuevo la vista pública.

## Aceptación

1. Sin variables del proveedor no aparece botón de login activo, no se fabrican credenciales y todas las rutas administrativas permanecen denegadas.
2. Con configuración de cliente válida, el flujo de autorización es `code` + PKCE y procesa state/nonce; no se almacena token en `localStorage` ni se expone en consola/URL.
3. Tokens con issuer o audience incorrectos reciben 401; un principal autenticado sin roles mapeados obtiene una lista vacía y las mutaciones reciben 403.
4. Un rol configurado obtiene exactamente los permisos de su lista; no hereda permisos por prefijo o similitud textual.
5. `/api/v1/me` determina los controles de React; una respuesta inválida, un timeout o 403 no presenta permisos administrativos.
6. Sign-out elimina el usuario/token de la pestaña y redirige al destino local validado.
7. `npm test`, build, lint, suite Maven, Compose config y smoke local pasan. La ausencia de IdP real se reporta como no verificable, sin afirmar integración UPTC operativa.
8. Sin `academic:period:read`, la interfaz no conserva datos administrativos previamente cargados; sin `academic:period:write`, no ofrece transiciones. Con escritura, solicita confirmación y muestra la respuesta del servidor.

## Gates institucionales restantes

DTIC debe confirmar issuer/discovery, audiencia, claim de grupos, scopes, URI de retorno por ambiente, lista vigente de grupos y matriz aprobada grupo→permiso. La etapa implementa el mecanismo genérico, no esa asignación. El despliegue productivo también requiere HTTPS, CSP, configuración de proxy, políticas de sesión/token, registro de aplicación y revisión de seguridad.
