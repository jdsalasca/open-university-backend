# Consolidación segura directamente en `develop`

**Fecha:** 3 de octubre de 2026
**Objetivo:** publicar en `origin/develop` los cambios útiles de Universiry y mantener el checkout fuente intacto.

## Decisión de integración

El patrocinador indicó que frontend y backend deben permanecer en `develop` y que no se creen ramas nuevas. La integración usa el checkout `develop` ya existente en `.worktrees/docs-library-barcode-20261003`, sin cambiar la rama del checkout fuente. En backend, el frontend continúa como submódulo y su gitlink debe señalar el SHA publicado de `Universiry-frontend/develop`.

## Inventario observado

- Backend de referencia: `origin/develop` en `ac6e8356b660cbc9a67d2b3e44465be1d79a23fb`.
- Frontend de referencia: `origin/develop` en `ef6c8a53c1e4333c3256c3b2900b867ab8cef835`.
- El checkout fuente está en `agent/coordination-espejo`, 73 commits detrás de `origin/develop`; no se publicará esa rama.
- Las modificaciones locales de catálogo curricular, directorio estudiantil, portada unificada y planes/documentación ya están cubiertas por versiones más recientes en `develop`. Se conservarán esas versiones.
- Los documentos de coordinación del checkout fuente reflejan una distribución anterior de trabajo y se consideran reemplazados por las instrucciones vigentes del repositorio y de Harness Moon.
- Los archivos `.playwright-mcp/`, `output/playwright/`, `output/harness-project-profile-20261002.jpg` y `backend/time,uptime,level,tags*` son capturas o salidas de ejecución; no forman parte del producto.
- Se conserva el checkout fuente y sus archivos locales. Ningún archivo de ese checkout se borra, se reescribe ni se sube desde su rama antigua.

## Ejecución

1. Mantener el trabajo nuevo en el checkout existente de `develop` y comprobar que los cambios se limitan a este hito.
2. Ejecutar RED-GREEN-REFACTOR con pruebas AAA para el incremento funcional y las validaciones de ambos monolitos.
3. Publicar primero el commit de frontend en `Universiry-frontend/develop`; luego publicar backend con el gitlink actualizado al SHA de frontend.
4. Verificar ambas referencias remotas, CI para los dos SHA y el preview local de Compose. No declarar operación institucional ni tratar datos reales.

## Criterios de cierre

- No crear ni publicar ramas distintas de `develop`.
- Preservar implementaciones remotas más nuevas y excluir artefactos efímeros.
- Verificar las pruebas y builds afectados antes de cada commit.
- Confirmar commits, gitlink, push, CI y preview con evidencia.
