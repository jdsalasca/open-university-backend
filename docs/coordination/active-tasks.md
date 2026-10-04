# Tareas activas (espejo)

Reflejo humano del estado en Harness Moon. Ver `README.md` para el protocolo completo.
No duplicar contratos ni criterios de aceptación: eso vive en Harness.

| Dueño | Ámbito / claim | Checkout | Hot-files en uso | Estado | Siguiente acción |
| --- | --- | --- | --- | --- | --- |
| Codex principal (`task_12e6f7ef-ba7b-4aa7-be9a-7dc0002c6573`) | Conciliar cambios locales útiles y seguir entregando la plataforma en los dos `develop` | Worktree integrado de backend y submódulo frontend, ambos sobre `develop` | Documentación del hito y gitlink frontend/backend | El directorio estudiantil y el enlace de solo lectura a la Resolución 111 están integrados en `develop`; frontend `95f6a0404fe6d03c216b252d3c795b6679b71a5a`, CI `37181629590` en `success`, 71 archivos de prueba y 493 pruebas aprobadas; lint/build aprobados. Backend `2108c785ef38683bc476d41d2d80afac89e0ef52` está en `origin/develop` y CI `37181933033` finalizó `success`; ambas ramas remotas exponen solo `develop`. Compose/Watch está activo en `http://localhost:5175`; frontend responde 200, backend health `UP` y MySQL `healthy`. Harness conserva la tarea en `planning/write_plan`: rechazó registrar evidencia `integration` por desorden de fase y el registro del worktree falló al resolver la ruta interna; las verificaciones Git/GitHub se hicieron directamente y este error no detiene el trabajo. El checkout antiguo de backend se conserva en `agent/coordination-espejo`, 82 commits detrás y 1 por delante, con 16 archivos rastreados modificados y 71 no rastreados; el frontend antiguo está 52 commits detrás, con 20 rastreados modificados y 21 no rastreados. No se integran en bloque: currículo, directorio público y portada ya están presentes en `develop`; las demás diferencias siguen preservadas y se revisan individualmente. | Continuar la conciliación por cambio y mantener intactos los checkouts antiguos y sus archivos no revisados. |

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
