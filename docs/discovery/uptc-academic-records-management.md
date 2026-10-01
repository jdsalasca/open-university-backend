# Expediente académico y gestión documental electrónica UPTC

**Corte de evidencia pública:** 1 de octubre de 2026<br>
**Estado:** hallazgo preliminar; alcance, responsables y sistemas fuente requieren validación institucional.<br>
**Datos personales:** no se copiaron expedientes, identificadores ni soportes.

## Hechos publicados

| Fuente | Hecho | Límite |
|---|---|---|
| [Contacto de ACRA](https://uptc.edu.co/sitio/portal/sitios/universidad/vic_aca/adm_reg/cont.html) | Publica canales en la sede central y direcciones de contacto para Duitama, Chiquinquirá, Sogamoso y Aguazul. | Contactos por sede no demuestran por sí solos unidades administrativas distintas, autoridad sobre los datos ni sistema fuente. |
| [Organigrama de la Facultad Seccional Sogamoso](https://www.uptc.edu.co/sitio/export/sites/default/portal/sitios/universidad/vic_aca/facultades/fac_sog/sog/.content/documentos/organigram_decanatura.pdf) | Incluye “Registro y Control Académico” entre los procesos administrativos de la seccional. | El diagrama no asigna claves de datos, interfaces, custodia técnica ni titularidad de sistemas. |
| [Operaciones/contratos UPTC 2024](https://www.uptc.edu.co/sitio/export/sites/default/portal/sitios/universidad/taip/ntaip/.content/doc/ops/2024_ops.pdf) | Un objeto contractual describe apoyo a recepción/verificación documental, liquidación de matrícula, admisión, matrícula y renovación, y actualización de historiales y expedientes en “SGDA” para posgrado y algunos programas de pregrado en la oficina de Admisiones y Control de Registro Académico de Sogamoso. | Es evidencia de un encargo de apoyo publicado para 2024; no determina la operación actual, la fuente autoritativa ni qué significa exactamente “SGDA” en esa entrada. |
| [Comunicado UPTC sobre el SGDEA](https://www.uptc.edu.co/sitio/portal/cal_not_eve/noticias/det/UPTC-promueve-la-implementacion-del-Sistema-de-Gestion-de-Documentos-Electronicos-de-Archivo/) y [sistemas administrativos](https://www.uptc.edu.co/sitio/portal/sitios/administrativos/) | UPTC denomina **SGDEA** al Sistema de Gestión de Documentos Electrónicos de Archivo. En marzo de 2025 informó que fue implementado en 2019 y que se buscaba reactivar y actualizarlo. La página de sistemas ofrece acceso a SGDEA y un tutorial sobre expedientes generales. El alcance institucional publicado cubre planeación, gestión, trámite, organización, transferencia, disposición, preservación y valoración documental. | La evidencia pública no especifica versión/estado por seccional, integración con SIRA o «Inscríbete», cobertura de historiales estudiantiles, APIs ni matriz de acceso. La sigla “SGDA” del objeto contractual y “SGDEA” de las fuentes institucionales no se asumen equivalentes hasta confirmación. |
| [TRD 2025 de ACRA](https://www.uptc.edu.co/sitio/export/sites/default/portal/sitios/universidad/rectoria/sec_general/arch_corresp/.content/doc/tablas_ret_doc/2025/4130000_depadmcregacad.pdf) y [Cuadro de Clasificación Documental 2025](https://www.uptc.edu.co/sitio/export/sites/default/portal/sitios/universidad/rectoria/sec_general/arch_corresp/.content/doc/clasifdoc2025.pdf) | Identifican como oficina productora al Departamento de Admisiones y Control de Registro Académico (4130000). Clasifican convocatorias, historiales académicos de pregrado/posgrado y algunos registros de SIRA. La TRD indica 2 años en Archivo de Gestión + 8 en Archivo Central para convocatorias y 2 + 78 para historiales de pregrado/posgrado, con cómputos distintos. | Es clasificación y retención archivística de las series descritas. No confirma dueño operativo por entidad, regla de conservación de aspirantes no admitidos, plazo de bases activas, repositorios, copias o respaldos. |

## Lectura provisional

La evidencia encaja con un Departamento ACRA central y funciones/contactos operativos distribuidos por sede o seccional. La participación sugerida por el patrocinador como “ACRA y Registro Académico” podría abarcar ambas capas; las fuentes públicas no permiten decidir el reparto formal. Asimismo, la presencia del SGDEA y la referencia contractual “SGDA” crean una dependencia documental que debe inventariarse junto con SIRA y «Inscríbete». Estas son hipótesis de descubrimiento, no una arquitectura de integración aprobada.

La retención de la TRD regula las series archivísticas descritas. No se traduce directamente en borrado de tablas de MySQL, documentos del sistema de inscripción, adjuntos en almacenamiento de objetos o respaldos. Tampoco se infiere que la documentación del aspirante no admitido pertenezca a la misma subserie del historial de un estudiante.

## Decisiones antes de implementar expediente o soportes

1. Designar por entidad y sede al responsable funcional, dueño de datos, custodio operativo y responsable técnico; documentar quién puede corregir, certificar y consultar cada registro.
2. Confirmar los sistemas y poblaciones actualmente cubiertos por SIRA, «Inscríbete», SGDEA y cualquier solución que la fuente de 2024 denomina “SGDA”; resolver si ambas siglas nombran el mismo producto.
3. Precisar para cada tipo de documento su fuente oficial, finalidad, datos mínimos, clasificación, roles, destinatarios, historial de cambios, transferencias y respuesta a solicitudes del titular.
4. Obtener de Gestión Documental y del Oficial de Protección de Datos la interpretación aplicable de la TRD, los hitos de inicio, disposición final y reglas para aspirantes no admitidos, copias y conservación electrónica.
5. Acordar contratos/interfaces autorizados, metadatos, controles de integridad, antivirus, cifrado, límites, auditoría y eliminación segura. No se presume API ni permiso para duplicar expedientes.
6. Definir alineación con el proyecto UPTC de Fase III y el repositorio documental que el comunicado reporta integrado a «Inscríbete»; evitar captura doble o un repositorio paralelo.

Hasta cerrar estas decisiones, el desarrollo usa datos ficticios y no implementa carga de soportes ni un expediente paralelo. Para la relación con SIRA/Fase III y «Inscríbete», ver [control de solapamiento](uptc-new-academic-system-phase-iii.md), [hallazgo de Inscríbete](uptc-inscribete-2026.md) y [descubrimiento del ciclo estudiantil](student-lifecycle-baseline.md).
