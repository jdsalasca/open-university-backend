# Fuentes públicas para estructura académica y periodos

Consulta de fuentes institucionales públicas verificada el 30 de septiembre de 2026. Este inventario orienta el modelo técnico; no es un catálogo maestro ni acredita reglas operativas internas.

## Estructura y adscripción

- El [Acuerdo 003 de 2024](https://www.uptc.edu.co/export/sites/default/secretaria_general/consejo_superior/acuerdos_2024/Acuerdo_003_2024.pdf) modifica parcialmente el [Acuerdo 067 de 2005](https://pagos.uptc.edu.co/DocCompNormativa/637169474139066250.pdf), incorpora el título de sedes regionales y fija su estructura académica-administrativa básica. Por eso el modelo separa unidades organizacionales y lugares, y permite vincular un programa a ambos ejes con vigencia.
- La [página de regionalización de UPTC](https://www.uptc.edu.co/sitio/portal/sitios/universidad/vic_aca/vic_acad/regi/) describe la incorporación de sedes regionales mediante el Acuerdo 003 de 2024.
- El [directorio institucional de programas de pregrado](https://www.uptc.edu.co/sitio/portal/sitios/programas_ofer/pregrado.html?id_campus=06) presenta filtros independientes para facultad, nivel, lugar de desarrollo y modalidad. El catálogo normalizado conserva estas dimensiones sin inferirlas a partir de cadenas históricas.

## Intersemestrales y calendarios

- El [Acuerdo 035 de 2017](https://www.uptc.edu.co/secretaria_general/consejo_superior/acuerdos_2017/index.html) reglamenta cursos intersemestrales ofrecidos durante el receso de periodos académicos.
- El [Acuerdo 017 de 2023](https://www.uptc.edu.co/export/sites/default/secretaria_general/consejo_superior/acuerdos_2023/Acuerdo_017_2023.pdf) modifica el Acuerdo 035 de 2017: establece entre 20 y 35 estudiantes para abrir un **curso** intersemestral y ajusta la regla de derechos pecuniarios. Esas condiciones pertenecen a la futura oferta de cursos; este hito solo crea el tipo y el ciclo administrativo del periodo/calendario, no crea grupos ni aplica el conteo de estudiantes.
- La página actual de [ACRA para estudiantes de pregrado](https://uptc.edu.co/sitio/portal/sitios/universidad/vic_aca/adm_reg/2estu/est_pre.html) publica el segundo semestre de 2026: inscripción web presencial/FESAD del 22 de junio al 10 de julio; inicio de clases presenciales el 10 de agosto (FESAD, 15 de agosto). Es evidencia pública de que las ventanas de trámite pueden preceder el rango lectivo. También lista resoluciones de calendario, por lo que el modelo conserva versiones y referencias oficiales en lugar de fijar fechas en código.

## Aplicación técnica y asuntos por validar

1. Las unidades, las sedes y sus relaciones tienen identidades separadas; las relaciones usan fechas de vigencia y un orden estable.
2. El programa conserva la identidad existente del catálogo. Su afiliación vigente a unidad y sede es una relación normalizada y fechada; textos de facultad/sede importados en revisiones antiguas no se tratan como fuente estructural.
3. El periodo regular y el intersemestral usan el mismo ciclo auditable `DRAFT → APPROVED → OPEN → CLOSED`; la cancelación se limita a `DRAFT` o `APPROVED`. La clasificación del intersemestral es una decisión de modelado para la operación del sistema, no una afirmación de que la norma citada cree un semestre separado.
4. El calendario publicado es inmutable. Una modificación crea una revisión que debe portar su propio acto de referencia, se publica y después se selecciona en el periodo; las revisiones previas permanecen consultables.
5. Antes de carga institucional, ACRA, Secretaría General y las dependencias responsables deben confirmar el maestro de facultades/escuelas/unidades, la jerarquía, códigos y orden, los lugares vigentes, la afiliación de cada programa, los comandos auditables para corregir vigencias/prioridades y el alcance exacto de la apertura/cierre del periodo.

No se cargaron nombres, fechas ni reglas de selección desde estas páginas en la base local. Compose continúa sin catálogo institucional sembrado. Los enlaces y actos públicos son antecedentes verificables; la aprobación del dueño de proceso y la fuente interna vigente siguen siendo gates de implementación institucional.
