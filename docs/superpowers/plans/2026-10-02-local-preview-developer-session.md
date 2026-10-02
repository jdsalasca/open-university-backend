# Plan: sesión de desarrollador para preview local

> Ejecutar con Superpowers TDD, pruebas AAA y el repositorio en `develop`. El patrocinador autorizó el perfil local; no configurar ni publicar OIDC institucional.

**Objetivo:** ofrecer entrada local temporal para probar las consolas existentes, manteniendo el modo institucional sin cambios y excluido de producción.

**Diseño:** consultar `docs/superpowers/specs/2026-10-02-local-preview-developer-session.md`. El backend crea bearers opacos aleatorios de proceso, limitados por tiempo y revocables; el perfil Spring local es su único decoder/emisor. React integra el flujo con el proveedor de identidad existente y consulta `/api/v1/me` como autoridad de permisos. Ningún token queda persistido.

## Tareas

1. **Contrato y pruebas backend RED:** probar emisión, aleatoriedad, límite, vencimiento, bearer inválido/revocado; probar ruta local y permisos completos solo con `local-preview`, ausencia del endpoint fuera del perfil, `no-store`, sin cookies y acceso 401 tras revocar. Ejecutar los casos para observar fallos esperados.
2. **Backend GREEN:** agregar interfaz/servicio de sesión local, decoder y conversor protegidos por `@Profile("local-preview")`, endpoints de crear/revocar y permiso POST/DELETE condicionado en el filtro. Usar issuer/subject sintéticos fijos y no añadir seeds o autoridades en el modelo institucional.
3. **Contrato cliente RED:** probar emisión HTTP local, validación estricta de respuesta, errores, abort y revocación; ampliar pruebas del `IdentityProvider` para OIDC apagado por defecto, login explícito local, `/api/v1/me`, permisos exclusivamente del servidor, expiración, salir y fallo cerrado. Ejecutar para observar RED.
4. **Frontend GREEN:** integrar flujo en memoria dentro del contexto de identidad. Mostrar etiqueta «Desarrollador local · preview» mientras esté activo y una acción de entrada clara solo bajo Vite DEV. No habilitar el modo si OIDC tiene configuración inválida o si se construye producción.
5. **Aislamiento y documentación:** Compose activa `local-preview`; verificar `compose.yaml` mantiene los puertos ligados a loopback. Añadir pruebas al verificador de manifest para excluir tanto el cliente HTTP como el helper de identidad local del build de producción. Actualizar ADR de acceso, runbook, C4, flujo de proceso, ROADMAP y `AGENTS.md`.
6. **Verificación final:** pruebas focalizadas y completas backend/frontend; `npm run build`, `npm run lint`, Maven `verify`, `docker compose config --quiet`, reconstrucción y ejecución Compose sin eliminar volúmenes. Smoke test crear sesión, abrir `/api/v1/me`, mostrar una consola en `/#academia`, revocar, recargar y confirmar vuelta al estado sin sesión; confirmar perfil normal deniega la emisión.
7. **Integrar hito:** commit/push frontend a `develop`, mover submódulo, commit/push backend a `develop`, preservar archivos no rastreados del usuario y reportar límites institucionales.

## Casos de borde

- Token aleatorio no emitido, formato inválido, expiración, revocación repetida, límite de sesiones y reinicio de proceso.
- Emisión simultánea, respuesta malformada, error de red, `/api/v1/me` 401/403, permisos desconocidos y cancelación al salir.
- OIDC configurado conserva PKCE y no usa sesión local como fallback. OIDC mal configurado deja el login deshabilitado.
- El build de producción no contiene el módulo frontend local; sin perfil backend local, no se permite crear ni validar tokens locales.

## Resultado de implementación y verificación

- Frontend `develop`: commit `3136b37` publicado en `origin/develop`. `npm test`: 335 pruebas Vitest y 9 pruebas Node aprobadas; `npm run build` y `npm run lint` aprobados. La ruta `/#programas` quedó en 325.282 B JS/38.834 B CSS; el manifest no incluye el cliente HTTP ni el helper local. El límite JS subió 1.000 B (0,3 %) para esta entrega funcional y queda sujeto a revisión en la etapa de rendimiento.
- Backend: `mvnw verify` aprobó 311 pruebas; 11 contratos opcionales quedaron omitidos por su configuración de integración. `docker compose config --quiet` y `docker compose up --build -d --wait` pasaron; backend, frontend y MySQL quedaron saludables y se conservó el volumen.
- Smoke contra MySQL Compose: emisión con `no-store` y sin cookies, bearer opaco de 43 caracteres, subject `local-preview-developer`, 12 permisos devueltos por `/api/v1/me`, revocación HTTP 204 y rechazo posterior HTTP 401.
- La pestaña de Chrome queda en `http://localhost:5173/#academia`, sin sesión activa. El acceso visual se deja listo; no se pulsa desde el navegador porque esa acción habilita permisos administrativos locales y requiere confirmación justo antes de concederse.
