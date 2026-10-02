# ADR-0004: Sesión temporal de desarrollador para preview local

- **Estado:** Aceptado para Compose local
- **Fecha:** 2026-10-02
- **Alcance:** acceso de revisión a las consolas ya implementadas en datos exclusivamente locales

## Contexto

El SSO institucional, sus issuer/audience/claims y la provisión inicial continúan pendientes. Eso mantiene cerradas correctamente las consolas, pero impide inspeccionar de forma continua las entregas React/Spring Boot en Compose. El patrocinador autorizó un perfil de desarrollador con permisos amplios exclusivamente local y pidió que no exista en producción.

## Decisión

- Solo el servicio backend de `compose.yaml` activa Spring `local-preview`; los puertos publicados permanecen ligados a `127.0.0.1`.
- El controlador, servicio en memoria, decoder bearer y conversor de permisos locales se registran con `@Profile("local-preview")`. Fuera de ese perfil no se registran estos componentes; con el perfil se usa el decoder local en lugar del decoder OIDC.
- `POST /api/v1/dev/local-preview-session` emite un token opaco de 256 bits, aleatorio, de cuatro horas y con límite de 16 sesiones vivas. La tabla de sesiones solo existe en memoria. `DELETE` revoca la sesión. El proceso no registra el token.
- El decoder acepta únicamente tokens todavía presentes en el almacenamiento efímero y los convierte al issuer/subject sintético fijos. El conversor concede cada permiso allowlisted en `ApplicationPermission`, sin claims del navegador ni cambios al catálogo de roles.
- La consulta `/api/v1/me` conserva la autoridad del servidor para entregar permisos al cliente. La identidad canónica mínima que registra queda en la base Compose local; no se crea perfil funcional ni asignación de acceso.
- React carga el cliente de sesión con una importación dinámica solo en Vite DEV, ofrece entrada únicamente si OIDC está sin configurar, consulta `/api/v1/me`, conserva el token solo en memoria y revoca al salir. La interfaz conserva una banda visible de `Desarrollador local · modo preview`.
- El verificador del manifest de producción bloquea la inclusión del cliente local. El paquete Java conserva las clases perfiladas, pero Spring normal no registra sus componentes ni permite emitir o decodificar estos tokens; no se incluyen secretos ni valores de OIDC en `.env.example`.

## Consecuencias

- Un ciclo de navegador tras recargar debe volver a entrar. Reiniciar backend invalida todos los tokens emitidos previamente.
- La sesión local puede ejecutar las operaciones administrativas que ya existan sobre MySQL de desarrollo. No llenar la base con datos oficiales o estudiantiles; cualquier registro generado manualmente pertenece al preview local.
- Esta vía no simula SSO, MFA, revocación institucional, permisos por ámbito ni aceptación funcional. Nunca usarla en un ambiente institucional o productivo.
