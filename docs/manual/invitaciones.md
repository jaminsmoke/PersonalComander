# Invitaciones

**Gestión → Invitaciones** es la bandeja de invitaciones que te envían los establecimientos. La verdad vive en Identity (servicio camareros); Commander las lista con tu sesión.

<div class="pc-doc-shot" markdown>

![Invitaciones](../screenshots/invitaciones.png)

*Pendientes para aceptar o rechazar; debajo, las ya respondidas.*

</div>

## Sin sesión

El botón **Entrar** abre el login. Sin cuenta no hay bandeja.

## Con sesión

Al entrar se pide `GET /v1/camareros/me/invitaciones`. El icono de actualizar vuelve a pedirlas.

- **Pendientes:** nombre del local, rol y caducidad. **Aceptar** o **Rechazar** piden confirmación.
- **Anteriores:** aceptadas, rechazadas, caducadas o revocadas. Solo lectura. Las membresías resultantes se ven en [Locales](locales.md).

Si no hay ninguna, el estado vacío ofrece **Ver mi ficha** (QR). El email del magic-link sigue existiendo; esta pantalla es el camino in-app.

Aceptar crea la membresía en Identity y recarga Locales. Commander no llama al servicio de negocio (`:8082`).
