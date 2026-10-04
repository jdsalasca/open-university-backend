# Consolidación segura directamente en `develop`

**Fecha de actualización:** 3 de octubre de 2026

**Objetivo:** reconciliar el trabajo local con las versiones actuales de frontend y backend, integrar solo cambios útiles que falten y publicar exclusivamente en `origin/develop`.

## Decisión de integración

El patrocinador confirmó que ambos monolitos deben integrarse en `develop`, que no se creen ramas nuevas y que los cambios aprobados se publiquen en los dos remotos. El trabajo se mantiene en el worktree backend `develop` ya existente (`.worktrees/docs-library-barcode-20261003`) y en su submódulo frontend, también en `develop`. No se hará push forzado ni se publicarán ramas de funcionalidad.

## Inventario actualizado

- Backend `origin/develop`: `bb844c881f215547071520519aaa719b4c85d824`.
- Frontend `origin/develop`: `e7ad98eb88b67fcf59e2403235a9aef1d4a65f5a`.
- En ambos remotos, `git ls-remote --heads origin` muestra únicamente `refs/heads/develop`.
- El submódulo frontend del `develop` backend ya apunta a `e7ad98eb88b67fcf59e2403235a9aef1d4a65f5a`.
- El checkout fuente `agent/coordination-espejo` y otros worktrees conservan modificaciones locales. No se cambiarán ni limpiarán esos checkouts: contienen archivos locales de producto obsoletos y salidas de ejecución que requieren conservarse.
- La comparación curricular, el directorio de pregrado, la portada unificada y el directorio estudiantil base ya están en el `develop` remoto. Se conservarán sus versiones actuales, que son más nuevas que varios archivos locales.
- La diferencia funcional útil que sí falta en el `develop` frontend es separar la tarjeta de apoyo socioeconómico de la tarjeta de Bienestar Virtual. El contenido se respaldará con fuentes UPTC y no afirmará disponibilidad ni elegibilidad.
- Se excluyen `.harness-moon/`, `.playwright-mcp/`, `output/` y `backend/time,uptime,level,tags*`: son contratos/estados de ejecución locales, capturas y trazas, no código de producto. Se preservan sin borrarlos.

## Plan de integración

1. Probar primero el comportamiento de la tarjeta socioeconómica en el frontend actual y observar el fallo esperado.
2. Implementar el cambio mínimo en el catálogo vigente y ejecutar pruebas focalizadas, suite completa, lint y build.
3. Registrar la procedencia del texto en `docs/discovery/uptc-student-services-directory-2026-10.md` y actualizar el plan de la capacidad.
4. Publicar primero frontend `develop`; actualizar después el gitlink backend al SHA remoto del frontend y publicar backend `develop`.
5. Verificar los SHA remotos, las CI correspondientes, `git diff --check` y la vista local de Compose. No presentar la vista local como producción institucional.

## Riesgos y controles

- Las páginas UPTC pueden cambiar. El directorio cita la URL y la fecha visible de la fuente; el usuario debe continuar en la página institucional para confirmar la vigencia.
- El contenido socioeconómico no determina requisitos, beneficios, cupos ni convocatorias disponibles.
- Los cambios de checkouts antiguos pueden ser versiones previas, duplicadas o incompletas. Se contrasta cada archivo contra el `develop` vigente antes de integrar.
- Las capturas, logs y archivos de estado locales pueden contener datos de ejecución; se preservan y no se publican.

## Criterios de aceptación

- Bienestar Virtual y apoyo socioeconómico aparecen como fichas separadas con enlaces HTTPS oficiales.
- Las pruebas cubren enlace, atribución y fecha de actualización sin recolectar datos personales ni inferir disponibilidad.
- Se conserva la implementación más reciente del resto de los módulos.
- Cada remoto modificado mantiene `develop` como única rama publicada y el gitlink backend señala el SHA remoto frontend.
- Pruebas, lint, build, CI y preview quedan verificados antes de afirmar el resultado.
