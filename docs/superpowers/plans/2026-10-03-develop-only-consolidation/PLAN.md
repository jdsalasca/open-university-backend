# Plan de reconciliación e integración en `develop`

## Objetivo

Revisar el trabajo local pendiente de Universiry, incorporar a los dos repositorios `develop` las diferencias vigentes que aporten valor y conservar los cambios que aún requieran revisión. Integrar siempre el frontend primero y apuntar después el gitlink del backend a ese SHA. No publicar ramas adicionales, hacer force push ni reemplazar versiones actuales por copias antiguas.

## Reglas de alcance

- Usar los worktrees activos `develop` para integrar los cambios aceptados.
- No editar, limpiar, resetear ni eliminar los checkouts antiguos mientras sus diferencias no estén conciliadas.
- Excluir secretos, credenciales, `.harness-moon`, `.playwright-mcp`, capturas, resultados locales, trazas y archivos generados.
- No incorporar reglas, datos personales, catálogos oficiales, selección, matrícula ni migraciones sin fuente, contrato y aprobación institucional verificables.
- Cada cambio de código sigue TDD AAA y verificación del repositorio antes de commit y push.

## Estado observado — 4 de octubre de 2026

| Checkout | Estado activo | Diferencia del checkout antiguo |
| --- | --- | --- |
| Backend | `develop` `98950efbd13982d4d5058800ae1c5b1a24107126`, igual a `origin/develop` | `agent/coordination-espejo`: 80 commits detrás y 1 por delante; 16 cambios rastreados y 71 archivos sin seguimiento. |
| Frontend | `develop` `16f3d81350d2bd4a1e2001ad5c95ed39575575b7`, igual a `origin/develop` | checkout `develop`: 51 commits detrás; 20 cambios rastreados y 21 archivos sin seguimiento. |

Cada remoto anuncia solo `refs/heads/develop`. Los checkouts antiguos se mantienen preservados: no son un conjunto apto para commit en bloque, pues mezclan artefactos efímeros, documentación desfasada y copias anteriores de capacidades que ya evolucionaron en `develop`.

## Entrega funcional integrada

- Ampliar `/#estudiantes` de cinco a nueve fichas públicas con enlaces UPTC para UPTC Conecta, SIRA/Campus Virtual, opciones de inscripción de materias y calendario de pregrado. La página sigue siendo informativa: no consulta horarios/notas personales, no procesa inscripciones y no solicita credenciales.
- Hacer accesible como `role="alert"` el error de la carga inicial del directorio público de pregrado.
- Enlazar desde el respaldo público 2027-I el registro oficial de la Resolución 111 de 2026 en la Compilación Normativa UPTC; la referencia no se aplica a convocatorias posteriores ni interpreta el acto.
- Documentar las fuentes, su alcance editorial y los límites de interpretación en `docs/discovery/uptc-student-services-directory-2026-10.md`.

## Secuencia verificada

1. Comparar las implementaciones locales con las versiones vigentes de ambos `develop`; trasladar solo comportamiento útil que falte.
2. Verificar pruebas AAA, lint y build del frontend; revisar el diff y excluir artefactos generados.
3. Integrar el frontend mediante fast-forward en `develop` y confirmar el SHA remoto.
4. Actualizar el gitlink y la documentación del backend; ejecutar su verificación CI y confirmar el SHA remoto.
5. Mantener Compose y Compose Watch en preview local; comprobar HTTP y sincronización de archivos.
6. Registrar los resultados y continuar la reconciliación por archivo, sin borrar los checkouts antiguos.

## Criterios de aceptación

- Ambas referencias `origin/develop` coinciden con sus worktrees activos y el gitlink del backend coincide con `Universiry-frontend/develop`.
- Las rutas nuevas apuntan a páginas institucionales HTTPS y no implican elegibilidad, vigencia, cupos, trámites ni acceso automatizado.
- El enlace a la Resolución 111 se muestra solo cuando el calendario de respaldo lo incluye; un calendario publicado distinto no hereda ese acto.
- Las pruebas del directorio cubren consulta, enlaces y error accesible; la suite, lint y build completos del frontend pasan.
- Backend CI pasa con su workflow normal; ningún dato personal o secreto se incorpora.
- El preview sigue en `http://localhost:5175`, Compose Watch actualiza el contenedor y los archivos temporales de prueba quedan retirados.
- Las únicas ramas remotas son `develop`; no se publica una rama ni se fuerza un ref.

## Resultados observados

- Frontend: commit `16f3d81350d2bd4a1e2001ad5c95ed39575575b7` publicado en `Universiry-frontend/develop`. `npm test`: 71 archivos y 491 pruebas aprobadas; `npm run lint` y `npm run build` terminaron con código 0.
- Frontend CI: [run 37180088841](https://github.com/jdsalasca/Universiry-frontend/actions/runs/37180088841) terminó `success` para ese SHA.
- Backend: commit `98950efbd13982d4d5058800ae1c5b1a24107126` publicado en `Universiry-backend/develop`; su gitlink apunta a `16f3d81350d2bd4a1e2001ad5c95ed39575575b7`.
- Backend CI: [run 37180237877](https://github.com/jdsalasca/Universiry-backend/actions/runs/37180237877) terminó `success` para ese SHA.
- Preview: Compose mantiene frontend, backend y MySQL en ejecución; `http://localhost:5175/` responde HTTP 200, backend reporta salud `UP` y MySQL aparece healthy. Compose Watch sincronizó el módulo actualizado; la sonda temporal se eliminó.
- No cambió el diseño de componentes ni los límites entre servicios; C4 y diagramas de proceso no requieren modificación en este hito.

## Siguiente acción

Revisar individualmente los cambios pendientes del checkout antiguo y portar a `develop` solo aquellos que sigan siendo distintos, seguros, compatibles con los contratos vigentes y verificables. Mantener el resto preservado hasta resolver su procedencia o duplicación.
