# Tareas activas (espejo)

Reflejo humano del estado en Harness Moon. Ver `README.md` para el protocolo completo.
No duplicar contratos ni criterios de aceptación: eso vive en Harness.

| Dueño | Ámbito / claim | Checkout | Hot-files en uso | Estado | Siguiente acción |
| --- | --- | --- | --- | --- | --- |
| Codex principal (`task_12e6f7ef-ba7b-4aa7-be9a-7dc0002c6573`) | Conciliar cambios locales útiles y seguir entregando la plataforma en los dos `develop` | Worktree integrado de backend y submódulo frontend, ambos sobre `develop` | Documentación del hito y gitlink frontend/backend | El directorio estudiantil y el enlace de solo lectura a la Resolución 111 están integrados en `develop`; el frontend `95f6a0404fe6d03c216b252d3c795b6679b71a5a` tiene CI `37181629590` en `success`, 71 archivos de prueba y 493 pruebas aprobadas, lint/build aprobados. Las verificaciones actuales de backend, SHA remoto y Compose quedan registradas en Harness Moon. Compose/Watch está activo en `http://localhost:5175`; frontend responde 200, backend health `UP` y MySQL `healthy`. Los remotos exponen solo `develop`. El checkout antiguo de backend se conserva en `agent/coordination-espejo` con 80 commits detrás, 1 por delante, 16 archivos rastreados modificados y 71 no rastreados; el frontend antiguo está 51 commits detrás, con 20 rastreados modificados y 21 no rastreados. No se integran en bloque por contener artefactos efímeros y versiones anteriores; se revisan cambio por cambio. | Reconciliar individualmente el siguiente cambio local que siga siendo útil y compatible; dejar preservados los archivos no revisados y los artefactos generados fuera de Git. |

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
