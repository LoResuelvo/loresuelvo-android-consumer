Feature: Calificar trabajo terminado

  Como consumidor
  Quiero calificar el trabajo terminado
  Para dejar constancia de mi experiencia con el prestador y ayudar a la comunidad

  Background:
    Given estoy autenticado como consumidor

  Scenario: 01-CT Visualizar opción para calificar una orden pagada
    Given tengo una orden de trabajo completamente pagada
    When accedo al detalle de la orden
    Then veo la opción "Calificar servicio"

  @wip
  Scenario: 02-CT Abrir formulario de calificación
    Given tengo una orden de trabajo completamente pagada
    When selecciono la opción "Calificar servicio"
    Then veo el formulario de calificación
    And veo un selector de 1 a 5 estrellas
    And veo un campo opcional para ingresar un comentario
    And veo el contador de caracteres "0/500"

  @wip
  Scenario: 03-CT Seleccionar una calificación
    Given tengo abierto el formulario de calificación
    When selecciono 4 estrellas
    Then veo 4 estrellas seleccionadas
    And el botón "Enviar" queda habilitado

  @wip
  Scenario: 04-CT Ingresar comentario de la calificación
    Given tengo abierto el formulario de calificación
    And seleccioné 5 estrellas
    When ingreso el comentario "Excelente trabajo y muy buena atención"
    Then veo el contador actualizado con la cantidad de caracteres ingresados
    And puedo enviar la calificación sin completar el comentario

  @wip
  Scenario: 05-CT Enviar calificación exitosamente
    Given tengo una orden de trabajo completamente pagada
    And seleccioné 5 estrellas
    And ingresé el comentario "Excelente trabajo"
    When selecciono "Enviar"
    Then se registra la calificación correctamente
    And veo una confirmación de agradecimiento
    And veo las 5 estrellas doradas en el detalle de la orden
    And veo el comentario "Excelente trabajo"
    And ya no puedo volver a calificar la orden

  @wip
  Scenario: 06-CT Calificar sin comentario
    Given tengo una orden de trabajo completamente pagada
    And seleccioné 4 estrellas
    And no ingresé ningún comentario
    When selecciono "Enviar"
    Then se registra la calificación correctamente
    And veo las 4 estrellas doradas en el detalle de la orden
    And no veo ningún comentario asociado

  @wip
  Scenario: 07-CT Impedir calificar una orden ya reseñada
    Given tengo una orden de trabajo completamente pagada
    And la orden ya tiene una calificación realizada por el consumidor
    When accedo al detalle de la orden
    Then veo la calificación realizada
    And no veo la opción "Calificar servicio"

  @wip
  Scenario: 08-CT Limitar comentario a 500 caracteres
    Given tengo abierto el formulario de calificación
    When ingreso un comentario de 600 caracteres
    Then el sistema no permite superar los 500 caracteres
    And el contador muestra en rojo "600/500"

  @wip
  Scenario: 09-CT Impedir envío sin calificación
    Given tengo abierto el formulario de calificación
    And no seleccioné ninguna estrella
    Then el botón "Enviar" está deshabilitado

  @wip
  Scenario: 10-CT Deshabilitar calificación con saldo pendiente
    Given tengo una orden de trabajo con saldo pendiente
    When accedo al detalle de la orden
    Then veo un mensaje indicando que la orden debe estar completamente pagada para poder calificarla

  @wip
  Scenario: 11-CT Informar error de servicio al enviar la calificación
    Given tengo una orden de trabajo completamente pagada
    And seleccioné 5 estrellas
    When envío la calificación y el servicio no está disponible
    Then veo un mensaje de error indicando que no se pudo registrar la calificación

