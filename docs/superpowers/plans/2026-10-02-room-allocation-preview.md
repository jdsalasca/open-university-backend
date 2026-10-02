# Vista previa de asignación de aulas — Plan de implementación

> Seguir TDD AAA por corte; el usuario autorizó continuar la plataforma en `develop` y que cada hito quede disponible en el preview local.

**Objetivo:** entregar un prototipo ejecutable del planificador de aulas con datos sintéticos, algoritmos probados y límites de producción explícitos.
**Diseño:** [especificación](../specs/2026-10-02-room-allocation-preview-design.md).
**Rama:** `develop`; conservar los archivos no rastreados preexistentes.

## Restricciones globales

- Mantener el backend como monolito Spring Boot/Java 25 y la interfaz en React/Vite/SCSS.
- Escribir una prueba AAA por comportamiento, observar RED, implementar el mínimo y confirmar GREEN antes de avanzar.
- No agregar tablas, fixture institucional, PII, rol, permiso, reserva ni escritura en sistema académico.
- Exponer la ruta solo en perfil `local-preview`, autenticada; perfil normal y build de producción deben excluir el preview.
- Limitar payload y costo del algoritmo; el controlador no debe aceptar una propuesta parcial como óptima si agota el límite.
- Actualizar C4, proceso, roadmap, modelo de datos e instrucciones de agente.

## Tareas

### [x] 1. Núcleo Java de planificación

- Añadir el puerto `RoomAssignmentPlanner` y records de escenario, grupo, reunión, aula y propuesta al paquete `roomplanning`.
- Escribir primero pruebas para asignación, maximización, mínimo de holgura, orden de entrada, equipo, capacidad, disponibilidad, reuniones traslapadas/contiguas, razones de descarte, límites y estados máximos.
- Implementar búsqueda acotada, determinista y sin acceso a infraestructura.
- Expected: suite unitaria nueva RED→GREEN, sin Spring ni MySQL.

### [x] 2. API de preview aislada

- Crear request/response web con validación de forma, mapa explícito a dominio y controller `@Profile("local-preview")`.
- Autorizar solo `POST /api/v1/dev/room-allocation/proposals` autenticado en ese perfil; perfiles normales continúan en `denyAll`.
- Probar 401, escenario válido, errores 400, complejidad 422 y ausencia del controlador fuera del perfil.
- Expected: pruebas MockMvc y aislamiento de perfil aprobados; no se modifica el esquema Flyway.

### [x] 3. Interfaz DEV en React

- Añadir cliente tipado y página con selector de escenarios, carga, error, reintento, resumen y tabla de resultados.
- Mostrar advertencia de escenario ficticio; ejecutar solo con sesión local de desarrollador; nunca guardar el token.
- Escribir pruebas AAA de navegación, ausencia de POST sin sesión, parámetros enviados, resultados, fallas y estados vacíos.
- Añadir guarda Node para excluir `roomplanning/demo/` del manifiesto productivo.
- Expected: pruebas de cliente/componente/ruta/manifiesto aprobadas; los módulos reales y navegación institucional no cambian.

### [x] 4. Documentación, integración y preview

- Actualizar diagramas C4 y de proceso, roadmap, data-model y `AGENTS.md`/`frontend/AGENTS.md`.
- Ejecutar pruebas completas backend/frontend, build, lint y `docker compose config --quiet`.
- Reconstruir/reiniciar los contenedores sin eliminar volúmenes; revisar salud/HTTP y la ruta `/#aulas-demo` en el navegador.
- Integrar el frontend primero, registrar su nuevo gitlink en el coordinador/backend, y publicar ambos `develop` como solicita el usuario.
- Expected: el preview de asignación se ve en DEV, API responde con sesión autenticada, las pruebas quedan verdes y no aparecen cambios ajenos.

## Revisión

Revisar primero límites de búsqueda, determinismo, aislamiento del perfil y exclusión del manifest. No describir este planificador como sistema oficial ni convertir los escenarios en datos UPTC.

## Verificación ejecutada

- Frontend: `npm test` — 48 archivos y 384 pruebas Vitest aprobadas; 12 pruebas Node aprobadas. `npm run lint` y `npm run build` aprobados; el presupuesto confirma que el laboratorio no aparece en el manifest de producción.
- Backend: `./mvnw verify` con Java 25.0.3 — 335 pruebas, cero fallos/errores; 11 contratos opcionales de MySQL no se ejecutaron por requerir el servicio de integración correspondiente.
- Integración: `git diff --check` en ambos repositorios y `docker compose config --quiet` aprobados. Compose reporta MySQL saludable; `/actuator/health` está `UP`, la raíz frontend devuelve HTTP 200 y el endpoint de propuesta sin sesión devuelve HTTP 401.
- Compose Watch ya estaba ejecutándose; se mantuvo el proceso existente para que los cambios sigan reflejándose en el preview.
