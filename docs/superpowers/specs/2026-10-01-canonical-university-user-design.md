# Identidad canónica de usuario institucional

## Intención

Dar a cada persona un `user_id` UUID, estable e inmutable, para que las capacidades futuras puedan enlazar aspiración, admisión, matrícula, docencia, empleo y egreso sin crear cuentas paralelas por perfil. La identidad OIDC autentica a la persona; sus estados académicos y los permisos administrativos siguen siendo conceptos separados.

Esta entrega es infraestructura técnica. No modela ni activa procesos institucionales, no ingiere datos personales y no declara que la vinculación de identidades o los roles estén aprobados para operación real.

## Estado previo a V21

`institutional_identity` identifica cada par OIDC `issuer+subject` con un `identity_id`. Las asignaciones de rol, sus actores y su auditoría también apuntan a ese ID. El esquema no tiene una entidad de usuario canónico; por ello una misma persona vinculada a más de una identidad no tiene una clave estable para sus dominios y podría tratar cada vínculo como cuentas separadas.

## Modelo acordado

- Añadir `university_user(user_id, created_at)` sin nombres, correo, documento, contacto ni otros atributos personales.
- Añadir `institutional_identity.user_id` como FK no único a `university_user.user_id`. Cada par OIDC conserva una vinculación única; varias vinculaciones podrán apuntar al mismo usuario cuando exista un proceso institucional de asociación validado.
- Renombrar conceptualmente `RegisteredIdentity.id` como identificador técnico de la vinculación y exponer por separado su `userId` canónico.
- Cambiar asignaciones de rol para que destinatario y otorgante sean `user_id`; cada auditoría guarda tanto actor `user_id` como `actor_identity_id` del vínculo OIDC usado. Los perfiles derivados de ciclo estudiantil no se vuelven asignables manualmente ni se confunden con permisos.
- Resolver cada principal autenticado por su par OIDC a `user_id` antes de consultar asignaciones. La autoconcesión y la autorrevocación se comparan por `user_id`, no por un alias OIDC.

## Migración de datos

Flyway V21 crea un usuario para cada vinculación preexistente, usando su UUID `identity_id` como `user_id`. Luego asigna ese mismo valor a la vinculación, al destinatario y otorgante de cada rol y al `actor_user_id` de cada auditoría. Conserva `actor_identity_id` y su FK para identificar cuál vínculo autenticado ejecutó la acción. Esta correspondencia uno a uno conserva las asociaciones actuales; V21 no fusiona personas ni infiere equivalencias.

Los registros nuevos se crean dentro de una sola transacción: usuario canónico y primera vinculación OIDC. Si otra solicitud gana una carrera por el par único `issuer+subject`, la solicitud perdedora devolverá el usuario ya registrado y no dejará un usuario huérfano. Las pruebas de migración ejecutarán V1–V20, insertarán registros sintéticos heredados y después migrarán a V21, comprobando que cada asociación y referencia conserva su destino.

## Contratos de aplicación y API

- `IdentityDirectory` registra, consulta y busca vinculaciones; devuelve su `userId` y permite comprobar la existencia de un usuario canónico. No ofrece una operación para fusionar o enlazar identidades en esta entrega.
- `RoleAssignmentRepository` consulta, crea y revoca por UUID de usuario. Las transacciones de asignación y auditoría siguen siendo atómicas; el registro de auditoría comprueba que identidad federada y usuario canónico pertenecen entre sí.
- `GET /api/v1/me` incorpora `userId` al resumen sin retornar claims personales del token. Se mantiene temporalmente `subject` por compatibilidad del contexto de interfaz existente.
- La búsqueda administrativa de identidades mantiene `issuer` y `subject` opacos y añade `userId`. Consultar y crear asignaciones pasa a seleccionar el `userId` devuelto; la respuesta identifica el destinatario por `targetUserId`.
- React valida el UUID canónico como dato del servidor. No interpreta claims ni calcula permisos. El servidor sigue siendo la autoridad.

## Seguridad y límites

- El acceso federado continúa cerrado hasta que DTIC confirme issuer, audience, callbacks, claims y matriz de autorización. No añadir usuarios, tokens, contraseñas, grupos, proveedores, roles ni datos seed.
- La primera vinculación solo se crea tras una solicitud autenticada con principal válido. Un `user_id` por sí mismo nunca otorga permisos.
- No habrá endpoint de autoinscripción, vinculación, fusión o desvinculación de identidades. La vinculación múltiple requerirá un proceso con evidencia y autoridad institucional, fuera de este corte.
- No crear tablas de aspirante, admitido, estudiante, profesor, funcionario o egresado. Cuando esas capacidades se aprueben, sus entidades de dominio referenciarán este ID y validarán su propia fuente maestra.
- Mantener `Cache-Control: no-store` para identidad y acceso; las respuestas no incluirán correo ni claims adicionales.

## Verificación de aceptación

1. V21 transforma datos heredados sin perder ni reasignar identidades, roles o auditorías, y conserva FK e índices en H2/MySQL 8.4.
2. Registrar dos veces el mismo OIDC devuelve el mismo `identity_id` y `user_id`; una carrera no crea usuarios huérfanos.
3. Dos vinculaciones asociadas al mismo usuario consultan los mismos roles y permisos; una identidad distinta no los hereda.
4. Autoconceder o autorrevocar mediante otro alias del mismo `user_id` es rechazado.
5. `/api/v1/me`, búsqueda administrativa y asignaciones entregan/consumen los UUID canónicos esperados, sin exponer claims personales ni saltarse permisos del servidor.
6. Pruebas AAA cubren casos felices, usuario inexistente, asociación duplicada, carrera, formato inválido, autorización y rollback transaccional.
7. C4, modelo de datos, flujo de identidad, roadmap y guía de agentes describen la frontera nueva y aclaran que no se ha activado identidad institucional real.

## Fuera de alcance

Vinculación o fusión operativa de cuentas, migración desde directorios legados, expedientes personales, inscripción o selección de aspirantes, matrícula, nómina, identificación institucional, autorización por perfiles de dominio y cualquier corte de fuente oficial.

## Estado técnico de V21

El modelo canónico, el backfill V20→V21, la autorización por `user_id` y los contratos de API están implementados. Las asignaciones consultan usuarios canónicos; la auditoría conserva el usuario y el vínculo OIDC concreto, con validación de consistencia al insertar. OIDC permanece deshabilitado y el corte no crea asociación/fusión de identidades, PII ni perfiles del ciclo estudiantil.
