# Coordinación entre agentes

Espejo humano commiteado del protocolo de trabajo multi-agente. Sirve cuando un agente
(por ejemplo Luna) no tiene acceso al MCP Harness Moon. **Harness Moon es la fuente
volátil**; este documento es el contrato estable de convivencia.

## Reglas anti-colisión

1. **Un responsable = un worktree detached desde `origin/develop`.** No se crean ramas de
   funcionalidad ni PRs. La única rama remota es `develop`; los commits verificados se
   publican con fast-forward a esa rama solo después de autorización explícita del usuario.
2. **Claim por carpeta.** Reclama tu ámbito antes de editar: OpenCode reclama `backend/`
   e infraestructura; Luna reclama `frontend/` y `frontend/.../features`. No editar
   archivos fuera del claim activo.
3. **Serializar hot-files.** Estos archivos los toca **un solo agente a la vez**, nunca en
   paralelo: `AGENTS.md`, `docs/ROADMAP.md`, `docs/architecture/c4.md`,
   `docs/architecture/process-flows.md`, `compose.yaml`, `.github/workflows/ci.yml`.
   Si otro los está tocando, espera o pide handoff.
4. **Orden de integración frontend-gitlink-primero.** Se integra primero el `frontend`
   (submódulo/gitlink) y luego se actualiza el gitlink en el repo padre. El orden inverso
   deja el padre apuntando a un commit inexistente.
5. **Gate de revisión local.** Antes del commit, revisa `git status -sb`, `git diff --check`
   y el diff staged; incluye solo archivos del ámbito reclamado y excluye secretos y
   artefactos generados. Después del commit, verifica estado limpio, fast-forward y SHA de
   `origin/develop`.

## Ownership

| Agente | Ámbito | No toca |
| --- | --- | --- |
| Responsable actual | Ámbito | Estado |
| --- | --- | --- |
| Codex | `frontend/` y documentación de `backend/`; sin cambios de dominio backend | Solo esta integración secuencial; el siguiente trabajo se reclama en Harness |

## Sincronización con Harness Moon (sin duplicar estado)

Harness es la fuente volátil de verdad; este espejo solo refleja el protocolo y el reparto.
Reglas de sincronización:

- **Handoff**: al cerrar una sesión, escribe el handoff en Harness y refleja la fila en
  `active-tasks.md` (resumen, no copia de estado).
- **Tasks**: el detalle de tareas/contratos vive en Harness; aquí solo va dueño, ámbito,
  rama y bloqueos. No dupliques criterios de aceptación.
- **Checkpoint**: guarda el checkpoint en Harness; en `active-tasks.md` actualiza estado y
  "siguiente acción" de una línea.
- Si Harness no está disponible para Luna, `active-tasks.md` es la única entrada manual y
  debe reconciliarse al reconectar con Harness (Harness gana en conflicto).
