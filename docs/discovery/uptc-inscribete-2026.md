# Sistema de inscripción «Inscríbete» anunciado por UPTC para 2026-II

**Corte de evidencia:** 1 de octubre de 2026
**Fuente principal:** comunicado institucional UPTC n.º 105, publicado el 5 de mayo de 2026
**Estado:** sistema de inscripción anunciado para la convocatoria 2026-II; relación con SIRA, la Fase III del nuevo sistema académico y la convocatoria 2027-I pendiente de confirmación.

## Hechos publicados

En el [comunicado oficial de apertura de inscripciones para pregrado](https://www.uptc.edu.co/sitio/portal/cal_not_eve/noticias/det/Abiertas-inscripciones-para-programas-de-pregrado-en-la-UPTC/), UPTC indicó que implementó un nuevo sistema de inscripción para el segundo semestre de 2026. La publicación incluye pregrado presencial, programas a distancia/FESAD, virtuales y posgrados; este proyecto mantiene como alcance prioritario el pregrado presencial.

El flujo que describe el comunicado es: adquirir un PIN, entrar al módulo «Inscríbete», elegir programa(s) en primera y segunda opción, diligenciar información personal y académica y grupos poblacionales, validar los datos, registrar el PIN para validación en bases de datos institucionales y cargar documentos para formalizar la inscripción. La noticia dice que el sistema integra un repositorio documental.

Para personas externas, la publicación describe registro con correo personal, creación de una clave y verificación del correo por notificación institucional. Para personas que ya tienen correo institucional UPTC, indica acceso con ese correo sin dominio y sin registrar otro correo. Esta descripción pública no permite inferir si el acceso interno es federado, cómo se vincula a la identidad institucional ni cómo se almacenan o protegen las credenciales.

Las fechas citadas en el comunicado corresponden a 2026-II: venta de PIN presencial hasta el 29 de mayo e inscripción web desde el 1 de junio; para programas a distancia o virtuales, PIN hasta el 6 de junio e inscripción desde el 9 de junio. No deben trasladarse a otras convocatorias.

## Límites de la evidencia

La publicación confirma que UPTC anunció este sistema para 2026-II y describe parte de la experiencia de inscripción. No identifica su producto técnico, URL funcional, contratos, custodio de datos, APIs, base de datos, controles de acceso, retención/eliminación de documentos, conexión con el sistema de pagos/PIN ni fuente maestra por entidad.

La [Resolución 111 de 2026 para 2027-I](https://apps3.uptc.edu.co/compilacion-normativa-web/#/compilaciones-normativas/detalle-documento/9906) menciona un proceso de admisión SIRA en su calendario. La fuente sobre «Inscríbete» citada aquí se refiere a 2026-II. No se ha demostrado que ambos nombres describan el mismo producto, que «Inscríbete» sea parte de la Fase III del nuevo sistema académico, ni cuál plataforma ejecutará la inscripción de 2027-I. Tampoco se presume que el canal antiguo publicado en `registro.uptc.edu.co` represente el procedimiento vigente.

## Gate antes de implementar admisiones

Antes de desarrollar formularios, cuentas de aspirante, captura de PIN, carga de documentos, selección o migración de registros, la instancia institucional debe documentar:

1. si «Inscríbete» corresponde a SIRA, a la Fase III, a una solución independiente o a un componente conectado;
2. qué sistema opera cada paso de la convocatoria 2027-I y las siguientes, y cuál es la fuente maestra por entidad;
3. dueño funcional, custodia técnica, autoridad de decisión y responsabilidades entre DTIC, la instancia de Fase III y las áreas que UPTC designe; ACRA/Registro Académico es participación funcional sugerida por el patrocinador, y debe aclararse si se refiere a dos equipos o a la dependencia cuyo nombre público integra Control de Registro Académico;
4. interfaces autorizadas para PIN, identidad, validaciones y documentos; no se presume que exista una API;
5. finalidades, campos mínimos, tratamiento de aspirantes menores y datos sensibles, permisos, auditoría, retención, eliminación y respuesta a incidentes;
6. estrategia para reutilizar, integrar, coexistir o retirar componentes existentes sin crear doble captura ni dos registros oficiales.

Hasta cerrar el gate se permite continuar el descubrimiento, documentar calendarios públicos y diseñar modelos técnicos sin datos personales; no se implementa una segunda inscripción ni un repositorio paralelo de documentos. Ver también el [control de solapamiento de Fase III](uptc-new-academic-system-phase-iii.md) y el [descubrimiento del ciclo del estudiante](student-lifecycle-baseline.md).
