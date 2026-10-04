# Tareas activas (espejo)

Reflejo humano del estado en Harness Moon. Ver `README.md` para el protocolo completo.
No duplicar contratos ni criterios de aceptación: eso vive en Harness.

| Dueño | Ámbito / claim | Checkout | Hot-files en uso | Estado | Siguiente acción |
| --- | --- | --- | --- | --- | --- |
| Codex principal (`task_12e6f7ef-ba7b-4aa7-be9a-7dc0002c6573`) | Auditar cambios locales y publicar solo cambios vigentes en `develop` | Worktree existente `docs-library-barcode-20261003`, rama `develop`, para ambos repositorios | Solo este espejo de coordinación | La revisión partió de backend `develop` `2be28ec1e7cf5090042bbdd41681bdc1fdb00f90`; su resumen se publicó como `023b4fff34e0ccb1c033136e36a2a380914f3109` y Backend CI `37178642130` terminó en `success`. Frontend remoto `develop` = `bda3a2b61419bb5472b49fc1a5000986044636a4`, con CI `37173565892` en `success`; el gitlink backend apunta a ese SHA. Ambos remotos anuncian únicamente `develop`. La comparación curricular, el directorio público, servicios estudiantiles y la portada ya están en sus versiones vigentes en ambos repositorios. El checkout antiguo sigue preservado con 87 estados pendientes en backend y 41 en frontend; sus ramas están 77 y 50 commits detrás, respectivamente. Su comparación con los `develop` actuales muestra miles de líneas retiradas y versiones anteriores de módulos, además de artefactos Harness/Playwright y registros generados, por lo que no se hizo `git add`, reset ni push de ese snapshot. No se crearon ramas nuevas; el Harness registró la planificación, aunque rechazó registrar la evidencia de push porque la tarea continúa en fase de planificación. | Seguir usando los `develop` existentes; publicar solo cambios revisados y conservar el checkout viejo hasta reconciliar sus archivos uno por uno. |

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
