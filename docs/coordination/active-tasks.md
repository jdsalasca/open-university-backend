# Tareas activas (espejo)

Reflejo humano del estado en Harness Moon. Ver `README.md` para el protocolo completo.
No duplicar contratos ni criterios de aceptación: eso vive en Harness.

| Dueño | Ámbito / claim | Checkout | Hot-files en uso | Estado | Siguiente acción |
| --- | --- | --- | --- | --- | --- |
| Codex principal (task_12e6f7ef-ba7b-4aa7-be9a-7dc0002c6573) | Conciliar cambios locales útiles y continuar la plataforma en ambos develop | Worktree detached develop-delivery-20261003, backend 421f7b2, frontend 95f6a04 | Plan de reconciliación, pruebas de preview curricular | Remotos verificados: solo develop en ambos repositorios. Los checkouts activos están limpios; Compose está activo en http://localhost:5175, backend en 8080 y MySQL healthy. El backend antiguo está 84 commits detrás/1 delante con 87 rutas locales; el frontend antiguo está 52 detrás/0 delante con 41. La comparación curricular, navegación móvil, directorio público, servicios estudiantiles y resolución 111 ya tienen entregas actuales en los remotos. La prueba focal del servicio de preview pasó 2/2 con Java 25. La documentación de lectura por código de barras ya es ancestro del backend remoto. Los checkouts antiguos se preservan: contienen copias divergentes, un espejo de coordinación multiagente y artefactos locales; no se integran en lote. | Revisar el diff limitado al plan, dos pruebas AAA y esta conciliación; después publicar fast-forward en backend develop y consultar CI. |

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
