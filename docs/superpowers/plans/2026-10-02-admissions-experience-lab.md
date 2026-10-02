# Laboratorio local de experiencia de admisiones — Plan de implementación

> **Para agentes ejecutores:** aplicar `superpowers:executing-plans` y `superpowers:test-driven-development` tarea por tarea. Trabajar de manera nativa en el worktree actual; no iniciar subagentes.

**Objetivo:** entregar en `/#admisiones` perspectivas locales de aspirante y operador que compartan casos totalmente sintéticos en memoria, junto al calendario público existente.

**Arquitectura:** módulo React de desarrollo, cargado con importación dinámica condicionada por `import.meta.env.DEV`; React Hook Form valida una ficha sin PII y Zustand comparte una cola efímera entre vistas. No cambia el contrato backend ni añade tablas; el build productivo elimina el laboratorio.

**Stack:** React 19, TypeScript, Vite, SCSS, React Hook Form, Zustand, Vitest, Testing Library.

**Especificación:** [diseño del laboratorio](../specs/2026-10-02-admissions-experience-lab-design.md).

## Restricciones globales

- Seguir `AGENTS.md`, `frontend/AGENTS.md` y el gate de admisiones de `docs/superpowers/specs/2026-09-30-pregrado-admissions-process-discovery.md`.
- Usar AAA y ver RED antes de cada cambio de comportamiento; GREEN y suite relevante antes de integrar.
- No introducir PII, datos oficiales precargados, llamadas de postulación, persistencia web, endpoint, migración Flyway, rol falso, puntaje, elegibilidad o decisión.
- Mantener el calendario público y su consola existente bajo sus permisos actuales.
- Actualizar C4, proceso, roadmap e instrucciones frontend si la implementación modifica un límite.

## Archivos y responsabilidades previstos

- `frontend/src/features/admissions/demo/admissionsDemoStore.ts`: contratos y store efímero con fixtures sintéticos y transiciones permitidas.
- `frontend/src/features/admissions/demo/admissionsDemoStore.test.ts`: límites de dominio del store.
- `frontend/src/features/admissions/demo/AdmissionsWorkflowLab.tsx`: selector de perspectivas y carga local del estado compartido.
- `frontend/src/features/admissions/demo/AdmissionsApplicantDemo.tsx`: formulario RHF sin datos personales.
- `frontend/src/features/admissions/demo/AdmissionsAdminDemo.tsx`: bandeja sintética filtrable, detalle y transiciones demo de revisión/corrección.
- `frontend/src/features/admissions/demo/AdmissionsWorkflowLab.scss`: estilos de tabs, pasos, formularios y bandeja, mobile-first.
- `frontend/src/features/admissions/AdmissionsExperience.tsx`: contenedor de importación DEV y calendario público.
- `frontend/src/features/admissions/AdmissionsExperience.test.tsx`: suite del calendario público y su consola existentes.
- `frontend/src/features/admissions/AdmissionsExperienceDev.test.tsx`: integración de laboratorio DEV con el calendario público.
- `frontend/scripts/check-bundle-budget.mjs` y prueba Node asociada: afirmar que el módulo de demo no entra al build productivo.
- `docs/architecture/c4.md`, `docs/architecture/process-flows.md`, `docs/ROADMAP.md`, `frontend/AGENTS.md`: límites, diagramas y estado de entrega.

## Tareas

### 1. Modelar casos de demostración en memoria

- [x] Escribir pruebas AAA para carga de dos fichas ficticias, alta de nueva ficha, rechazo de opciones repetidas, transición permitida, transición inválida, reinicio y ausencia de persistencia.
- [x] Ejecutar las pruebas del store y observar RED antes de crear el módulo.
- [x] Crear tipos acotados y store con datos ficticios reconocibles, sin nombres o identificadores personales.
- [x] Ejecutar suite enfocada en GREEN; 6 pruebas del store pasan.

### 2. Construir recorrido de aspirante

- [x] Escribir pruebas de componente para opción obligatoria, segunda opción distinta, ambas autoverificaciones obligatorias, errores accesibles, envío válido y ausencia de campos personales.
- [x] Ejecutar pruebas en RED antes de implementar la vista.
- [x] Crear formulario RHF mobile-first conectado al store; el éxito muestra la referencia sintética, sin pedir nombre/documento/contacto/archivos.
- [x] Ejecutar suite de componente en GREEN; 3 pruebas pasan.

### 3. Construir bandeja de operador

- [x] Escribir pruebas de lectura de casos, lista vacía, inicio/cierre de revisión demo, ausencia de botones de admisión/rechazo y resumen sin PII.
- [x] Ejecutar en RED antes de crear la vista.
- [x] Crear bandeja accesible que comparte el store y limite acciones a estados de demostración.
- [x] Ejecutar suite de componente en GREEN; 4 pruebas pasan.

### 4. Integrar el laboratorio solo en desarrollo

- [x] Escribir pruebas de integración para tabs de aspirante, equipo y calendario; navegación por teclado; estado compartido durante el montaje y limpio tras remontar.
- [x] Conservar las pruebas de calendario público/consola y verificar que la pestaña pública muestra la agenda.
- [x] Integrar el laboratorio con importación dinámica bajo `import.meta.env.DEV`; mostrar aviso fijo de demo sin PII.
- [x] Añadir una comprobación de manifest/build que falla si se incluye el chunk/módulo del laboratorio en producción.
- [x] Ejecutar suite enfocada en GREEN; 27 pruebas de admisiones pasan y 5 pruebas del bundle pasan.

### 5. Actualizar diagramas e instrucciones

- [x] Añadir el laboratorio local DEV al C4 y el recorrido aspirante→cola→operador a `process-flows.md`.
- [x] Añadir el estado y los límites demo-only a `ROADMAP.md` y `frontend/AGENTS.md`.
- [x] Mantener el gate institucional: los documentos identifican el prototipo como no oficial y no operativo.

### 6. Validación, integración y preview

- [x] Ejecutar `npm test`, `npm run build` y `npm run lint`.
- [x] Inspeccionar el build/manifest: el calendario continúa en producción y no aparece ninguna entrada del laboratorio; Vite procesó 82 módulos.
- [x] Revisar duplicación, límites de permisos, estados accesibles, navegación por teclado y SCSS adaptable a móvil.
- [x] Crear commits en ambos repos, actualizar el puntero del submódulo e integrar los dos repositorios en `develop` conforme a la autorización existente.
- [x] Actualizar con `docker compose up --build -d --wait` y verificar UI 200, backend `UP`, MySQL sano y agenda pública sin convocatorias administradas ni tráfico de postulación.

## Continuación aprobada: respuesta de ajuste en el recorrido demo

- [x] Extender el contrato de estados para solicitar un ajuste fijo, confirmarlo desde Aspirante · demo, reanudar la revisión y cerrarla.
- [x] Añadir prueba AAA para verificar que un motivo inválido o una transición fuera de secuencia no alteran la ficha.
- [x] Añadir pruebas de bandeja para búsqueda/filtro, detalle, motivo visible y cierre de revisión sin decisión de admisión.
- [x] Implementar la acción de cierre que faltaba en la revisión reanudada y presentar el motivo fijo en el detalle del equipo.
- [x] Adaptar los nuevos controles del inbox/detalle a SCSS y a los breakpoints existentes.
- [x] Actualizar la especificación, diagrama de proceso, estado C4, roadmap e instrucciones frontend.
- [x] Ejecutar suite completa (361 pruebas Vitest y 10 verificaciones Node), build de producción (82 módulos, laboratorio excluido), linter e inspección de rutas y servicios del preview después de este incremento.
