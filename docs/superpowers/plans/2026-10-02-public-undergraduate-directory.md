# Directorio público UPTC de pregrado — Plan de implementación

> Entrega implementada en el monolito frontend, en `develop`. Mantener fuera del maestro académico hasta que UPTC valide fuentes, responsables y datos.

**Objetivo:** hacer consultable en `/#programas` el catálogo público de pregrados que publica la UPTC, conservando las modalidades registradas en la fuente, con procedencia explícita y sin convertirlo en el maestro curricular interno.

**Arquitectura:** instantánea JSON local emitida como asset hash estático; filtros React en memoria y enlace a la publicación/fichas oficiales; ninguna API backend, tabla MySQL o migración.

**Diseño:** [Directorio público UPTC de pregrado](../specs/2026-10-02-public-undergraduate-directory-design.md).

## Implementación y verificación

- [x] Capturar y documentar una instantánea pública, con fecha de fuente y fecha local.
- [x] Escribir pruebas AAA para filtros, búsqueda normalizada, conteos, enlaces, vacíos y aviso semántico de la marca de fuente; observar RED antes del componente.
- [x] Implementar el directorio con facultad, lugar, modalidad, nivel, marca y búsqueda de texto.
- [x] Separar visual y documentalmente el directorio público del maestro interno curricular.
- [x] Mantener el botón de admisiones vinculado al calendario publicado, sin introducir formulario sintético.
- [ ] Ejecutar la suite completa, build/manifiesto, lint y revisión en navegador.
- [ ] Integrar el commit del submódulo frontend y actualizar el puntero/documentación en el `develop` del backend.

La ficha de fuente es [uptc-undergraduate-directory-snapshot-2026-10.md](../../discovery/uptc-undergraduate-directory-snapshot-2026-10.md). Los resultados de verificación y las revisiones de integración se completan tras ejecutar el plan.
