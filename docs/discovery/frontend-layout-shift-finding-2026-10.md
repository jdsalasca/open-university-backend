# Hallazgo: desplazamiento de diseño (CLS) en `/#programas`

Fecha: 2026-10-03. Estado: **cerrado el 2026-10-04, rectificado el 2026-10-05**. Ver
[la evidencia del cierre](../evidence/round-2026-10-04-programs-layout-shift/README.md) y
[la medición con perfil limpio](../evidence/round-2026-10-05-cls-clean-profile/README.md).

> **Rectificación del 5 de octubre de 2026.** El cierre del 4 de octubre atribuyó el CLS a la reserva
> del directorio dentro de `AcademicCatalogPage` y reportó 0. El script de medición reutilizaba un
> perfil de navegador fijo, así que las corridas 2 y 3 no eran limpias. Con perfil limpio la carga
> inicial daba 0.7503, y la causa era el `Suspense` **de la ruta** en `App.tsx`, cuyo fallback de
> 486 px no cubría los 4 508 px del módulo. Corregido reservando la altura del módulo en el fallback:
> el CLS inicial pasó a 0.0064. Este documento se conserva como análisis original.

Fecha: 2026-10-03. Estado: **cerrado el 2026-10-04**. Ver
[la evidencia del cierre](../evidence/round-2026-10-04-programs-layout-shift/README.md). El CLS
medido pasó de 0.8507 a 0 sin reordenar la página: el directorio público muestra una ventana de 24
tarjetas con botón «Ver más», y el `Suspense` que lo envuelve reserva la altura que el directorio
ocupa. Este documento se conserva como análisis original.

## Qué se observó

Lighthouse (escritorio, servidor de desarrollo Vite) reporta en `/#programas`:

- `cumulative-layout-shift` (CLS) ≈ **1.33** (objetivo «bueno» < 0.1).
- El elemento que más se desplaza es `section.catalog-hero` (puntaje de desplazamiento ≈ 0.83), seguido de `main#programas` (≈ 0.43).

Reproducible en dos corridas con el host en calma (CPU 42 %), por lo que no es un artefacto de carga.

## Causa

En `src/features/academics/AcademicCatalogPage.tsx` el directorio público de pregrado se carga de forma diferida dentro de un `Suspense` **por encima** del `catalog-hero`:

```tsx
<Suspense fallback={<div className="catalog-loading" role="status">Cargando el directorio público de programas…</div>}>
  <PublicUndergraduateDirectory />
</Suspense>
<section className="catalog-hero" aria-labelledby="catalog-title"> … </section>
```

- El `fallback` mide ~64 px (`min-height: 64px` en `.catalog-loading`).
- El directorio renderizado mide ~**9 598 px** (mide la lista completa de programas).
- Al resolverse el chunk diferido, el directorio reemplaza al fallback y empuja el `catalog-hero` (que estaba en el viewport) hacia abajo: de ahí el CLS.

Medición directa en el navegador: `directoryHeight = 9598`, `heroTop = 9697`.

## Por qué no se corrigió unilateralmente

El orden actual (directorio antes del catálogo) es **intencional**: la prueba `AcademicCatalogPage.test.tsx` se llama «shows the public UPTC undergraduate directory separately before the platform curriculum catalog» y afirma ese orden. Cambiarlo contradice una decisión explícita del autor.

Las alternativas evaluadas:

1. **Reordenar** el `catalog-hero` (que contiene el `<h1>`) por encima del directorio. Corrige el CLS y el orden de encabezados (h2 antes de h1), pero cambia el orden visual decidido.
2. **Reservar la altura** del directorio en el fallback. No es viable: reservar ~9 600 px crea un vacío enorme; reservar solo la parte visible no elimina el desplazamiento del hero.
3. **Carga anticipada** del directorio (sin `Suspense`). Elimina el fallback y el CLS, pero suma el chunk del directorio al bundle de entrada, que ya está cerca de su presupuesto (`entryJavaScript: 286 000`, `entryStyles: 21 000`).

## Recomendación

Opción 1 (reordenar) si el `<h1>` debe encabezar la página; opción 3 si el directorio debe seguir primero y el presupuesto de bundle lo permite. La decisión es de producto/diseño, no técnica.

## Evidencia

- Lighthouse JSON: `cumulative-layout-shift` con `layout-shifts` apuntando a `.catalog-hero`.
- Medición en navegador: `directoryHeight = 9598`, `heroTop = 9697`.
- Prueba que fija el orden: `src/features/academics/AcademicCatalogPage.test.tsx` («…directory separately before the platform curriculum catalog»).
