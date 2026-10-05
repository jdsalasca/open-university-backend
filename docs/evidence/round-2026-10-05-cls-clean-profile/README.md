# CLS de `/#programas` con perfil limpio — 5 de octubre de 2026

Corrige la medición del [paquete del 4 de octubre](../round-2026-10-04-programs-layout-shift/README.md).
Su tabla.reportaba CLS 0 en tres corridas «con perfil limpio por corrida», pero el script
`measure-cls.cjs` reutilizaba un `--user-data-dir` fijo, de modo que las corridas 2 y 3 heredaban la
caché del bundle de la corrida 1. La afirmación no se sostenía para una primera visita.

## Defecto del método anterior

`measure-cls.cjs` línea 3 fija `--user-data-dir=...prof-fin`. El directorio ya existía en esta máquina
antes de la primera corrida, así que el navegador arrancaba con la caché de disco y HTTP poblada. El
perfil nunca estuvo limpio; el 0 medido correspondía a una navegación con el chunk ya disponible.

## Causa raíz del salto

La ruta es un `import()` diferido. Mientras descarga el chunk, el `Suspense` de `App.tsx` muestra
`<p className="module-loading">`, cuyo `min-height` es `clamp(220px, 54vh, 520px)`. Al montar la
página, el bloque pasa de ~486 px a los **4 508 px** reales del módulo, y el footer se desplaza
miles de píxeles. La reserva `catalog-loading-directory` de 3 404 px solo cubre la carga interna del
directorio, no la descarga del chunk de la ruta.

## Medición con perfil limpio

`measure-cls-clean.cjs` borra su perfil y crea uno nuevo por corrida, y usa un puerto CDP distinto por
corrida. Chromium headless 1440×900 contra el Compose local, 12 s de espera.

| Corrida | Antes (perfil reutilizado, afirmación previa) | Antes (perfil limpio) | Después (perfil limpio) |
| --- | --- | --- | --- |
| 1 | 0 | 0.7503 | 0.0071 |
| 2 | 0 | 0.7496 | 0.0064 |
| 3 | 0 | 0.7496 | 0.0064 |

El salto residual proviene de `catalog-page-content` y mueve 49 px (footer de 4 601 a 4 552), no del
footer completo. El resultado queda dentro del umbral «bueno» de CLS (< 0.1).

## Qué se cambió

`App.tsx` aplica `module-loading-tall` al fallback cuando la vista es la de programas, y `App.scss`
reserva la altura medida del módulo:

```scss
.module-loading-tall { min-height: 4508px; align-content: start; }
```

`align-content: start` deja el aviso arriba; `place-items: center` deja de aplicación porque la fila
toma la altura del propio texto. El límite de CSS de entrada sube de 22 400 a 22 500 B: la reserva
cuesta ~57 B y el arreglo elimina un CLS de 0.75.

### Por qué no se quitó el `import()` diferido

El chunk de `AcademicCatalogPage` mide 40 kB y el entry está en 281 kB. Pasarlo a carga inicial
elevaría el entry por encima del presupuesto de 286 kB, así que la reserva es el arreglo más barato.

## Verificación

- `npx vitest run --maxWorkers=2`: **517 pruebas en 77 archivos, todas aprobadas**. Una corrida
  anterior falló 2 pruebas por saturación del host de esta máquina; la corrida completa registrada en
  `vitest.txt` pasa entera.
- `npm run build`: aprobado, presupuestos verificados (`build.txt`).
- `npm run lint`: 0 avisos, 0 errores (`lint.txt`).
- Guardas de Node: 36 aprobadas.
- Navegador: `programas-fallback.png` muestra el aviso con el espacio reservado y sin hueco roto;
  `programas-resuelto.png` muestra la página final. La captura se tomó ralentizando la CPU 20× para
  poder observar el fallback sin bloquear la red.
- Prueba de regresión `reserves the catalog height while the programs route chunk loads` en
  `App.test.tsx`: falla sin `module-loading-tall` y pasa con él.

## Lo que este arreglo no decide

`4508px` replica la altura real con la ventana de 24 tarjetas. Si cambia la ventana, el número de
programas o el copy del hero, hay que volver a medir; el comentario `ponytail:` en `App.scss` lo
advierte. El orden de la página sigue siendo directorio primero y catálogo curricular después.
