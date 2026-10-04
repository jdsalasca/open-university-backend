# Contraste del modo oscuro — ronda del 4 de octubre de 2026

Auditoría de contraste WCAG AA sobre las rutas públicas del frontend con
`data-theme="dark"`, medida en Chromium real a 1440 px y 390 px. El método recorre cada
elemento con texto, resuelve su fondo efectivo (sólido o el primer color del degradado) y
compara el ratio con el mínimo 4.5:1.

## Hallazgos y arreglos

| Ruta | Elementos bajo 4.5:1 | Causa raíz | Arreglo |
| --- | --- | --- | --- |
| `/#admisiones` | 17 (1.02–2.27) | `.admissions-checklist` con `#fff`, `.admissions-official-source` con degradante claro y seis superficies hijas con fondos claros hardcodeados (`#fffdf5`, `#fbf8ed`, `#f4e6a1`, `#f5f4ee`, `#fff8d5`, `#fff7cd`) | Regla `:is()` para las seis superficies y los dos contenedores, más una regla de color para `.admissions-eyebrow` y `.admissions-checklist-footnote` en `_theme.scss` |
| `/#inicio` | 15 (1.11–2.36) | `.editor-actions` con `#fdfdfa`, `.status-banner-icon` con `#f4df8c`, `.button-primary:disabled` con `#c5c6be`, y las etiquetas de la vista previa con `#aaa9a1` sobre la superficie blanca de marca | Regla `:is()` para `.editor-actions` y `.banner-editor-card`, badge oscuro para `.status-banner-icon`, fondo `#383d36` para el botón deshabilitado, y `#6b6c63` en el SCSS del componente para las etiquetas de la vista previa (5.32:1 sobre blanco) |
| `/#espacios` | 1 (1.09) | Icono de búsqueda `⌕` sobre `#fbfbf8` | Ya cubierto por la regla consolidada de espacios; el elemento medido es decorativo y usa `aria-hidden` |
| `/#estudiantes`, `/#resumen` | 0 | — | Sin cambios |
| `/#programas` | 13 reportados | Falso positivo: el auditor toma el primer color del degradante del hero como si fuera el fondo del texto | Ninguno; la captura confirma que el directorio es legible |

Las reglas nuevas siguen el patrón existente: superficies oscuras con tokens
(`--ui-surface-raised`, `--ui-text-primary`, `--ui-text-secondary`, `--ui-border`) en lugar
de colores literales, para que el tema siga siendo la fuente de verdad.

## Coste en el presupuesto de estilos

`entryStyles` sube de 21 000 B a 22 000 B. Son ~1,4 kB de reglas de superficie en modo oscuro,
que es lo que exige el arreglo de accesibilidad. El presupuesto sigue cuadrando con
`npm run build`.

## Evidencia

- `verificacion.txt`: salida real de `npm run build`, las guardas de Node, `npm run lint` y
  `npx vitest run --maxWorkers=8` (75 archivos, 508 pruebas, 35 guardas, 0 lint).
- `admisiones-dark-before.png` / `admisiones-dark-after.png`: el enlace «Consultar Calendario
  oficial de ACRA» pasa de invisible a legible.
- `inicio-dark-before.png` / `inicio-dark-after.png`: la barra de acciones del editor pasa de
  texto claro sobre blanco a superficie oscura legible.

## Lo que esta ronda no cubre

Las rutas `/#avisos`, `/#avisos-admin` y `/#biblioteca` quedaron fuera de esta medición. La
ronda siguiente lo corrige: el perfil `local-preview` concede los 18 permisos, así que esas
tres rutas sí son auditables en local. Ver
[`../round-2026-10-04-authenticated-routes/`](../round-2026-10-04-authenticated-routes/),
donde se miden con 0 elementos bajo 4.5:1.

Siguen pendientes el CLS de `/#programas` y la auditoría de la vista previa del Centro de
Identidad Visual, cuyo texto principal usa los colores que publica el centro y no los del
tema.
