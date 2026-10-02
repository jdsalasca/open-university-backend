# Vista previa de asignación de aulas

**Estado:** herramienta local de desarrollo; no es una asignación académica oficial.
**Fecha:** 2 de octubre de 2026.

## Propósito

Permitir que equipos académicos evalúen el comportamiento de una propuesta de aulas para grupos y horarios sintéticos. El módulo hace visibles tres restricciones comprensibles: disponibilidad del aula, capacidad suficiente y equipamiento requerido. Busca asignar el mayor número de grupos y, entre las soluciones con esa cantidad, reducir los asientos libres.

## Alcance de esta entrega

- Una página React en `/#aulas-demo`, importada únicamente en Vite DEV, presenta escenarios sintéticos, solicita el cálculo y separa grupos asignados de grupos sin aula.
- Un algoritmo Java puro y determinista sirve como núcleo reutilizable dentro del monolito Spring Boot.
- `POST /api/v1/dev/room-allocation/proposals` recibe el escenario en la solicitud, calcula una propuesta y no escribe datos. El controlador existe solo con el perfil Spring `local-preview` y exige autenticación.
- Cada reunión se interpreta como intervalo semiabierto `[inicio, fin)`: dos reuniones de un aula pueden tocarse en el mismo límite sin traslaparse; en el mismo día, un cruce de minutos ocupa el aula para ambas.
- Una sala es candidata si está activa, cubre la capacidad del grupo y contiene todos sus equipos requeridos. La optimización maximiza grupos asignados, luego minimiza la suma de asientos libres; el orden estable de grupos restringidos, holgura y código de aula determina empates.
- El tamaño se acota a 12 grupos, 24 aulas, 8 reuniones por grupo y 100.000 estados de búsqueda. Escenarios inválidos reciben `400`; una búsqueda que supera su presupuesto recibe `422` y no se presenta como solución óptima.

## Límites

No se consulta ni modifica el inventario institucional, oferta, matrícula, salones, equipos, docentes, disponibilidad, calendario, cupos ni horarios. No reserva espacios, no confirma carga académica, no persiste, no genera eventos de auditoría y no se activa en perfiles Spring normales. Los escenarios y resultados se rotulan como ficticios; no codifican prioridades, jornadas, descansos, accesibilidad, costos ni políticas de UPTC.

La propuesta no asigna docentes ni decide qué asignaturas se ofrecen. Su algoritmo es una base para prototipar conflictos y criterios de capacidad; cualquier operación real requerirá fuentes vigentes, reglas, permisos, volumen, responsables y aceptación institucional. El límite de búsqueda y las mediciones locales no certifican un SLA.

## Contratos

La aplicación expone el puerto `RoomAssignmentPlanner.propose(RoomPlanningScenario)`. El transporte valida formatos y límites antes de mapear registros inmutables de dominio. La respuesta conserva un resultado por grupo, con aula/cupo restante cuando se asigna y una razón cerrada cuando no se asigna: sin aulas activas, capacidad insuficiente, equipamiento incompatible o conflicto de horario.

La UI requiere una sesión `local-preview` para ejecutar el POST, no acepta entradas libres en el request y transmite solamente las fixtures del escenario seleccionado. Las fallas de sesión, validación, complejidad y red se muestran sin conservar bearer ni escenario calculado fuera de memoria.

## Criterios de aceptación

1. No hay dos grupos traslapados en la misma aula; las reuniones contiguas sí pueden reutilizarla.
2. No se asigna un aula inactiva, pequeña o sin todo el equipo requerido.
3. Una solución con más grupos asignados vence cualquier solución menor; el segundo criterio minimiza asientos libres y los empates son reproducibles aunque cambie el orden de entrada.
4. Las razones de no asignación distinguen incompatibilidades estáticas de conflicto de horario.
5. Solicitudes vacías, duplicadas, inválidas, demasiado grandes o con intervalos incorrectos se rechazan; el límite de estados falla cerrado.
6. Sin sesión local la página no llama al endpoint. La producción no contiene página, fixtures ni cliente del preview.
7. Compose continúa en loopback y los perfiles normales no exponen la ruta `dev`.
