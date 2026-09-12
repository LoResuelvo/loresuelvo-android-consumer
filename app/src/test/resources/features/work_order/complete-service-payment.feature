Feature: Confirmar acuerdo de servicio

  Como consumidor
  quiero ver y confirmar en la app el acuerdo del servicio enviado por el prestador
  para aceptar los términos pactados y avanzar con la contratación

  @wip
  Scenario: Visualizar el acuerdo enviado por el prestador
    Given que tengo un acuerdo de servicio enviado por un prestador
    When consulto el acuerdo
    Then veo la descripción del servicio
    And veo el precio acordado
    And veo la fecha o el horario estimado del servicio cuando corresponda

  @wip
  Scenario: Confirmar que quiero aceptar el acuerdo
    Given que tengo un acuerdo de servicio pendiente de confirmación
    When selecciono la opción para confirmar el acuerdo
    Then veo un mensaje solicitando confirmar la aceptación del acuerdo

  @wip
  Scenario: Cancelar la confirmación del acuerdo
    Given que estoy confirmando un acuerdo de servicio
    When cancelo el mensaje de confirmación
    Then el acuerdo permanece pendiente de confirmación
    And no se inicia la contratación

  @wip
  Scenario: Mostrar un indicador mientras se procesa la confirmación
    Given que confirmé que quiero aceptar el acuerdo
    When se está procesando la confirmación
    Then veo un indicador de carga
    And no puedo confirmar nuevamente el acuerdo

  Scenario: Iniciar el pago de la seña para confirmar el acuerdo
    Given que tengo un acuerdo de servicio pendiente de confirmación
    And conozco el importe de la seña correspondiente
    When confirmo que quiero aceptar el acuerdo
    Then soy dirigido al proceso de pago de la seña

  Scenario: Confirmar el acuerdo cuando la seña fue pagada correctamente
    Given que inicié el pago de la seña de un acuerdo de servicio
    When el pago es aprobado
    And regreso a LoResuelvo
    Then veo un mensaje indicando que el acuerdo fue confirmado correctamente
    And la solicitud de servicio refleja que el acuerdo fue aceptado

  Scenario: Informar que la seña no pudo ser pagada
    Given que inicié el pago de la seña de un acuerdo de servicio
    When el pago es rechazado
    And regreso a LoResuelvo
    Then veo un mensaje indicando que la seña no pudo ser pagada
    And el acuerdo permanece pendiente de confirmación
    And puedo intentar nuevamente

  @wip
  Scenario: Informar que el pago de la seña continúa en proceso
    Given que inicié el pago de la seña de un acuerdo de servicio
    When el pago continúa en proceso
    And regreso a LoResuelvo
    Then veo un mensaje indicando que el pago está siendo procesado
    And el acuerdo permanece pendiente de confirmación
    And la aplicación continúa consultando el estado del pago

  @wip
  Scenario: Confirmar el acuerdo después de que la seña queda aprobada
    Given que inicié el pago de la seña de un acuerdo de servicio
    And regreso a LoResuelvo mientras el pago continúa en proceso
    When la seña finalmente es aprobada
    Then veo un mensaje indicando que el acuerdo fue confirmado correctamente
    And la solicitud de servicio refleja que el acuerdo fue aceptado

  @wip
  Scenario: Informar el rechazo de la seña después de regresar mientras estaba en proceso
    Given que inicié el pago de la seña de un acuerdo de servicio
    And regreso a LoResuelvo mientras el pago continúa en proceso
    When la seña finalmente es rechazada
    Then veo un mensaje indicando que la seña no pudo ser pagada
    And el acuerdo permanece pendiente de confirmación
    And puedo intentar nuevamente

  @wip
  Scenario: Informar un error al confirmar el acuerdo
    Given que tengo un acuerdo de servicio pendiente de confirmación
    When intento confirmar el acuerdo
    And no es posible procesar la confirmación
    Then veo un mensaje indicando que no fue posible confirmar el acuerdo
    And el acuerdo permanece sin cambios

  @wip
  Scenario: No permitir editar los términos al confirmar el acuerdo
    Given que tengo un acuerdo de servicio pendiente de confirmación
    When consulto el acuerdo
    Then puedo revisar sus condiciones
    But no puedo modificar la descripción del servicio
    And no puedo modificar el precio acordado
    And no puedo modificar la fecha o el horario acordado

  @wip
  Scenario: Actualizar el estado de la solicitud después de confirmar el acuerdo
    Given que tengo un acuerdo de servicio pendiente de confirmación
    And la seña fue pagada correctamente
    When regreso a la solicitud de servicio
    Then la solicitud refleja que el acuerdo fue aceptado