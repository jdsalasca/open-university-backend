# Tareas activas (espejo)

Reflejo humano del estado en Harness Moon. Ver `README.md` para el protocolo completo.
No duplicar contratos ni criterios de aceptación: eso vive en Harness.

| Dueño | Ámbito / claim | Checkout | Hot-files en uso | Estado | Siguiente acción |
| --- | --- | --- | --- | --- | --- |
| Codex | Integración frontend y documentación de integración backend | Worktree detached desde `origin/develop`, `develop-delivery-20261003` | `AGENTS.md`, `docs/PROJECT.md`, `docs/ROADMAP.md`, C4, flujos y gitlink `frontend/` | Frontend publicado en `develop` hasta `ca10b71`; tests/build/lint locales aprobados. Documentación y gitlink backend en preparación; sin rama remota adicional. | Verificar documentos y diff, actualizar gitlink, publicar fast-forward solo en backend `develop` y consultar CI. |

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
