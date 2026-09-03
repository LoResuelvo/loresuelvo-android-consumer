# language: es
#
# Especificación ejecutable para la US-54 "Visualizar propuesta
# de servicio". Cubre el journey del consumidor que recibe
# propuestas de prestadores, las explora desde el inicio y desde
# "Mis Servicios", abre el detalle, salta a la conversación
# asociada y consulta el tiempo estimado tanto en la propuesta
# como en la orden de trabajo.
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
# asserta tipos de error y efectos observables.
#
# El acceso a la app sin sesión ya está cubierto por el smart
# router de `LoResuelvoNav` y por `authentication-session.feature`,
# por lo que no se replica acá. El caso de sesión expirada
# (401 mid-flow) es una mejora pendiente del feature de auth.

Feature: Visualizar propuesta de servicio

  Como consumidor
  quiero acceder a la vista detallada de una propuesta de servicio
  para revisar sus condiciones, conocer su estado actual y acceder a la conversación.

  Background:
    Given que el usuario tiene una sesión iniciada
    And que el usuario tiene propuestas de servicio recibidas

  # Inicio

  @wip
  Scenario: 01-VSP Visualizar propuestas que requieren atención en el inicio
    Given que entre las propuestas recibidas hay pendientes
    When accede al inicio
    Then debe visualizar dichas propuestas destacadas

  @wip
  Scenario: 02-VSP Visualizar próximos trabajos en el inicio
    Given que entre las propuestas recibidas hay aceptadas
    When accede al inicio
    Then debe visualizar los trabajos próximos destacados

  # Mis Servicios

  @wip
  Scenario: 03-VSP Visualizar todas las propuestas de servicio
    When accede a "Mis Servicios"
    Then debe visualizar todas sus propuestas de servicio

  @wip
  Scenario: 04-VSP Visualizar las propuestas más recientes primero
    Given que el usuario tiene varias propuestas de servicio con fechas distintas
    When accede a "Mis Servicios"
    Then debe visualizar primero la propuesta más reciente

  @wip
  Scenario: 05-VSP Consultar propuestas que requieren atención
    Given que el usuario tiene propuestas en diferentes estados
    When selecciona el filtro de propuestas que requieren su atención
    Then debe visualizar únicamente las propuestas pendientes

  @wip
  Scenario: 06-VSP Consultar propuestas aceptadas
    Given que el usuario tiene propuestas en diferentes estados
    When selecciona el filtro de propuestas aceptadas
    Then debe visualizar únicamente las propuestas aceptadas

  @wip
  Scenario: 07-VSP Consultar propuestas rechazadas
    Given que el usuario tiene propuestas en diferentes estados
    When selecciona el filtro de propuestas rechazadas
    Then debe visualizar únicamente las propuestas rechazadas

  # Detalle de la propuesta

  @wip
  Scenario: 08-VSP Consultar el detalle de una propuesta de servicio
    Given que existe una propuesta de servicio
    When el usuario accede a su detalle
    Then debe visualizar el nombre completo del prestador
    And debe visualizar el rubro del prestador
    And debe visualizar el motivo de la visita
    And debe visualizar el monto acordado
    And debe visualizar la fecha y hora acordadas
    And debe visualizar el estado actual de la propuesta

  @wip
  Scenario: 09-VSP Visualizar la foto del prestador
    Given que el prestador tiene una foto de perfil
    When el usuario consulta el detalle de su propuesta
    Then debe visualizar la foto de perfil del prestador

  @wip
  Scenario: 10-VSP Mostrar imagen predeterminada cuando el prestador no tiene foto
    Given que el prestador no tiene una foto de perfil
    When el usuario consulta el detalle de su propuesta
    Then debe visualizar una imagen predeterminada

  @wip
  Scenario: 11-VSP Visualizar el monto acordado formateado como moneda
    Given que existe una propuesta de servicio por un monto de 15000 pesos
    When el usuario consulta el detalle de la propuesta
    Then debe visualizar el monto como "$ 15.000"

  @wip
  Scenario: 12-VSP Visualizar la fecha y hora acordadas
    Given que existe una propuesta de servicio para el 15 de octubre de 2026 a las 14:30
    When el usuario consulta el detalle de la propuesta
    Then debe visualizar la fecha y hora como "15/10/2026 - 14:30 hs"

  # Conversación

  @wip
  Scenario: 13-VSP Acceder a la conversación desde una propuesta
    Given que el usuario está consultando una propuesta de servicio
    When selecciona "Ver conversación"
    Then debe acceder a la conversación relacionada con la propuesta

  @wip
  Scenario: 14-VSP Consultar el resumen de la propuesta desde la conversación
    Given que existe una conversación relacionada con una propuesta de servicio
    When el usuario accede a la conversación
    Then debe visualizar un resumen de la propuesta
    And debe visualizar el monto acordado
    And debe visualizar la fecha acordada
    And debe visualizar la descripción del servicio
    And debe visualizar el estado actual de la propuesta

  # Duración estimada

  @wip
  Scenario Outline: 15-VSP Visualizar la duración estimada del servicio
    Given que existe una propuesta de servicio con una duración estimada de <duracion>
    When el usuario consulta el detalle de la propuesta
    Then debe visualizar la duración estimada como "<resultado>"

    Examples:
      | duracion       | resultado  |
      | 45 minutos     | 45 min     |
      | 1 hora         | 1 h        |
      | 1 hora 30 min  | 1 h 30 min |
      | 2 horas        | 2 h        |

  # Orden de trabajo

  @wip
  Scenario: 16-VSP Consultar el tiempo estimado de trabajo en la orden
    Given que existe una orden de trabajo con un tiempo estimado para realizar el servicio
    When el usuario consulta el detalle de la orden de trabajo
    Then debe visualizar el tiempo estimado de trabajo junto con los datos acordados del servicio

  # Estados vacíos

  @wip
  Scenario: 17-VSP No existen propuestas para mostrar
    Given que el usuario no tiene propuestas de servicio
    When accede a la sección correspondiente
    Then debe visualizar un mensaje indicando que no tiene propuestas para mostrar

  @wip
  Scenario: 18-VSP No existen propuestas para el estado seleccionado
    Given que el usuario tiene propuestas de servicio
    And no tiene propuestas correspondientes al estado seleccionado
    When selecciona dicho estado
    Then debe visualizar un mensaje indicando que no hay propuestas para mostrar
