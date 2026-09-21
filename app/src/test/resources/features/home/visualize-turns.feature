# language: en
#
# Especificación ejecutable para la US "Visualizar mis turnos".
# Cubre el journey del consumidor que necesita ver los turnos
# agendados con cada prestador: acceder desde el Home, ver la
# lista, ver el empty state, inspeccionar la información de un
# turno (contraparte, motivo, monto, fecha, hora) y ver el estado
# del turno (Pendiente / Confirmado / Finalizado / Cancelado).
#
# **Fuera de scope de esta feature** (viven en 12-VT, post-MVP):
#  - Pantalla de detalle de un turno individual (`TurnoDetail`).
#  - CTA "Contactar" desde el detalle: el `conversation_id` se
#    obtiene de `GET /work-orders/{id}` (no del listado, que
#    debe quedarse liviano), por lo que el contact belongs to
#    the detail screen — no al listado de "Mis Turnos".
#
# Cada escenario arranca marcado con `@wip` (salteado). Cada
# commit remueve el `@wip` de exactamente un escenario, deja sus
# aserciones en verde y conserva el resto en `@wip`. Cuando se
# quita el último `@wip`, el feature está terminado. El runner
# de Cucumber JVM filtra `@wip` mediante la system property
# `cucumber.filter.tags` configurada en `app/build.gradle.kts`.
#
# Los textos visibles al usuario se validan en español y en
# inglés en los tests instrumentados de Compose, no aquí. El BDD
# asserta tipos de estado, efectos observables y navegación.
#
# El acceso a la app sin sesión ya está cubierto por el smart
# router de `LoResuelvoNav` y por `authentication-session.feature`,
# por lo que no se replica acá.
#
# **Nota sobre los keywords del Gherkin**: el `# language: en`
# declara la feature como inglesa y los keywords usados son los
# ingleses (`Given`, `And`, `When`, `Then`) — el mismo patrón
# que ya sigue `home.feature` del repo. Cucumber JVM matchea
# los steps por el texto que sigue al keyword, no por el keyword
# en sí; el cuerpo de cada step queda en español para alinear
# el journey con el resto del feature.

Feature: Visualizar mis turnos

  Como usuario
  Quiero visualizar mis turnos
  Para organizarme y recibir al prestador correctamente

  Background:
    Given que estoy autenticado como usuario
    And me encuentro en la pantalla Home

  # Acceso

  Scenario: 01-VT Acceder a Mis Turnos desde el Home
    When selecciono la opción "Mis Turnos"
    Then veo la pantalla "Mis Turnos"

  # Lista

  Scenario: 02-VT Visualizar mis turnos registrados
    Given que tengo turnos registrados
    When accedo a la pantalla "Mis Turnos"
    Then veo una lista con mis turnos

  Scenario: 03-VT Visualizar mensaje cuando no tengo turnos
    Given que no tengo turnos registrados
    When accedo a la pantalla "Mis Turnos"
    Then veo un mensaje indicando que no tengo turnos

  # Información de un turno

  Scenario: 04-VT Visualizar la información de un turno
    Given que tengo un turno registrado
    When accedo a la pantalla "Mis Turnos"
    Then veo el nombre y apellido de la contraparte
    And veo la foto de perfil de la contraparte
    And veo el motivo del servicio
    And veo el monto del servicio
    And veo la fecha del turno
    And veo la hora del turno

  Scenario: 05-VT Visualizar el estado del turno
    Given que tengo un turno registrado
    When visualizo el turno
    Then veo el estado actual del turno

  # Estados posibles

  Scenario: 06-VT Visualizar turno pendiente
    Given que tengo un turno con estado "Pendiente"
    When visualizo el turno
    Then veo el estado "Pendiente"

  Scenario: 07-VT Visualizar turno confirmado
    Given que tengo un turno con estado "Confirmado"
    When visualizo el turno
    Then veo el estado "Confirmado"

  Scenario: 08-VT Visualizar turno finalizado
    Given que tengo un turno con estado "Finalizado"
    When visualizo el turno
    Then veo el estado "Finalizado"

  Scenario: 09-VT Visualizar turno cancelado
    Given que tengo un turno con estado "Cancelado"
    When visualizo el turno
    Then veo el estado "Cancelado"

  # Errores

  Scenario: 13-VT Mostrar error de red al cargar turnos
    Given que el backend no responde
    When accedo a la pantalla "Mis Turnos"
    Then veo un mensaje de error de conexión
    And veo un botón para reintentar

  Scenario: 14-VT Mostrar error de servidor al cargar turnos
    Given que el backend responde con error
    When accedo a la pantalla "Mis Turnos"
    Then veo un mensaje de error del servidor
    And veo un botón para reintentar
