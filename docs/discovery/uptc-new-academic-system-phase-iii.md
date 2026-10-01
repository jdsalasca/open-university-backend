# Iniciativa UPTC del nuevo sistema de gestión académica — control de solapamiento

**Corte de consulta:** 1 de octubre de 2026
**Tipo de evidencia:** informes públicos institucionales UPTC
**Estado:** hallazgo confirmado en documentación pública; relación con este proyecto y situación operativa actual pendientes de validación institucional.

## Hallazgo público

El [informe de rendición de cuentas UPTC de 2025](https://www.uptc.edu.co/sitio/export/sites/default/portal/sitios/universidad/rectoria/planeacion/rdc/.content/doc/2026/aud_pub/infrdc_2025.pdf) identifica el proyecto **“Formular la Fase III del proyecto del nuevo sistema de gestión académico, como alternativa al actual sistema de información SIRA”**. El informe reporta 100 % de cumplimiento para la formulación de esa fase y afirma que se entregaron el alcance funcional y técnico, una arquitectura propuesta y una hoja de ruta de transición. Para la meta “ejecutar el 90 % en las fases de desarrollo del nuevo sistema”, el mismo informe registra un avance de 50 % durante 2025. Ese es el cumplimiento reportado de la actividad; no significa que el sistema estuviera construido al 50 %.

El texto presenta la formulación como insumo para planear etapas posteriores y aprobar la inversión necesaria. Por tanto, estos porcentajes no demuestran por sí solos que la inversión se haya aprobado, que el sistema esté terminado, que un módulo específico esté en producción ni cuál sea su estado después del periodo reportado. Tampoco permiten identificar si el proyecto autorizado por el patrocinador de esta plataforma es el mismo proyecto institucional, una iniciativa complementaria o una línea independiente.

Los boletines de Vicerrectoría Académica de [enero](https://www.uptc.edu.co/sitio/export/sites/default/portal/sitios/universidad/vic_aca/vic_acad/inf/doc/2025/001_bolvicacad_2025.pdf) y [mayo de 2025](https://www.uptc.edu.co/sitio/export/sites/default/portal/sitios/universidad/vic_aca/vic_acad/inf/doc/2025/005_bolvicaca_2025.pdf) describen, en paralelo, actualización de SIRA con migración de datos y funcionalidades para PAE, planes de estudio, prerrequisitos, créditos de libre elección, oferta/programación de cursos, horarios y selección de cursos. Estos boletines y el informe anual tienen alcances y cortes de reporte distintos; juntos muestran una dependencia institucional relevante, pero no un inventario operativo de 2026 ni un contrato de integración.

El [Plan de Acción Institucional 2024](https://www.uptc.edu.co/sitio/export/sites/default/portal/sitios/universidad/taip/06_plan/planes/infavance/05_2024_infavacp.pdf) reportó 75 % de avance validado frente al 100 % programado para formular la Fase III, y 85 % frente al 100 % programado para la meta de ejecutar el 50 % de las fases de desarrollo. La [rendición de cuentas UPTC de 2024](https://www.uptc.edu.co/sitio/export/sites/default/portal/sitios/universidad/rectoria/planeacion/rdc/.content/doc/2025/audpubl/inf_rdc_2024_1.pdf) presentó una ruta de diseño curricular que incluye, entre otros temas, capacidades de ingreso, caracterización socioeconómica, admitidos, riesgo de deserción/retención y diseño/seguimiento curricular. Son metas y temas reportados para 2024, no evidencia de campos aprobados ni de módulos actualmente desplegados.

Los indicadores publicados en 2024 y 2025 utilizan metas y cortes distintos; no se deben comparar como un porcentaje acumulado de terminación del producto. El detalle funcional/técnico y la arquitectura propuesta siguen requiriendo el artefacto de Fase III y el inventario operativo vigente para conocer su alcance implementable.

## Implicación para esta plataforma

La plataforma local mantiene sus módulos académicos como prototipos técnicos sin datos oficiales. Antes de ampliar el catálogo, desarrollar admisiones, expediente estudiantil, oferta, carga de cursos o registro de asignaturas, el patrocinador y las autoridades institucionales deben dejar por escrito si este desarrollo:

1. pertenece al proyecto del nuevo sistema descrito por UPTC;
2. reutiliza o implementa una parte de su alcance aprobado;
3. se integra como capacidad complementaria; o
4. tiene un alcance institucional distinto y una frontera explícita.

La decisión debe identificar el patrocinio y mandato aplicables, los artefactos reutilizables, módulos dentro y fuera de alcance, arquitectura e interfaces aprobadas, sistema maestro por entidad, custodia técnica, criterios de aceptación y transición. La mención pública de caracterización socioeconómica, admisión o riesgo/retención no aprueba la captura de esos datos ni define finalidad, base legal o perfil de acceso. No se debe iniciar una segunda fuente institucional ni migrar, conciliar o sustituir datos de SIRA sin esa decisión.

El patrocinador sugiere **ACRA y Registro Académico** para validar proceso, fuente funcional y campos del ciclo del estudiante. Esta sugerencia no es una designación institucional ni determina cuál de esas áreas gobierna cada dominio. Debe confirmarse junto con Vicerrectoría Académica, DTIC y la instancia que dirige el nuevo sistema; la responsabilidad técnica, el estado de los productos y las reglas de acceso también están por confirmar.

## Preguntas de validación y evidencia de salida

| Pregunta | Evidencia necesaria |
|---|---|
| ¿Este desarrollo corresponde al proyecto del nuevo sistema académico o fue autorizado como iniciativa independiente? | Acta, alcance o comunicación institucional que identifique patrocinio, mandato, relación entre proyectos y autoridad de decisión. |
| ¿Qué se formuló en Fase III y qué componentes de desarrollo se ejecutaron o continúan? | Documento funcional/técnico, arquitectura, hoja de ruta aprobada, estado de inversión y releases/inventario vigente. |
| ¿Qué productos SIRA y del nuevo sistema están activos hoy, en qué población y entorno? | Inventario validado por DTIC y dueños funcionales; versión y responsable operativo. |
| ¿Quién es dueño funcional y custodio técnico de cada entidad y proceso? | Matriz RACI aprobada; ACRA y Registro Académico aparecen como propuesta del patrocinador, pendiente de confirmación. |
| ¿Qué estrategia de plataforma aplica a currículo, admisiones, expediente, oferta, matrícula y carga? | Decisión explícita de reutilizar, integrar, complementar o reemplazar por fases; contratos y fuentes de verdad por dominio. |

Hasta cerrar estas preguntas, se permite continuar el desarrollo local de capacidades no superpuestas y con datos sintéticos. Los siguientes incrementos coincidentes de currículo, admisiones, expediente, oferta, carga y registro permanecen en modo de descubrimiento; las capacidades locales existentes siguen siendo prototipos no habilitados institucionalmente. Ninguna referencia pública autoriza procesar datos personales ni aprueba reglas institucionales.
