# Experiencia local de consulta académica del estudiante — Plan

> **Para agentes ejecutores:** seguir `superpowers:executing-plans` y `superpowers:test-driven-development`, tarea por tarea, con casos AAA y RED antes de cada comportamiento.

**Objetivo:** entregar una vista semanal estudiantil utilizable en el preview local, con agenda, filtros y detalle sobre datos inventados.

**Arquitectura:** componente React diferido que solo se importa con `import.meta.env.DEV`; no usa backend, tablas, permisos simulados ni estado persistente.

**Especificación:** [diseño de la experiencia](../specs/2026-10-02-student-academic-week-preview-design.md).

## Restricciones

- Leer `AGENTS.md`, `frontend/AGENTS.md` y la sección `Mis asignaturas y horario` de `docs/discovery/sponsor-university-platform-scope-2026-10.md`.
- Mantener la lectura propia de datos académicos detrás de identidad/permiso en la futura versión conectada.
- No precargar datos UPTC, roles, nombres, notas, matrícula, ubicación real, docentes o periodos.
- No agregar API, migración, cliente HTTP, caché, persistencia del navegador o dependencia npm para esta vista.
- Mantener la navegación y el contenido de producción sin cambios funcionales.
- Actualizar C4, proceso, roadmap y guía frontend para distinguir el demo de la consulta real pendiente.

## Tareas

### 1. Contratos de interacción y exclusión de producción

- [x] Pruebas AAA: aviso de datos sintéticos; filtro por día; día vacío; selección y detalle; ausencia de controles de inscripción/notas.
- [x] Prueba de integración del shell: enlace visible solo en desarrollo y ruta `/#estudiante-demo` navegable.
- [x] Prueba Node del manifiesto: falla si aparece `src/features/students/demo/` en producción.
- [x] Ejecutar pruebas nuevas y observar RED antes de implementar.

### 2. Vista “Mi semana · demo”

- [x] Crear fixtures locales con código, curso, día, hora, aula y docente completamente inventados.
- [x] Implementar filtros accesibles, lista semanal, panel de detalle y estado vacío.
- [x] Crear SCSS mobile-first con tokens compartidos, foco visible y tema claro/oscuro.
- [x] Ejecutar pruebas y observar GREEN; refactorizar duplicación y semántica.

### 3. Integración y documentación

- [x] Agregar ruta hash y navegación visible exclusivamente en `import.meta.env.DEV`.
- [x] Agregar la guarda de producción y su prueba Node.
- [x] Actualizar C4, proceso, roadmap y `frontend/AGENTS.md`.
- [x] Ejecutar `npm test`, `npm run build` y `npm run lint`.
- [x] Confirmar Compose activo y verificar `/#estudiante-demo` en el navegador de preview; revisar que el componente no llama APIs ni usa persistencia.
- [x] Integrar el frontend en `develop`, actualizar el submódulo backend y confirmar los estados de los repositorios sin tocar archivos ajenos.

### Verificación observada

- RED→GREEN: la prueba de navegación falló al faltar el enlace/ruta; cuatro pruebas de la vista fallaron antes del componente; el contrato Node falló antes de agregar la guarda del manifest. Después de implementar, las pruebas enfocadas quedaron verdes.
- RED→GREEN de revisión visual: al filtrar martes seguía visible el detalle del lunes; el cambio de día ahora limpia la selección y la regresión tiene cobertura AAA.
- Frontend: `npm test` pasa 43 archivos / 343 pruebas Vitest y 10 comprobaciones Node; `npm run build` y `npm run lint` terminan correctamente. El manifest de producción no contiene `src/features/students/demo/`; entry 277 311 B JS / 16 597 B CSS y ruta `#programas` 325 651 B JS / 38 834 B CSS siguen bajo sus presupuestos.
- Bitácora previamente integrada: la suite frontend completa y `AcademicStructureAuditControllerTest` con Java 25.0.4 de SDKMAN pasaron; autorización, filtro, cursor, página vacía y entradas inválidas siguen cubiertos.
- Compose ya estaba activo con frontend/backend `Up` y MySQL saludable. El navegador local mostró `/#estudiante-demo`, aplicó filtro de día y desplegó el detalle ficticio. El componente solo lee fixtures locales y no contiene cliente HTTP ni almacenamiento de navegador; el shell compartido sigue conservando sus servicios de identidad visual/sesión.
- Integración publicada: `Universiry-frontend/develop` en `6b425ab` (`feat(students): add dev week agenda preview`); el repositorio coordinador registra la referencia del submódulo y esta documentación en su rama `develop`.
