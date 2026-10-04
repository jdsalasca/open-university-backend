# Ventana de resultados del directorio de posgrado — 4 de octubre de 2026

Cierra el mismo defecto que
[`round-2026-10-04-programs-layout-shift`](round-2026-10-04-programs-layout-shift/README.md)
resolvió para pregrado, aplicado al directorio de posgrado, que era **peor**.

## Medición

`measure-postgraduate.cjs` abre `/#programas`, registra `layout-shift` con
`PerformanceObserver`, cambia al botón «Posgrado» y espera a que el chunk diferido resuelva.
Viewport 1440×900, perfil limpio por corrida.

| Métrica | Antes | Después |
| --- | --- | --- |
| Tarjetas renderizadas | 139 | 24 (ventana inicial) |
| Altura del directorio | 15 734 px | 3 134 px |
| Altura total de la página | 16 968 px | 4 369 px |
| CLS al cambiar a posgrado | 0.6167 | 0.0007 |

El 0.0007 restante es `topbar-meta` / `theme-selector-choice`, el mismo cambio de ancho del
selector de tema que ya se había medido en pregrado; no depende de este directorio.

## Defectos corregidos

### 1. Los 139 programas se renderizaban de golpe

`PublicPostgraduateDirectory.tsx` mapeaba `programs.map(...)` sobre el snapshot completo. Ahora
usa la misma ventana que pregrado: `RESULT_WINDOW = 24` tarjetas y un botón «Ver más (N
restantes)» que amplía en pasos de 24. El total atribuido sigue visible en el encabezado
—«139 programas en el directorio público»— y en el contador «139 resultados de 139», de modo
que la ventana no oculta que hay más. La ventana se reinicia al cambiar cualquier filtro.

Cada tarjeta de posgrado es más alta que las de pregrado: todas las 139 llevan
`locationsSummary`, que ocupa una línea adicional. Por eso el bloque era de 15 734 px y no de
9 600 px como el de pregrado.

### 2. Los estados de carga propios no reservaban espacio

La ronda anterior añadió `catalog-loading-directory` al `Suspense` externo de
`AcademicCatalogPage`, pero cada directorio tiene **su propio** estado de carga cuando aún no
tiene el snapshot: `PublicPostgraduateDirectory.tsx` y `PublicUndergraduateDirectory.tsx`
renderizan `className="catalog-loading"` a secas, con el `min-height: 64px` original.

Eso producía un fotograma de 64 px entre el directorio de pregrado ya montado y el de posgrado
recién cargado. Se mide en la traza de muestreo: la secuencia pasó por
`dirH: 3404 → dirH: 64 → dirH: 3134`. Ese solo fotograma explica los 0.6167 de CLS. Ambos
componentes usan ahora `catalog-loading catalog-loading-directory`.

El `Suspense` por nivel de `PublicProgramDirectories.tsx` también usa la clase con reserva, para
que la reserva exista antes de que el componente pregunte por su snapshot.

## Verificación

- `npx vitest run --maxWorkers=2`: **516 pruebas en 77 archivos, todas aprobadas** (salida en
  `vitest.txt`). Con más workers dos pruebas fallan de forma intermitente por saturación del host;
  la CI remota es el gate autoritativo.
- `npm run build`: aprobado sin cambiar el presupuesto de estilos.
- `npm run lint`: 0 avisos, 0 errores.
- Guardas de Node: 36 aprobadas.
- Navegador: «Ver más» lleva la ventana de 24 a 48 tarjetas; la captura muestra «Mostrando 48 de
  139 programas» y «Ver más (91 restantes)», con el catálogo curricular ya legible debajo en
  modo oscuro.

## Nota sobre la reserva de altura

`.catalog-loading-directory` reserva `min-height: 3404px`, la altura que el directorio de
pregrado ocupa con 24 tarjetas. Posgrado con la misma ventana mide 3 134 px, así que la reserva
sobrespira unos 270 px. Se deja así a propósito: la reserva tiene que ser la del caso más alto
para que ningún cambio de nivel produzca un salto. Si en el futuro cambia el tamaño de las
tarjetas o el valor de `RESULT_WINDOW`, hay que volver a medir con
`round-2026-10-04-programs-layout-shift/measure-cls.cjs`.
