# Handoff — micro-cortes de robustez de interfaz

**Agente:** OpenCode (`muse/admissions-*`, `muse/offering-*`)
**Fecha:** 2026-10-02
**Estado:** seis cortes integrados en `develop` de ambos repositorios, sin gates de datos ni de identidad cruzados.

## Qué se entregó

| Corte | Archivo | Comportamiento |
|---|---|---|
| Descarga `.ics` | `frontend/src/features/admissions/AdmissionsCalendarPage.tsx` | Un error al generar el calendario ya no se pierde en silencio; se anuncia con `role="status"` y se limpia al reintentar con éxito. |
| Consola de convocatorias | `frontend/src/features/admissions/AdmissionsCallManagementPanel.tsx` | El estado de error de carga ofrece «Reintentar» en lugar de dejar la consola muerta. |
| Agenda pública | `frontend/src/features/admissions/AdmissionsExperience.tsx` | La persona visitante puede reintentar la consulta pública fallida. |
| Identidad de eventos ICS | `frontend/src/features/admissions/admissionsCalendarIcs.ts` | `UID` incluye el ámbito de la convocatoria: dos convocatorias con la misma clave de hito ya no colisionan al importarse en un calendario. |
| Historial de oferta | `frontend/src/features/academics/AcademicOfferingDraftPanel.tsx` | Un historial fallido se puede reconsultar sin recargar la lista de borradores. |
| Asignaturas del currículo | `frontend/src/features/academics/AcademicOfferingDraftPanel.tsx` | Una consulta de asignaturas fallida se puede reintentar y rehabilita el selector. |

## Verificación observada

Ejecutada en `.worktrees/muse-admissions-info/frontend` con Node 24.18.0 sobre
`origin/develop` y el submódulo en su commit de gitlink:

- `vitest run`: 56 archivos, 406 pruebas, todas en verde.
- `oxlint src/features/academics/ src/features/admissions/`: 0 avisos, 0 errores.
- `npm run build`: `tsc` + Vite + presupuestos de bundle verificados
  (entry 277 689 B JS / 17 234 B CSS; `/#programas` 326 029 B JS / 39 471 B CSS).
- Cada corte siguió RED → GREEN con una prueba AAA nueva; la única expectativa
  preexistente modificada fue el `UID` del `.ics`, por cambio intencional de contrato.

## Commits

Frontend `develop`: `4c6af84`, `711341f`, `695ede8`, `b471d6a`, `fe35f9a`, `e8027b2`.
Backend `develop` (solo gitlink): `9218873`, `12ff03f`, `16d4f76`, `143b8ed`, `d23c9c2`, `c2b1b88`.

## Cobertura que falta

Los reintentos son la mejora de robustez de bajo riesgo que quedaba libre sin
tocar archivos con claim ajeno. Siguen sin acción de reintento:

- La consulta de currículos publicados del panel de oferta reinicia el formulario
  (`setSelectedProgramId('')`) en vez de reintentar la misma consulta.
- El aviso de identidad institucional de respaldo en `src/App.tsx` no reintenta,
  aunque `BrandingProvider` ya expone `refresh`.
- `src/features/spaces/SpaceGuidePage.tsx` ya reintenta correctamente; no se cambió.

## Límites y gates

- Ningún cambio toca `AGENTS.md`, `docs/ROADMAP.md`, `docs/architecture/*`,
  `compose.yaml` ni CI, porque están serializados o reclamados.
- `docs/coordination/active-tasks.md` sigue sin editar: hay un claim activo de otro
  agente sobre `docs/coordination/*`.
- No se crean semillas, datos de Holders, PII, captchas ni reglas de selección.
- Sin commit en `develop` del checkout principal: ese árbol tiene trabajo ajeno
  sin integrar; este agente solo trabajó en su worktree.
