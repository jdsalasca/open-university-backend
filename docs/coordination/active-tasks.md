# Tareas activas (espejo)

Reflejo humano del estado en Harness Moon. Ver `README.md` para el protocolo completo.
No duplicar contratos ni criterios de aceptación: eso vive en Harness.

| Dueño | Ámbito / claim | Checkout | Hot-files en uso | Estado | Siguiente acción |
| --- | --- | --- | --- | --- | --- |
| Codex principal (task_12e6f7ef-ba7b-4aa7-be9a-7dc0002c6573) | Conciliar cambios locales útiles y continuar la plataforma en ambos develop | Checkout integrado backend 3482885, frontend 95f6a04; plan en worktree detached | Plan de reconciliación, pruebas del preview curricular | El backend 3482885 está publicado y coincide con origin/develop; CI 37184155245 pasó suite Maven y contratos MySQL. Frontend permanece en 95f6a04 y CI previo está en verde. Ambos remotos exponen solo develop. El preview local responde 200 en frontend 5175 y backend 8080; MySQL sigue healthy. El backend antiguo está 84 commits detrás/1 delante con 87 rutas locales; el frontend antiguo, 52 detrás/0 delante con 41. La comparación curricular, navegación móvil, directorio público, servicios estudiantiles y resolución 111 ya tienen entregas actuales. Los checkouts antiguos siguen intactos porque contienen copias divergentes, un espejo multiagente no aplicable y artefactos locales. | Revisar individualmente una diferencia local restante que no esté ya cubierta en develop y transferir solo una mejora segura. |

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
