Feature: Consultar detalle y evidencia de una orden de trabajo

  Como participante de una orden de trabajo
  quiero consultar su detalle y la evidencia fotográfica presentada
  para verificar el trabajo realizado antes del pago y conservar el historial contractual

  Background:
    Given que estoy autenticado como usuario
    And me encuentro en la pantalla Home

  @wip
  Scenario: 01-VTD El consumidor puede abrir el detalle de una orden desde la Home
    Given participa en una orden de trabajo
    When selecciona para ver detalle de la orden desde la Home
    Then el sistema debe mostrar el detalle de la orden

  @wip
  Scenario: 02-VTD Un participante puede abrir el detalle de una orden desde el Chat
    Given tengo una conversación abierta con el prestador "Juan Pérez"
    And participa en una orden de trabajo asociada a la conversación con "Juan Pérez"
    When selecciona para ver detalle de la orden desde el Chat
    Then el sistema debe mostrar el detalle de la orden

  Scenario: 03-VTD El detalle muestra la información principal de la orden
    Given participa en una orden de trabajo
    And selecciona para ver detalle de la orden desde la Home
    When la orden se encuentra en estado "scheduled"
    Then debe mostrar el nombre y apellido de la contraparte
    And debe mostrar el avatar de la contraparte
    And debe mostrar el rubro de la contraparte cuando corresponda
    And debe mostrar el estado "scheduled"
    And debe mostrar el monto acordado
    And debe mostrar la fecha y hora programada
    And debe mostrar la descripción original del servicio

  Scenario: 04-VTD Una orden awaiting_payment muestra la evidencia de finalización
    Given participa en una orden de trabajo
    And la orden se encuentra en estado "awaiting_payment"
    And la orden tiene un reporte de finalización
    When selecciona para ver detalle de la orden desde la Home
    Then debe mostrar la sección "Evidencia de finalización"
    And debe mostrar la fecha y hora en que se reportó la finalización
    And debe mostrar la descripción de entrega del prestador
    And debe mostrar las fotografías de evidencia

  @wip
  Scenario: 05-VTD Una orden paid muestra la evidencia de finalización
    Given participa en una orden de trabajo
    And la orden se encuentra en estado "paid"
    And la orden tiene un reporte de finalización
    When selecciona para ver detalle de la orden desde la Home
    Then debe mostrar la sección "Evidencia de finalización"
    And debe mostrar la fecha y hora en que se reportó la finalización
    And debe mostrar la descripción de entrega del prestador
    And debe mostrar las fotografías de evidencia

  @wip
  Scenario: 06-VTD El usuario puede visualizar una fotografía de evidencia en tamaño completo
    Given participa en una orden de trabajo con evidencia fotográfica
    And selecciona para ver detalle de la orden desde la Home
    When selecciona una fotografía de evidencia
    Then debe abrirse la fotografía en un visor de tamaño completo

  @wip
  Scenario: 07-VTD Una orden paid muestra la información del pago y la reseña
    Given participa en una orden de trabajo
    And la orden se encuentra en estado "paid"
    And el pago de la orden fue realizado
    And el consumidor ya emitió una reseña
    When selecciona para ver detalle de la orden desde la Home
    Then debe mostrar la fecha en que se saldó el pago
    And debe mostrar la reseña del consumidor
    And debe mostrar la calificación de la reseña

  @wip
  Scenario: 08-VTD Una orden paid sin reseña no muestra una reseña inexistente
    Given que el usuario está visualizando una orden
    And la orden se encuentra en estado "paid"
    And el consumidor todavía no emitió una reseña
    When selecciona para ver detalle de la orden desde la Home
    Then debe mostrar la fecha en que se saldó el pago
    And no debe mostrar una reseña inexistente

  @wip
  Scenario: 09-VTD El consumidor puede abonar una orden awaiting_payment
    Given que el consumidor está visualizando una orden
    And la orden se encuentra en estado "awaiting_payment"
    When se muestra el detalle de la orden
    Then debe mostrarse un apartado de pago pendiente antes de las categorías
    And debe destacarse la acción principal para abonar el saldo restante

  @wip
  Scenario: 10-VTD La evidencia no se muestra para una orden scheduled
    Given que el usuario está visualizando una orden
    And la orden se encuentra en estado "scheduled"
    When se muestra el detalle de la orden
    Then no debe mostrarse la sección "Evidencia de finalización"