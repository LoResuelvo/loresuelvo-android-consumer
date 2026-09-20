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

Característica: Visualizar mis turnos

  Como usuario
  Quiero visualizar mis turnos
  Para organizarme y recibir al prestador correctamente

  Antecedente:
    Dado que estoy autenticado como usuario
    Y me encuentro en la pantalla Home

  # Acceso

  @wip
  Escenario: 01-VT Acceder a Mis Turnos desde el Home
    Cuando selecciono la opción "Mis Turnos"
    Entonces veo la pantalla "Mis Turnos"

  # Lista

  @wip
  Escenario: 02-VT Visualizar mis turnos registrados
    Dado que tengo turnos registrados
    Cuando accedo a la pantalla "Mis Turnos"
    Entonces veo una lista con mis turnos

  @wip
  Escenario: 03-VT Visualizar mensaje cuando no tengo turnos
    Dado que no tengo turnos registrados
    Cuando accedo a la pantalla "Mis Turnos"
    Entonces veo un mensaje indicando que no tengo turnos

  # Información de un turno

  @wip
  Escenario: 04-VT Visualizar la información de un turno
    Dado que tengo un turno registrado
    Cuando accedo a la pantalla "Mis Turnos"
    Entonces veo el nombre y apellido de la contraparte
    Y veo la foto de perfil de la contraparte
    Y veo el motivo del servicio
    Y veo el monto del servicio
    Y veo la fecha del turno
    Y veo la hora del turno

  @wip
  Escenario: 05-VT Visualizar el estado del turno
    Dado que tengo un turno registrado
    Cuando visualizo el turno
    Entonces veo el estado actual del turno

  # Estados posibles

  @wip
  Escenario: 06-VT Visualizar turno pendiente
    Dado que tengo un turno con estado "Pendiente"
    Cuando visualizo el turno
    Entonces veo el estado "Pendiente"

  @wip
  Escenario: 07-VT Visualizar turno confirmado
    Dado que tengo un turno con estado "Confirmado"
    Cuando visualizo el turno
    Entonces veo el estado "Confirmado"

  @wip
  Escenario: 08-VT Visualizar turno finalizado
    Dado que tengo un turno con estado "Finalizado"
    Cuando visualizo el turno
    Entonces veo el estado "Finalizado"

  @wip
  Escenario: 09-VT Visualizar turno cancelado
    Dado que tengo un turno con estado "Cancelado"
    Cuando visualizo el turno
    Entonces veo el estado "Cancelado"

  # Contactar

  @wip
  Escenario: 10-VT Contactar a la contraparte desde un turno
    Dado que tengo un turno registrado
    Cuando selecciono la opción "Contactar" del turno
    Entonces se abre la conversación con la contraparte

  # Errores

  @wip
  Escenario: 13-VT Mostrar error de red al cargar turnos
    Dado que el backend no responde
    Cuando accedo a la pantalla "Mis Turnos"
    Entonces veo un mensaje de error de conexión
    Y veo un botón para reintentar

  @wip
  Escenario: 14-VT Mostrar error de servidor al cargar turnos
    Dado que el backend responde con error
    Cuando accedo a la pantalla "Mis Turnos"
    Entonces veo un mensaje de error del servidor
    Y veo un botón para reintentar
