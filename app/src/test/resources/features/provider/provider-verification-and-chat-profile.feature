@provider-verification
Feature: Prestadores verificados y perfil desde el chat
  Como consumidor
  quiero identificar prestadores verificados y acceder a su perfil público
  para elegir con mayor confianza y consultar su información.

    Scenario: 01-PV Mostrar insignia en un listado de prestadores por categoría
    Given que un prestador tiene identity_verified igual a true
    When se muestra en el listado de su categoría
    Then se muestra la insignia de prestador verificado

    Scenario: 02-PV No mostrar insignia a un prestador no verificado
    Given que un prestador tiene identity_verified igual a false
    When se muestra en una superficie pública
    Then no se muestra la insignia de verificación

    Scenario: 03-PV Mostrar insignia en el perfil público
    Given que el perfil público pertenece a un prestador verificado
    When se abre el perfil del prestador
    Then se muestra la insignia junto a su identidad

    Scenario: 04-PV Mostrar insignia en una recomendación del chat con IA
    Given que la IA recomienda un prestador verificado
    When se muestra la recomendación
    Then se muestra la insignia de verificación

    Scenario: 05-PV Mostrar insignia en la cabecera del chat
    Given que el consumidor conversa con un prestador verificado
    When se muestra la cabecera de la conversación
    Then se muestra la insignia junto al nombre o avatar

    Scenario: 06-PV Abrir el perfil desde una conversación
    Given que existe una conversación cargada
    And pertenece al prestador "Juan Pérez"
    When el consumidor toca su foto o avatar
    Then se abre el perfil público del prestador "Juan Pérez"

    Scenario: 07-PV Abrir el perfil usando el avatar de fallback
    Given que el prestador no tiene foto de perfil
    And se muestra el avatar con la inicial de su nombre
    When el consumidor toca el avatar
    Then se abre el perfil público correcto

    Scenario: 08-PV No exponer información privada de verificación
    Given que un prestador está verificado
    When se muestra la insignia
    Then solo se muestra el estado público de verificación
    And no se muestran documentos ni datos internos

    Scenario: 09-PV Mantener las acciones existentes del chat
    Given que el consumidor está dentro de una conversación
    When utiliza volver o ver la propuesta
    Then cada acción conserva su comportamiento actual
