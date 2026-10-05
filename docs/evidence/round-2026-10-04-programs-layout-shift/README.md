# CLS de `/#programas` — cerrado el 4 de octubre de 2026

> **Corrección del 5 de octubre de 2026.** La tabla de este paquete afirma «perfil limpio por
> corrida», pero `measure-cls.cjs` reutiliza un `--user-data-dir` fijo, de modo que las corridas 2 y 3
> heredaban la caché del bundle. Con perfil realmente limpio la carga inicial de la ruta da CLS
> **0.7503**, no 0. La causa era el `Suspense` de la ruta en `App.tsx`, no la reserva
> `catalog-loading-directory` de esta página. La medición y el arreglo están en
> [el paquete del 5 de octubre](../round-2026-10-05-cls-clean-profile/README.md). Las cifras de este
> documento se conservan como se observaron en su momento, no como resultado de una primera visita.

Cierra [`docs/discovery/frontend-layout-shift-finding-2026-10.md`](../../discovery/frontend-layout-shift-finding-2026-10.md),
que estaba abierto desde el 3 de octubre y marcado «requiere decisión de producto». Se resolvió
sin reordenar la página y sin tocar el presupuesto de JavaScript.

## Medición

`measure-cls.cjs` (incluido aquí) registra `layout-shift` con `PerformanceObserver` en Chromium
headless a 1440×900, con perfil limpio por corrida y 12 s de espera para que el chunk diferido del
directorio resuelva. Las tres corridas de referencia, medidas con perfil limpio por corrida:

| Corrida | CLS | Altura del directorio |
| --- | --- | --- |
| Antes del arreglo | 0.8507 | 9 635 px |
| Después, corrida 1 | 0 | 3 360 px |
| Después, corrida 2 | 0 | 3 360 px |
| Después, corrida 3 | 0 | 3 360 px |

El único cambio residual que reporta el navegador es `topbar-meta` /
`theme-selector-choice` con 0.0007 cuando el selector de tema cambia de ancho.

## Qué se cambió

### 1. Ventana de resultados en el directorio público

`PublicUndergraduateDirectory.tsx` renderizaba los 79 programas de golpe. Ahora muestra
`RESULT_WINDOW = 24` tarjetas y un botón «Ver más (N restantes)» que amplía la ventana en pasos de
24. El contador superior sigue informando el total real («79 resultados de 79»), y la ventana se
reinicia al cambiar cualquier filtro, para que la persona vea las mejores coincidencias y no la
cola de una ventana abierta antes del filtro.

Esto reduce el bloque de 9 635 px a 3 360 px —una pantalla y media— y evita pedir un scroll de
9 600 px antes de llegar al catálogo curricular.

### 2. Reserva de espacio para el directorio diferido

El `Suspense` de `AcademicCatalogPage.tsx` mostraba un fallback de 64 px, así que el catálogo
curricular quedaba arriba del viewport durante la carga y saltaba miles de píxeles al resolverse
el chunk. El fallback usa ahora la clase `catalog-loading-directory`, que reserva
`min-height: 3404px` — lo que el directorio ocupa realmente con una ventana de 24 tarjetas. El
salto medido pasó de 62 px a 18 px, y el CLS a 0.

### 3. Contraste del hero del catálogo en modo oscuro

`.catalog-hero` conserva un degradante crema (`rgb(255,254,249)` → `rgb(245,244,238)`) mientras el
tema oscuro pinta su encabezado claro: «Mallas curriculares de pregrado» se renderizaba con ratio
**1.0**, es decir invisible. Ahora el hero usa `--ui-surface-raised` y `--ui-text-primary`, y
`.catalog-intro` usa `--ui-text-secondary`. Se añadió la prueba
`dark theme keeps the curriculum catalog hero readable`.

## Verificación

- `npx vitest run --maxWorkers=2`: **511 pruebas en 76 archivos, todas aprobadas**. La suite con
  más workers falla de forma intermitente por saturación del host en esta máquina; los dos tests
  que fallan en esa condición (`PublicProgramDirectories` y `InstitutionalNoticesAdminPage`) pasan
  aislados con y sin estos cambios, verificado con `git stash`.
- `npm run build`: aprobado. El presupuesto `entryStyles` sube de 22 000 a 22 400 B; el comentario
  en `scripts/check-bundle-budget.mjs` acumula los arreglos de accesibilidad de modo oscuro.
- `npm run lint`: 0 avisos, 0 errores.
- Guardas de Node (presupuesto + tema oscuro): 36 aprobadas.
- Navegador: el botón «Ver más» pasa de 24 a 48 tarjetas; las capturas muestran el directorio y el
  hero ya legibles en modo oscuro.

## Lo que este arreglo no decide

El orden de la página sigue siendo directorio primero y catálogo curricular después, tal como fija
`AcademicCatalogPage.test.tsx` («shows the public UPTC undergraduate directory separately before the
platform curriculum catalog»). La reserva de `min-height` depende de que la ventana siga siendo de
24 tarjetas: si cambia, hay que volver a medir. El texto del hero lo dice explícitamente al usuario
—«El directorio público de programas es independiente de este catálogo y aparece arriba»—, así que
esa decisión de producto queda intacta.
