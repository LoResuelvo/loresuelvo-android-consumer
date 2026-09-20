# language: es
#
# Especificación ejecutable para la US "Visualizar mis turnos".
# Cubre el journey del consumidor que necesita ver los turnos
# agendados con cada prestador: acceder desde el Home, ver la
# lista, ver el empty state, inspeccionar la información de un
# turno (contraparte, motivo, monto, fecha, hora), ver el estado
# del turno (Pendiente / Confirmado / Finalizado / Cancelado) y
# contactar a la contraparte.
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
# **Nota sobre los keywords del Gherkin**: aunque el `# language:
# es` declara la feature como española, los keywords usados son
# los ingleses (`Given`, `And`, `When`, `Then`) — el patrón que
# ya siguen `visualize-service-proposal.feature` y otros del
# repo. Cucumber JVM matchea los steps por el texto que sigue al
# keyword, no por el keyword en sí.

Característica: Visualizar mis turnos

  Como usuario
  Quiero visualizar mis turnos
  Para organizarme y recibir al prestador correctamente

  Antecedente:
    Given que estoy autenticado como usuario
    And me encuentro en la pantalla Home

  # Acceso

  Escenario: 01-VT Acceder a Mis Turnos desde el Home
    When selecciono la opción "Mis Turnos"
    Then veo la pantalla "Mis Turnos"

  # Lista

  Escenario: 02-VT Visualizar mis turnos registrados
    Given que tengo turnos registrados
    When accedo a la pantalla "Mis Turnos"
    Then veo una lista con mis turnos

  Escenario: 03-VT Visualizar mensaje cuando no tengo turnos
    Given que no tengo turnos registrados
    When accedo a la pantalla "Mis Turnos"
    Then veo un mensaje indicando que no tengo turnos

  # Información de un turno

  Escenario: 04-VT Visualizar la información de un turno
    Given que tengo un turno registrado
    When accedo a la pantalla "Mis Turnos"
    Then veo el nombre y apellido de la contraparte
    And veo la foto de perfil de la contraparte
    And veo el motivo del servicio
    And veo el monto del servicio
    And veo la fecha del turno
    And veo la hora del turno

  Escenario: 05-VT Visualizar el estado del turno
    Given que tengo un turno registrado
    When visualizo el turno
    Then veo el estado actual del turno

  # Estados posibles

  @wip
  Escenario: 06-VT Visualizar turno pendiente
    Given que tengo un turno con estado "Pendiente"
    When visualizo el turno
    Then veo el estado "Pendiente"

  @wip
  Escenario: 07-VT Visualizar turno confirmado
    Given que tengo un turno con estado "Confirmado"
    When visualizo el turno
    Then veo el estado "Confirmado"

  @wip
  Escenario: 08-VT Visualizar turno finalizado
    Given que tengo un turno con estado "Finalizado"
    When visualizo el turno
    Then veo el estado "Finalizado"

  @wip
  Escenario: 09-VT Visualizar turno cancelado
    Given que tengo un turno con estado "Cancelado"
    When visualizo el turno
    Then veo el estado "Cancelado"

  # Contactar

  @wip
  Escenario: 10-VT Contactar a la contraparte desde un turno
    Given que tengo un turno registrado
    When selecciono la opción "Contactar" del turno
    Then se abre la conversación con la contraparte

  # Errores

  @wip
  Escenario: 13-VT Mostrar error de red al cargar turnos
    Given que el backend no responde
    When accedo a la pantalla "Mis Turnos"
    Then veo un mensaje de error de conexión
    And veo un botón para reintentar

  @wip
  Escenario: 14-VT Mostrar error de servidor al cargar turnos
    Given que el backend responde con error
    When accedo a la pantalla "Mis Turnos"
    Then veo un mensaje de error del servidor
    And veo un botón para reintentar
