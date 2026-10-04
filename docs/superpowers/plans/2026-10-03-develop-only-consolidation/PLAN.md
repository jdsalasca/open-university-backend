# Plan de reconciliación e integración en `develop`

## Alcance

Reconciliar los cambios sin commit del checkout local de Universiry con los HEAD remotos actuales. Integrar en las ramas `develop` de `Universiry-frontend` y `Universiry-backend` solo cambios útiles ausentes, sin crear ramas ni reemplazar implementaciones más recientes.

## Estado base

- Backend: `origin/develop` `bb844c881f215547071520519aaa719b4c85d824`.
- Frontend: `origin/develop` `e7ad98eb88b67fcf59e2403235a9aef1d4a65f5a`.
- Los remotos publican únicamente `develop`; el gitlink del backend apunta al SHA frontend indicado.
- Los cambios locales de comparación curricular, catálogo de pregrado, portada y directorio base tienen equivalentes más recientes en `develop`.
- Diferencia funcional pendiente elegida: separar el acceso público a apoyo socioeconómico del contenido general de Bienestar Virtual.

## Secuencia

1. Mantener este plan y los riesgos en Harness Moon.
2. Confirmar en fuentes UPTC el enlace, alcance informativo y fecha publicada del apoyo socioeconómico.
3. Añadir primero pruebas AAA en el frontend `develop`; ejecutar el test focal y comprobar el fallo esperado.
4. Modificar lo mínimo en la vista y sus pruebas. Mantener la búsqueda, filtros, atribución y límites de privacidad existentes.
5. Añadir o actualizar la ficha de fuentes en backend y enlazarla desde la hoja de ruta.
6. Ejecutar suite, lint y build frontend; `mvnw verify` y `git diff --check` en backend; revisar preview local.
7. Commitear y hacer push fast-forward primero en frontend `develop`, luego actualizar y publicar el backend con el gitlink frontend.
8. Verificar las referencias remotas y el estado de CI para ambos SHA. No crear ni publicar ramas adicionales.

## Criterios de aceptación

- La tarjeta de Bienestar Virtual no atribuye a esa ficha el catálogo de apoyo socioeconómico.
- Una tarjeta propia enlaza la Línea de Apoyo Socioeconómico oficial y muestra su fecha visible de actualización como referencia de la página.
- No se afirman elegibilidad, cupos, beneficios vigentes ni convocatoria abierta.
- Las verificaciones frontend y backend pasan; el preview sigue identificándose como local.
- Las referencias publicadas en los dos repositorios son `develop` y el gitlink backend coincide con el `develop` frontend.

## Límites

No se integran migraciones de datos, expedientes, matrícula, selección de aspirantes, campos personales, secretos, capturas, logs ni snapshots del navegador. Los worktrees fuente permanecen intactos.

## Estado de ejecución

- Frontend: el commit `bda3a2b61419bb5472b49fc1a5000986044636a4` está publicado en `Universiry-frontend/develop`; `git ls-remote` confirmó ese SHA. Suite Vitest (489 pruebas), 26 verificaciones Node, lint y build pasaron antes del push.
- Frontend CI: run `37173565892` terminó `success` en `develop`.
- Preview local: Compose responde en `http://localhost:5175`; el módulo Vite actualizado responde por HTTP 200. La vista es una previsualización local, no un despliegue institucional.
- Backend: el commit `218c64e698be173b7b2f8c569a4cf4afe3f39863` está publicado en `Universiry-backend/develop`; su gitlink `frontend` apunta a `bda3a2b61419bb5472b49fc1a5000986044636a4`. Maven `verify` pasó en Java 25 con 412 pruebas, 0 fallos y 13 omitidas; 8 contratos MySQL corren en CI.
- Backend CI: run `37173669032` terminó `success` con los contratos MySQL.
- Harness verificó el SHA backend en `origin/develop`; la verificación del SHA frontend desde el root de backend no aplica al repositorio submódulo. Ambos SHA también se confirmaron directamente con `git ls-remote`.
