# Publicación y metadatos de los repositorios coordinados

## Objetivo

Dejar los repositorios del frontend y backend con los nombres públicos aprobados, mantener
`develop` como única rama remota y sincronizar el submódulo usado por Docker Compose.

## Alcance y límites

- Frontend: `jdsalasca/open-university-frontend`, monolito React/Vite/TypeScript/SCSS.
- Backend: `jdsalasca/open-university-backend`, monolito Java 25/Spring Boot.
- Actualizar README, instrucciones, ficha técnica, runbook, roadmap, diagramas existentes y
  URL de `.gitmodules`.
- Publicar primero el cambio de documentación frontend y luego el backend con su gitlink.
- No publicar logs, telemetría, capturas de herramientas, artefactos ni snapshots locales
  divergentes. No modificar ramas distintas de `develop`.
- La visibilidad del código no acredita aprobación institucional, fuente maestra ni permiso
  para procesar datos reales o habilitar trámites.

## Secuencia de integración

1. Verificar el árbol y el historial de ambos remotos; comprobar que solo anuncien `develop`.
2. Revisar las ediciones actuales y retirar de la documentación activa identificadores de
   coordinación y rutas privadas que no aportan al producto.
3. Publicar los cambios del frontend en `develop`; verificar SHA y CI.
4. Actualizar la URL y el gitlink del frontend en el checkout backend; publicar backend en
   `develop` y verificar SHA, CI y resolución del submódulo.
5. Confirmar la visibilidad pública y el acceso anónimo a las dos páginas y referencias Git.

## Verificación

- `git diff --check` para los dos repositorios.
- Gitleaks sobre el historial publicado; revisar y clasificar falsos positivos antes de
  cambiar la visibilidad.
- CI de frontend y backend para los SHA integrados.
- `git ls-remote --heads` para confirmar que ambos remotos contienen únicamente `develop`.
- Verificación pública sin credenciales después del cambio de visibilidad.
