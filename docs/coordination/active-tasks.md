# Tareas activas (espejo)

Reflejo humano del estado en Harness Moon. Ver `README.md` para el protocolo completo.
No duplicar contratos ni criterios de aceptación: eso vive en Harness.

| Dueño | Ámbito / claim | Checkout | Hot-files en uso | Estado | Siguiente acción |
| --- | --- | --- | --- | --- | --- |
| Codex principal (task_12e6f7ef-ba7b-4aa7-be9a-7dc0002c6573) | Conciliar cambios locales útiles y continuar la plataforma en ambos develop | Checkout integrado backend `9a0b1d9`, frontend `a520242`; ambos en `develop` | Navegación del shell React; sincronización del gitlink | La navegación ya no muestra botones inactivos «Próximo» ni una sección universitaria vacía; Programas y Estructura y periodos siguen accesibles. Frontend: 71 archivos Vitest/494 pruebas y 26 pruebas Node; build y lint pasan. CI frontend `37191004013` y backend `37191029719` pasan. Ambos SHA locales coinciden con `origin/develop`; los remotos solo exponen `develop`. Preview local: React 200, Spring health UP y MySQL healthy. El checkout histórico se conservó sin modificar: backend en `agent/coordination-espejo`, 92 commits detrás/1 delante; frontend, 52 detrás/0 delante. Las capacidades curriculares, directorio público, servicios estudiantiles, admisiones y portada ya tienen implementaciones vigentes en develop; se excluye el componente local de enlaces a demos sintéticos porque la guía impide exponerlos desde navegación. `.harness-moon`, `.playwright-mcp`, `output`, telemetría y cambios locales divergentes permanecen intactos; no se subieron artefactos generados ni datos fuera de las aplicaciones. Harness guarda evidencia TDD RED/GREEN, pero rechaza registrar el agente sobre develop, por lo que no se detuvo la integración por ese flujo. | Seguir cotejando el checkout histórico para transferir solo mejoras funcionales aún no cubiertas; elegir el próximo corte sin inventar reglas ni datos institucionales. |

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
