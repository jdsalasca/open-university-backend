# Instrucciones del repositorio

## Propósito

Construir por etapas una plataforma institucional que unifique y reemplace los sistemas de la UPTC. La primera línea de trabajo cubre la identidad visual administrable, identidad y permisos, información básica del estudiante y fundamentos del catálogo académico (programas, mallas, currículos y asignaturas). El alcance real de los legados se confirma mediante inventario institucional; los documentos públicos son antecedentes, no una fuente completa de requisitos.

## Arquitectura acordada

- Un repositorio con dos monolitos desplegables de forma independiente: `frontend/` usa Vite, React y TypeScript; `backend/` usa Java 25 y Spring Boot.
- Backend monolítico modular organizado por capacidades del negocio. No introducir microservicios, brokers ni duplicación de bases de datos sin una decisión arquitectónica aprobada y evidencia de necesidad.
- MySQL es la base relacional objetivo. Flyway versiona el esquema; Hibernate nunca crea ni actualiza el esquema en producción.
- Los módulos se comunican mediante contratos internos explícitos. Las reglas de negocio viven en el backend; el frontend solo ofrece validación temprana de experiencia.
- SDKMAN es el gestor de Java del proyecto en el equipo principal. El `.sdkmanrc` debe fijar la distribución y versión exactas; no cambiar variables globales de Windows sin verificar que Git Bash, PowerShell, Maven e IDE seleccionan el mismo JDK.

## Método obligatorio

- TDD: escribir una prueba AAA que describa el comportamiento, verla fallar por la razón esperada, implementar el mínimo, verla pasar y refactorizar manteniendo la suite verde.
- Cubrir casos felices, permisos, valores ausentes, inválidos, límites, duplicados, concurrencia relevante y regresiones. Preferir dependencias reales; justificar cada doble de prueba.
- Diseñar interfaces antes de adaptadores. Mantener alta cohesión, bajo acoplamiento y responsabilidades SOLID sin agregar capas vacías.
- Antes de agregar entidad, endpoint, token visual o helper, revisar duplicación y reutilizar el modelo/capacidad existente cuando corresponda.
- Ejecutar las verificaciones indicadas por el plan y reportar únicamente resultados observados.

## Seguridad, datos y operación

- No usar datos personales de estudiantes reales en desarrollo, pruebas, capturas ni fixtures; emplear datos sintéticos.
- Endpoints administrativos requieren autorización del lado del servidor. Nunca confiar en que ocultar una ruta o botón en React protege el recurso.
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
- Mantener `docs/architecture/` (C4, datos y procesos), `docs/ROADMAP.md`, decisiones ADR y planes de `docs/superpowers/plans/` sincronizados con el código.
- Actualizar los diagramas cuando cambie una frontera, integración, fuente oficial de datos o flujo de corte.
- No declarar migrado un dominio sin reconciliación, aceptación del responsable institucional, corte verificable, rollback probado y retiro acordado del legado.
