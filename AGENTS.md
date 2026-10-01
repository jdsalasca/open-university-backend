# Instrucciones del repositorio

## Propósito

Construir por etapas una plataforma institucional que unifique y reemplace los sistemas de la UPTC. La primera línea de trabajo cubre la identidad visual administrable, identidad y permisos, información básica del estudiante y fundamentos del catálogo académico (programas, mallas, currículos y asignaturas). El alcance real de los legados se confirma mediante inventario institucional; los documentos públicos son antecedentes, no una fuente completa de requisitos.

## Arquitectura acordada

- Dos repositorios Git privados y coordinados: `Universiry-frontend` contiene el monolito Vite/React/TypeScript y `Universiry-backend` contiene el monolito Java/Spring Boot, Compose, SDKMAN y la documentación de integración. Ambos usan `develop` como rama de integración.
- El checkout de backend incorpora `Universiry-frontend` como submódulo en `frontend/` para que `compose.yaml` levante las dos aplicaciones y MySQL desde una carpeta. Actualizar el submódulo después de integrar cambios del frontend.
- Backend monolítico modular organizado por capacidades del negocio. No introducir microservicios, brokers ni duplicación de bases de datos sin una decisión arquitectónica aprobada y evidencia de necesidad.
- MySQL es la base relacional objetivo. Flyway versiona el esquema; la aplicación nunca crea ni actualiza el esquema en producción.
- Los módulos se comunican mediante contratos internos explícitos. Las reglas de negocio viven en el backend; el frontend solo ofrece validación temprana de experiencia.
- En el dominio académico, facultades/escuelas/unidades y lugares de desarrollo son dimensiones distintas. Afiliar un programa reutiliza su identidad existente; las relaciones y su vigencia no se duplican en campos de texto.
- Una fila curricular usa el número de semestre para ubicar una asignatura dentro del plan. Un periodo regular o intersemestral es otra entidad fechada y auditable; nunca inferir calendario, cupos ni apertura desde el año o el plan curricular.
- La estructura organizacional, lugares y programas tiene prioridad explícita de visualización; una afiliación vigente, no los textos legados, define facultad/escuela/sede de un programa. Las actividades del calendario pueden abrir antes de las fechas lectivas y se validan por su propio intervalo.
- La API de estructura permite altas iniciales, cambios auditados de prioridad y acortar la vigencia de relaciones de unidades, sedes y afiliaciones de programa con permisos por operación y controles de concurrencia. `academic:structure:read` abre el snapshot administrativo completo, incluidas vigencias futuras e históricas; sin ese permiso, el árbol público solo muestra relaciones vigentes. Las reasignaciones compuestas siguen pendientes antes de cargar o mantener el maestro institucional; no usar SQL manual para saltarse el dominio.
- Los controles de edición de `/#academia` requieren `academic:structure:read` y `academic:structure:write` de `/api/v1/me`, referencia institucional y auditoría transaccional (`UNIT_CREATED`, `SITE_CREATED`, `UNIT_RELATED`, `SITE_RELATED`, `UNIT_RELATION_CLOSED`, `SITE_RELATION_CLOSED`, `PROGRAM_AFFILIATED`, `PROGRAM_AFFILIATION_CLOSED`). La alta de una unidad hija crea la unidad y su primera relación en una transacción, con ambos eventos (`UNIT_CREATED` y `UNIT_RELATED`) y una sola referencia. Los `PATCH` de cierre identifican el vínculo por sus identidades estables e inicio, solo acortan `valid_through` y registran actor/referencia en la misma transacción; la fecha final es inclusiva, la misma fecha es idempotente y un finito no se extiende. El árbol principal usa la estructura efectiva pública; la consola administrativa conserva la línea temporal completa sin presentar vínculos históricos como vigentes. Tras cada éxito/conflicto se releen ambas vistas y no se repite la escritura.
- La participación funcional sugerida por el patrocinador (“ACRA y Registro Académico”) no designa dueños, campos o fuentes. La UPTC publica un Departamento ACRA central (4130000), contactos por seccional y “Registro y Control Académico” en el organigrama de Sogamoso; tratar una capa local como hipótesis hasta confirmar RACI y fuente por entidad. Leer `docs/discovery/uptc-academic-records-management.md` antes de diseñar expedientes/documentos: SGDEA está publicado; un contrato de 2024 usa “SGDA”, sin confirmar que sea el mismo producto. Los plazos TRD de 2+8 y 2+78 años son archivísticos, no reglas automáticas de borrado de bases, repositorios o respaldos. No precargar facultades, programas, periodos ni datos oficiales.
- No crear datos seed de estructura ni periodos oficiales sin un maestro y una referencia validados por el dueño institucional. Intersemestral es un tipo operativo del modelo actual; confirmar reglas específicas antes de publicar cursos o automatizar límites de matrícula.
- SDKMAN es el gestor de Java del proyecto en el equipo principal. El `.sdkmanrc` debe fijar la distribución y versión exactas; no cambiar variables globales de Windows sin verificar que Git Bash, PowerShell, Maven e IDE seleccionan el mismo JDK.
- El desarrollo local coordinado usa Docker Compose con MySQL aislado, proxy API y Compose Watch; las aplicaciones siguen siendo monolitos independientes.

## Método obligatorio

- TDD: escribir una prueba AAA que describa el comportamiento, verla fallar por la razón esperada, implementar el mínimo, verla pasar y refactorizar manteniendo la suite verde.
- Cubrir casos felices, permisos, valores ausentes, inválidos, límites, duplicados, concurrencia relevante y regresiones. Preferir dependencias reales; justificar cada doble de prueba.
- Diseñar interfaces antes de adaptadores. Mantener alta cohesión, bajo acoplamiento y responsabilidades SOLID sin agregar capas vacías.
- Antes de agregar entidad, endpoint, token visual o helper, revisar duplicación y reutilizar el modelo/capacidad existente cuando corresponda.
- Ejecutar las verificaciones indicadas por el plan y reportar únicamente resultados observados.

## Seguridad, datos y operación

- No usar datos personales de estudiantes reales en desarrollo, pruebas, capturas ni fixtures; emplear datos sintéticos.
- Endpoints administrativos requieren autorización del lado del servidor. Nunca confiar en que ocultar una ruta o botón en React protege el recurso.
- El acceso federado comienza cerrado: no inventar grupos, issuer, audience, scopes ni callbacks institucionales. React no decodifica claims para asignar permisos; consulta `/api/v1/me`. El estado transaccional OIDC de retorno vive en `sessionStorage`; el usuario OIDC y sus tokens permanecen solo en memoria y se requiere un nuevo inicio de sesión tras recargar la página. No guardar tokens en Web Storage, habilitar refresh token ni imprimir token/claims. Antes de manejar expedientes reales, aprobar la arquitectura de sesión institucional y desplegar CSP y demás headers en el punto de entrada productivo.
- Registrar actor, fecha y cambio para modificaciones administrativas sensibles; proteger el historial de auditoría contra edición ordinaria.
- Validar tipo real, tamaño, dimensiones y contenido de imágenes subidas; generar nombres de almacenamiento propios y prevenir traversal, SVG ejecutable y archivos huérfanos.
- Guardar secretos fuera del repositorio. Configurar producción con TLS, cuentas de mínimo privilegio, respaldo y restauración probados.
- El objetivo de latencia de consultas críticas MySQL es promedio menor a 50 ms en una carga y volumen definidos. Reportar también percentiles; no anunciar el objetivo como alcanzado sin medición representativa.

## Identidad institucional e interfaz

- Mantener logos, paleta, nombres visibles de módulos y banners configurables desde el Centro de Identidad Visual, con roles, vista previa, validación y auditoría.
- Los valores iniciales deben proceder de activos o manuales oficiales vigentes de la UPTC, con su procedencia documentada. La configuración no puede eliminar contraste legible, texto alternativo ni estados de foco.
- Toda pantalla nueva debe ser adaptable, accesible por teclado, tener estados de carga/error/vacío y usar la configuración visual compartida.

## Documentación y continuidad

- Leer esta guía y `docs/PROJECT.md` antes de cambiar el diseño.
- En cambios del frontend, leer también `frontend/AGENTS.md` y ejecutar validaciones desde el submódulo frontend.
- Mantener `docs/architecture/` (C4, datos y procesos), `docs/ROADMAP.md`, decisiones ADR y planes de `docs/superpowers/plans/` sincronizados con el código.
- Actualizar los diagramas cuando cambie una frontera, integración, fuente oficial de datos o flujo de corte.
- Para cambios académicos, sincronizar explícitamente los diagramas C4, el modelo de datos, el proceso y `docs/ROADMAP.md`; mantener los gates institucionales visibles hasta que haya evidencia de aceptación.
- Antes de ampliar catálogo, admisiones, expediente, oferta, carga o registro académico, revisar `docs/discovery/uptc-new-academic-system-phase-iii.md` y `docs/discovery/uptc-inscribete-2026.md`. Exigir una decisión institucional sobre relación, alcance, fuente maestra e interfaces tanto para la Fase III alternativa a SIRA como para «Inscríbete», reportado por UPTC como implementado para 2026-II; no inferir que sean el mismo sistema ni crear una segunda inscripción o repositorio documental.
- Antes de implementar expediente, carga de archivos o retención estudiantil, revisar también `docs/discovery/uptc-academic-records-management.md`; identificar alcance del SGDEA, funciones locales por seccional, el significado de “SGDA” en la fuente de Sogamoso y la interpretación de la TRD antes de duplicar documentos o automatizar conservación/eliminación.
- No declarar migrado un dominio sin reconciliación, aceptación del responsable institucional, corte verificable, rollback probado y retiro acordado del legado.
