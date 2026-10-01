# Sistema de inscripción «Inscríbete» reportado como implementado para 2026-II

**Corte de evidencia:** 1 de octubre de 2026
**Fuente principal:** comunicado institucional UPTC n.º 105, publicado el 5 de mayo de 2026
**Estado:** UPTC informó que implementó el sistema para la convocatoria 2026-II; identidad técnica, relación con SIRA/Fase III y continuidad para 2027-I pendientes de confirmación.

## Hechos publicados

En el [comunicado oficial de apertura de inscripciones para pregrado](https://www.uptc.edu.co/sitio/portal/cal_not_eve/noticias/det/Abiertas-inscripciones-para-programas-de-pregrado-en-la-UPTC/), publicado el 5 de mayo de 2026, UPTC indicó que implementó un nuevo sistema de inscripción para el segundo semestre de 2026. La publicación incluye pregrado presencial, programas a distancia/FESAD, virtuales y posgrados; este proyecto mantiene como alcance prioritario el pregrado presencial.

El flujo que describe el comunicado es: adquirir un PIN, entrar al módulo «Inscríbete», elegir programa(s) en primera y segunda opción, diligenciar información personal y académica y grupos poblacionales, validar los datos, registrar el PIN para validación en bases de datos institucionales y cargar documentos para formalizar la inscripción. La noticia dice que el sistema integra un repositorio documental.

Para personas externas, la publicación describe registro con correo personal, creación de una clave y verificación del correo por notificación institucional. Para personas que ya tienen correo institucional UPTC, indica acceso con ese correo sin dominio y sin registrar otro correo. Esta descripción pública no permite inferir si el acceso interno es federado, cómo se vincula a la identidad institucional ni cómo se almacenan o protegen las credenciales.

Las fechas citadas en el comunicado corresponden a 2026-II: venta de PIN presencial hasta el 29 de mayo e inscripción web desde el 1 de junio; para programas a distancia o virtuales, PIN hasta el 6 de junio e inscripción desde el 9 de junio. No deben trasladarse a otras convocatorias.

La [página pública de aspirantes de pregrado](https://reportes.uptc.edu.co/sitio/portal/sitios/universidad/vic_aca/adm_reg/1aspi/pre/), marcada como actualizada el 15 de septiembre de 2026, presenta el calendario 2027-I y enlaza la Resolución 111 de 2026 para pregrado presencial y la Resolución 112 para FESAD. El contenido consultado no identifica por nombre el sistema técnico que recibirá la inscripción 2027-I. La Resolución 111 sí nombra SIRA en una etapa del proceso, pero las fuentes públicas no asignan «Inscríbete» a SIRA ni a la Fase III. Esta es una brecha de identificación documental, no prueba de que la interfaz o integración no exista.

El [comunicado UPTC n.º 240 del 22 de septiembre de 2026](https://uptc.edu.co/sitio/portal/cal_not_eve/noticias/det/UPTC-abre-inscripciones-para-estudiar-un-pregrado-presencial-a-distancia-o-virtual-el-proximo-semestre/) anuncia PIN e inscripción para 2027-I, dirige a los aspirantes al sitio institucional y enlaza la misma página ACRA; tampoco nombra «Inscríbete», SIRA ni la Fase III como el canal de registro. La página separada [«Registre su inscripción Pregrado presencial»](https://uptc.edu.co/sitio/portal/sitios/universidad/vic_aca/adm_reg/1aspi/pre/p2_aspreg_preg.html) declara actualización del 17 de julio de 2024. Sus instrucciones de registro, consulta y modificación con documento y PIN son un antecedente de esa fecha, no evidencia suficiente del flujo técnico vigente en 2027-I.

## Límites de la evidencia

La publicación institucional confirma que UPTC reportó haber implementado este sistema para 2026-II y describe parte de la experiencia de inscripción. No publica artefactos que permitan verificar versiones, disponibilidad efectiva, uso o soporte operativo; tampoco identifica su producto técnico, URL funcional, contratos, custodio de datos, APIs, base de datos, controles de acceso, retención/eliminación de documentos, conexión con el sistema de pagos/PIN ni fuente maestra por entidad.

La [Resolución 111 de 2026 para 2027-I](https://apps3.uptc.edu.co/compilacion-normativa-web/#/compilaciones-normativas/detalle-documento/9906) menciona un proceso de admisión SIRA en su calendario. La fuente sobre «Inscríbete» citada aquí se refiere a 2026-II. No se ha demostrado que ambos nombres describan el mismo producto, que «Inscríbete» sea parte de la Fase III del nuevo sistema académico, ni cuál plataforma ejecutará la inscripción de 2027-I. Tampoco se presume que el canal antiguo publicado en `registro.uptc.edu.co` represente el procedimiento vigente.

La UPTC publica por separado el [Sistema de Gestión de Documentos Electrónicos de Archivo (SGDEA)](https://www.uptc.edu.co/sitio/portal/cal_not_eve/noticias/det/UPTC-promueve-la-implementacion-del-Sistema-de-Gestion-de-Documentos-Electronicos-de-Archivo/). Una fuente contractual de 2024 para Sogamoso menciona historiales y expedientes en “SGDA”; no se ha verificado que esa sigla corresponda al SGDEA ni que cualquiera de ellos sea el repositorio integrado del comunicado sobre la implementación de «Inscríbete». Evitar una segunda carga de documentos hasta identificar el sistema y el contrato de integración autorizado; ver [hallazgo de expedientes y gestión documental](uptc-academic-records-management.md).

## Gate antes de implementar admisiones

Antes de desarrollar formularios, cuentas de aspirante, captura de PIN, carga de documentos, selección o migración de registros, la instancia institucional debe documentar:

1. si «Inscríbete» corresponde a SIRA, a la Fase III, a una solución independiente o a un componente conectado;
2. qué sistema opera cada paso de la convocatoria 2027-I y las siguientes, y cuál es la fuente maestra por entidad;
3. dueño funcional, custodia técnica, autoridad de decisión y responsabilidades entre DTIC, la instancia de Fase III y las áreas que UPTC designe; ACRA/Registro Académico es participación funcional sugerida. Mapear el Departamento central y la eventual operación local por seccional sin inferir dueños ni sistema fuente;
4. interfaces autorizadas para PIN, identidad, validaciones y documentos; verificar el alcance del SGDEA y qué significa “SGDA” en la evidencia de Sogamoso; no se presume que exista una API;
5. finalidades, campos mínimos, tratamiento de aspirantes menores y datos sensibles, permisos, auditoría, retención, eliminación y respuesta a incidentes;
6. estrategia para reutilizar, integrar, coexistir o retirar componentes existentes sin crear doble captura, dos repositorios documentales ni dos registros oficiales.

Hasta cerrar el gate se permite continuar el descubrimiento, documentar calendarios públicos y diseñar modelos técnicos sin datos personales; no se implementa una segunda inscripción ni un repositorio paralelo de documentos. Ver también el [control de solapamiento de Fase III](uptc-new-academic-system-phase-iii.md) y el [descubrimiento del ciclo del estudiante](student-lifecycle-baseline.md).
