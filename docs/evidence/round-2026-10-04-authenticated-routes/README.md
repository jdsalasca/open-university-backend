# Rutas autenticadas en modo oscuro — ronda del 4 de octubre de 2026

Esta ronda cierra la limitación que la ronda anterior dejó abierta: `/#avisos`,
`/#avisos-admin` y `/#biblioteca` estaban marcadas como «no medibles sin sesión
institucional». Esa premisa era incorrecta.

## Por qué ahora sí se pueden medir

`LocalPreviewAuthoritiesConverter` concede los **18 permisos** de
`ApplicationPermission` al sujeto `local-preview-developer`, y `POST
/api/v1/dev/local-preview-session` emite ese bearer solo bajo el perfil Spring
`local-preview`. El botón «Entrar al preview local» está disponible en Vite DEV. Por eso las
tres rutas se pueden abrir, auditar y recargar sinSeed de usuario: la identidad vive solo en
memoria del proceso local.

Comprobado en la sesión:

```
POST /api/v1/dev/local-preview-session -> 200 {"accessToken":"...","expiresAt":...}
GET  /api/v1/me  -> subject=local-preview-developer, permisos=18
```

Los 18 valores son `academic:catalog:*`, `academic:offerings:*`, `academic:period:*`,
`academic:structure:*`, `admissions:calendar:*`, `branding:*`, `identity:roles:*`,
`library:*` y `notices:*`. Ninguno es un perfil institucional ni una asignación en MySQL.

## Método

`audit-contrast.mjs` (incluido en esta carpeta) automate Chromium por el Chrome DevTools
Protocol sin dependencias externas: inicia sesión con el botón del preview, recorre cada
ruta, resuelve el fondo efectivo de cada elemento con texto y calcula el ratio WCAG. Se
ejecutó a 1440 px con `data-theme="dark"`.

Para que la vista no fuera un estado vacío se publicó un aviso sintético por API
(`sourceReference` `SINTETICA-AUDITORIA-2026-10-04`, audiencia `UNIVERSITY`), de modo que
`/#avisos` y `/#avisos-admin` se midieron con contenido real.

## Resultado

| Ruta | Elementos bajo 4.5:1 | Observaciones |
| --- | --- | --- |
| `/#avisos` | 0 | El aviso publicado muestra título, cuerpo, vigencia y la etiqueta de audiencia «Toda la universidad». |
| `/#avisos-admin` | 0 | La consola muestra el aviso con su referencia y actor, y el formulario de publicación completo. |
| `/#biblioteca` | 0 | Préstamos pendientes, búsqueda de títulos y búsqueda por código de barras. |

También se comprobó el estado de error: al bloquear las rutas de la API con
`Network.setBlockedURLs`, `/#avisos` muestra `.my-notices-error` y `/#avisos-admin` muestra
`.notices-admin-error`, ambos con botón «Reintentar avisos». `/#biblioteca` responde a
`/api/v1/admin/library/open-loans` y `/titles` con 200 y presenta su propio `.library-error`
en `role="alert"`.

## Textos de 8 px: no es un defecto de esta ronda

El barrido también reporta textos de 8 px en el pie de página y en los «kicker» de cada
pantalla. Es un patrón deliberado del proyecto: unas 120 declaraciones `font-size: 8px`
repartidas en doce archivos, siempre sobre mayúsculas con `letter-spacing`. No es un
problema de contraste (el color sí cumple 4.5:1) y cambiar la escala tipográfica del shell
es una decisión de diseño que excede esta ronda.

## Consecuencia

La regla «sin sesión no se puede verificar `notices` ni `library`» queda rebutada. En lo
adelanto, cualquier ruta del frontend es auditable en local con el preview local, y la
matriz de permisos real —que sigue vacía para OIDC institucional— no impide revisar la
interfaz. Lo que sí sigue pendiente es lo que el preview no puede responder: si los
avatares de las audiencias se resuelven contra una fuente institucional, y el CLS
documentado de `/#programas`.
