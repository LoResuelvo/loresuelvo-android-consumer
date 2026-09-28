Feature: Visualizar historial de trabajos y reseñas de un prestador

  Como consumidor
  quiero visualizar el historial de trabajos realizados y las reseñas de un prestador en su perfil público
  para evaluar su experiencia, calidad de atención y confiabilidad antes de contactarlo

  Background:
    Given estoy autenticado como consumidor

  Scenario: 01-CT Visualizar reputación de un prestador recomendado por el chat con IA
    Given estoy visualizando prestadores recomendados por el chat con IA
    And uno de los prestadores tiene calificaciones recibidas
    When accedo al perfil del prestador recomendado
    Then veo su calificación promedio representada con estrellas
    And veo el valor numérico de su calificación promedio
    And veo la cantidad total de reseñas recibidas

  Scenario: 02-CT Visualizar reputación de un prestador encontrado desde una categoría
    Given estoy visualizando prestadores de una categoría
    And uno de los prestadores tiene calificaciones recibidas
    When accedo al perfil de ese prestador
    Then veo su calificación promedio representada con estrellas
    And veo el valor numérico de su calificación promedio
    And veo la cantidad total de reseñas recibidas

  @wip
  Scenario: 03-CT Visualizar historial de trabajos completados de un prestador
    Given estoy visualizando el perfil de un prestador
    And el prestador tiene trabajos completados
    When consulto su historial de trabajos
    Then veo los trabajos completados del prestador
    And los trabajos están ordenados desde el más reciente al más antiguo

  @wip
  Scenario: 04-CT Visualizar información de un trabajo completado
    Given estoy visualizando el historial de un prestador
    And el prestador tiene un trabajo completado
    When visualizo el trabajo en el historial
    Then veo la fecha programada del trabajo
    And veo la descripción del trabajo realizado
    And veo el reporte de entrega redactado por el prestador

  @wip
  Scenario: 05-CT Visualizar reseña asociada a un trabajo completado
    Given estoy visualizando el historial de un prestador
    And el prestador tiene un trabajo completado con una reseña
    When visualizo el trabajo en el historial
    Then veo la calificación recibida representada con estrellas
    And veo el comentario de la reseña

  @wip
  Scenario: 06-CT Visualizar múltiples trabajos y sus reseñas
    Given estoy visualizando el perfil de un prestador
    And el prestador tiene múltiples trabajos completados
    And algunos de los trabajos tienen reseñas
    When consulto su historial de trabajos
    Then veo todos sus trabajos completados
    And los trabajos están ordenados desde el más reciente al más antiguo
    And veo la calificación y el comentario en los trabajos que tienen una reseña

    # tal vez sea mejor que no muestre nada directamente
  @wip
  Scenario: 07-CT Visualizar prestador sin calificaciones
    Given estoy visualizando el perfil de un prestador
    And el prestador no tiene calificaciones recibidas
    When visualizo su resumen de reputación
    Then veo una calificación promedio de 0
    And veo que tiene 0 reseñas

  @wip
  Scenario: 08-CT Visualizar prestador sin trabajos completados
    Given estoy visualizando el perfil de un prestador
    And el prestador no tiene trabajos completados
    When consulto su historial de trabajos
    Then veo un estado vacío indicando que todavía no tiene trabajos completados

  @wip
  Scenario: 09-CT Visualizar trabajo completado sin reseña
    Given estoy visualizando el historial de un prestador
    And el prestador tiene un trabajo completado sin reseña
    When visualizo el trabajo en el historial
    Then veo la información del trabajo realizado
    And no veo una calificación asociada al trabajo
    And no veo un comentario de reseña asociado al trabajo

  @wip
  Scenario: 10-CT No visualizar información económica en el historial público
    Given estoy visualizando el historial de un prestador
    And el prestador tiene trabajos completados
    When visualizo la información de los trabajos
    Then no veo los importes acordados
    And no veo los precios abonados

  @wip
  Scenario: 11-CT No visualizar datos personales de clientes en el historial público
    Given estoy visualizando el historial de un prestador
    And el prestador tiene trabajos completados
    When visualizo la información de los trabajos
    Then no veo nombres de los clientes
    And no veo datos personales de los clientes

  @wip
  Scenario: 12-CT No visualizar fotografías privadas de evidencia
    Given estoy visualizando el historial de un prestador
    And un trabajo completado tiene fotografías de evidencia
    When visualizo ese trabajo en el historial
    Then no veo las fotografías privadas de evidencia
