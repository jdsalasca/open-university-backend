# Directorio público de servicios UPTC — plan

- **Fecha:** 2026-10-02 (America/Bogota)
**Baseline:** backend develop `be32fbd`; frontend develop `4f875f6`.

## Objetivo

Hacer útil la ruta `/#estudiantes` como directorio de consulta pública para Bienestar y Biblioteca. La persona puede buscar nombres y descripciones, combinar la búsqueda con categorías y salir a la fuente institucional.

## Alcance y límites

- React/Vite/TypeScript/SCSS únicamente; datos editoriales locales y tipados, sin API ni persistencia.
- Categorías iniciales: Bienestar y Biblioteca. Incluir solo enlaces HTTPS del dominio oficial `uptc.edu.co`.
- Normalizar mayúsculas y diacríticos al filtrar. Anunciar el total accesible y permitir limpiar el estado vacío.
- Enlaces externos identifican que abren el portal UPTC y usan `target="_blank"` con `rel="noopener noreferrer"`.
- No pedir identidad, credenciales ni datos personales; no crear trámites, reservas, pagos ni disponibilidad.
- Las fichas y la documentación declaran fecha de consulta; fechas de actualización institucional solo cuando la página las publica.
- La navegación usa `/#estudiantes`; la etiqueta parte del módulo `students` existente en la configuración visual y mantiene nombre predeterminado claro.

## Fuentes revisadas en UPTC

- [Bienestar Universitario](https://www.uptc.edu.co/sitio/portal/sitios/universidad/rectoria/bie_uni/index.html), resultado indexado del portal consultado el 2026-10-02; informa actualización 2026-10-01.
- [Bienestar Virtual](https://www.uptc.edu.co/sitio/portal/sitios/universidad/rectoria/bie_uni/bieVir.html), resultado indexado del portal consultado el 2026-10-02; informa actualización 2026-09-11.
- [Servicios de Biblioteca Jorge Palacios Preciado](https://www.uptc.edu.co/sitio/portal/sitios/universidad/vic_aca/bibl/7secc/01jpp/serv.html), resultado indexado consultado el 2026-10-02; enumera consulta en sala, préstamos, referencia, formación de usuarios y patrimonio. No se implementan préstamos ni reservas.
- [Sistema de Bibliotecas y búsqueda pública](https://www.uptc.edu.co/sitio/portal/sitios/universidad/vic_aca/bibl/0_busq/index.html), resultado indexado consultado el 2026-10-02; informa actualización 2026-09-08 y búsqueda de libros, artículos, publicaciones y recursos digitales.

El lector de páginas no recuperó directamente las URL de Bienestar/Biblioteca en esta sesión (502/timeouts). La evidencia de contenido y actualización proviene de resultados públicos indexados de UPTC; no se afirma verificación HTTP directa. El detalle y los límites constan en [la revisión de fuentes](../../discovery/uptc-student-services-directory-2026-10.md).

La información del directorio orienta a la persona y no sustituye las condiciones vigentes de cada servicio. La página oficial destino sigue siendo la referencia institucional.

## Feature list

- [x] Ruta pública con navegación por teclado y estado activo.
- [x] Tarjetas tipadas, categorizadas, atribuibles y enlazadas a UPTC.
- [x] Búsqueda tolerante a mayúsculas y tildes en nombre, categoría, descripción y etiquetas.
- [x] Filtros por categoría combinables con texto; contador con `aria-live`.
- [x] Estado sin coincidencias con acción para reiniciar búsqueda y filtros; tras limpiar, el foco vuelve al campo de búsqueda.
- [x] Diseño adaptable, foco visible y mensaje de seguridad sin formularios personales.

## Pruebas AAA

1. RED→GREEN: navegación a `/#estudiantes` desde shell y encabezado de directorio (prueba App).
2. RED→GREEN: la prueba inicial de tarjetas falló al no encontrar fichas; tarjetas tipadas y atribuidas implementadas.
3. RED→GREEN: `PRESTAMO` encuentra “Préstamo”; el primer selector del contador esperaba una cadena contigua y se corrigió para comprobar por separado número y etiqueta accesible.
4. RED→GREEN: el filtro Biblioteca combinado con `PRESTAMO` falla inicialmente por ausencia del botón; filtro implementado con estado `aria-pressed`.
5. RED→GREEN: la búsqueda sin coincidencias falla inicialmente por ausencia de región de estado; vista vacía y restablecimiento implementados.
6. RED→GREEN: el nuevo caso de accesibilidad detectó que al desaparecer el botón de limpieza el foco caía en `body`; el manejador devuelve ahora el foco al campo de búsqueda.
7. Estabilización de verificación: el recorrido de admisiones carga dos módulos perezosos en secuencia y excedió el timeout de 1 s en entorno frío; el preview local mostró las pestañas al completar esa carga. La espera de esa prueba de ruta ahora permite hasta 5 s.
8. Verificación: prueba de rutas HTTPS `uptc.edu.co`, `target=_blank`, `noopener noreferrer` y ausencia de formularios.
9. Verificación: `npm exec -- vitest run --maxWorkers=1` pasa con 58 archivos y 412 pruebas; `npm run lint` termina sin errores; `npm run build` termina con `tsc -b`, manifest y presupuestos verificados; las 13 pruebas Node de presupuestos/tema pasan.
10. Revisión visual: el preview aislado en `127.0.0.1:5174/#estudiantes` muestra las cuatro fichas y se comprobaron categoría, búsqueda sin tildes, estado vacío y foco devuelto al buscador. El checkout y preview previos en el puerto 5173 no se alteraron.
11. Revisión de cambios: `git diff --check` no reporta errores en ambos repositorios. El chunk diferido de `StudentServicesPage` mide 5.98 kB JS y 7.33 kB CSS sin entrar al bundle inicial.

## Riesgos y controles

- **Cambio de páginas o servicios fuente:** fijar el destino oficial, registrar fecha de consulta y no presentar disponibilidad/reglas operativas propias.
- **Interpretar enlaces como integración:** mostrar que la acción abre el portal oficial; no solicitar autenticación dentro de Universiry.
- **Exposición accidental de PII:** únicamente una búsqueda local de texto libre no persistida; ninguna solicitud de red durante el filtrado.
- **Accesibilidad del contador:** usar región `aria-live="polite"`; verificar navegación con teclado, nombre de controles y foco.
- **Concurrencia en App:** reservar rutas y pruebas exactas en Harness antes de editarlas.

## Diagramas

La ruta agrega una pantalla dentro del monolito frontend y enlaces a páginas públicas. No agrega servicios, persistencia ni un flujo institucional. C4 y el proceso registrarán que búsqueda/filtros son locales y que la navegación a UPTC ocurre solo después de hacer clic; el modelo de datos no cambia.

## Integración y preview

1. Ejecutar prueba enfocada (RED/GREEN), `npm test`, `npm run lint` y `npm run build` en `Universiry-frontend`.
2. Revisar `git diff`, vínculos externos y artefactos de producción; ejecutar revisión visual en un puerto local separado del preview existente.
3. Tras verificaciones, integrar/publicar el hito frontend en `develop` según autorización del usuario y confirmar SHA/CI.
4. Actualizar el puntero de submódulo en el checkout backend; como no cambia código backend ni contrato, no crear migración o despliegue productivo. Confirmar el SHA del puntero y mantener preview local.

**Graphify:** Harness reporta `install_required` y no tiene artefactos Graphify (la revisión guardada apunta al snapshot `a5d7681`, no a este branch). No se invoca para este cambio: la ruta conserva los límites existentes del monolito y no cambia el grafo arquitectónico.

**Frontend integrado:** `53abe8b2adee4d7e5535b99f211e8a3ba9b62f0a` está en `origin/develop`. GitHub Actions `Frontend CI` (run `37096173517`) terminó con `success`. El cambio pendiente del backend actualiza el puntero del submódulo y la documentación; no modifica Java, SQL ni contratos.

**Harness:** la tarea, ruta del worktree y claims de archivos están registrados. El proyecto sigue en `planning`: `harness_register_progress` rechaza el `agentId` ya registrado con “Unknown agentId” y `harness_record_evidence` rechaza evidencias `tdd` mientras esa fase no avance. No se creó un brief ni un subagente; por eso esta tarea aún necesita que Harness habilite su transición antes de cerrar su workflow. El CI de backend aún no se ejecuta; se lanzará al publicar el commit con el puntero actualizado.
