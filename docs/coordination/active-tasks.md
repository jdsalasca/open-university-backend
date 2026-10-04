# Tareas activas (espejo)

Reflejo humano del estado en Harness Moon. Ver `README.md` para el protocolo completo.
No duplicar contratos ni criterios de aceptación: eso vive en Harness.

| Dueño | Ámbito / claim | Checkout | Hot-files en uso | Estado | Siguiente acción |
| --- | --- | --- | --- | --- | --- |
| Codex principal (task_12e6f7ef-ba7b-4aa7-be9a-7dc0002c6573) | Conciliar cambios locales útiles y continuar la plataforma en ambos develop | Checkout integrado backend `5e5d938`, frontend `a520242`; ambos en `develop` | Navegación del shell React; sincronización del gitlink | La navegación ya no muestra botones inactivos «Próximo» ni una sección universitaria vacía; Programas y Estructura y periodos siguen accesibles. Frontend: 71 archivos Vitest/494 pruebas y 26 pruebas Node; build y lint pasan. CI frontend `37191004013` y backend `37193280185` pasan. Ambos SHA locales coinciden con `origin/develop`; los remotos solo exponen `develop`. Preview local: React 200, Spring health UP y MySQL healthy. El checkout histórico se conservó sin modificar: backend en `agent/coordination-espejo`, 95 commits detrás/1 delante; frontend, 52 detrás/0 delante. Las capacidades curriculares, directorio público, servicios estudiantiles, admisiones y portada ya tienen implementaciones vigentes en develop; se excluye el componente local de enlaces a demos sintéticos porque la guía impide exponerlos desde navegación. `.harness-moon`, `.playwright-mcp`, `output`, telemetría y cambios locales divergentes permanecen intactos; no se subieron artefactos generados ni datos fuera de las aplicaciones. Harness guarda evidencia TDD RED/GREEN, pero rechaza registrar el agente sobre develop, por lo que no se detuvo la integración por ese flujo. | Cotejo del checkout histórico completado; elegir el siguiente corte funcional sin inventar reglas ni datos institucionales. |

## Orden de integración

1. Integrar `frontend` (submódulo/gitlink) en el repo frontend.
2. Actualizar el gitlink en el repo padre (backend).
3. Verificar `git status` limpio antes de cada paso.

## Hot-files serializados

`AGENTS.md` · `docs/ROADMAP.md` · `docs/architecture/c4.md` ·
`docs/architecture/process-flows.md` · `compose.yaml` · `.github/workflows/ci.yml`

## Reconciliación con Harness

Al reconectar con el MCP Harness Moon, esta tabla se sincroniza desde Harness (harness gana)
y se anotan aquí solo resumen, dueño, rama y bloqueo.

## Conciliación de checkouts históricos — 2026-10-04

- Se confirmó que backend `develop` (`c6c2937`) y frontend `develop` (`a520242`) están limpios y coinciden con `origin/develop`. Los únicos remotos configurados son `Universiry-backend` y `Universiry-frontend`; sus workflows CI más recientes de `develop` pasan: frontend `37191004013`, backend `37191404355`.
- El checkout integrado sigue visible para la revisión local: `http://localhost:5175/` devuelve HTTP 200, `/actuator/health` devuelve `UP` y MySQL figura `healthy` en Compose.
- El checkout histórico `agent/coordination-espejo` conserva 16 modificaciones backend y 71 archivos sin seguimiento; su frontend anidado está 52 commits detrás y conserva 20 modificaciones rastreadas más archivos nuevos. El contenido de aplicación revisado ya está cubierto por versiones posteriores en `develop`, incluidas comparación curricular (`fb4d9cd` backend; `e10f571` frontend), directorio de servicios y catálogo público. Reemplazar los archivos actuales por esas copias antiguas quitaría cambios y pruebas recientes.
- Dos ramas locales antiguas con commits de comparación curricular tampoco aportan un delta neto seguro: sus árboles, comparados con `origin/develop`, eliminan código/documentación de funcionalidades ya integradas. No se mezclan ni se publican sus snapshots antiguos.
- Permanecen en el checkout histórico, sin borrar ni subir, `.harness-moon/`, `.playwright-mcp/`, `output/`, los archivos locales de telemetría y las copias de código divergentes. No se detectó un cambio funcional faltante que se pueda copiar de forma segura; se conservan intactos hasta clasificar cualquier diferencia nueva.
