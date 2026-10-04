# Tareas activas (espejo)

Reflejo humano del estado en Harness Moon. Ver `README.md` para el protocolo completo.
No duplicar contratos ni criterios de aceptación: eso vive en Harness.

| Dueño | Ámbito / claim | Checkout | Hot-files en uso | Estado | Siguiente acción |
| --- | --- | --- | --- | --- | --- |
| Codex principal (`task_12e6f7ef-ba7b-4aa7-be9a-7dc0002c6573`) | Auditar cambios locales pendientes e integrar solo contenido útil que no esté ya en `develop` | Worktree existente `docs-library-barcode-20261003`, rama `develop`, para ambos repositorios | Solo este espejo de coordinación | Backend `develop` = `00435b20df6d05ab92741df8ea93be0c1537eb0f`; frontend `develop` = `bda3a2b61419bb5472b49fc1a5000986044636a4`. Ambos remotos solo publican `develop`; el gitlink backend coincide con frontend. Los pushes devolvieron `Everything up-to-date`. CI backend `37173838665` y frontend `37173565892` terminaron en `success`. La comparación curricular, el directorio público, servicios estudiantiles y el resumen ya están en `develop` en versiones vigentes; se excluyeron artefactos de ejecución y un componente suelto de demos sintéticas. Se conservó intacto el checkout antiguo con cambios locales, porque está atrasado y reemplazarlo podría revertir la versión actual. Harness no aceptó registrar la integración porque exige una rama distinta de `develop`; no se creó ninguna rama. | Continuar desde `develop`. Antes de limpiar el checkout antiguo, preservar y revisar por separado sus cambios locales; nunca copiarlos encima de las versiones actuales sin una comparación y validación. |

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
