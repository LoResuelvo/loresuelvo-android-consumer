# language: en

Feature: Visualizar mis turnos

  Como usuario
  Quiero visualizar mis turnos
  Para organizarme y recibir al prestador correctamente

  Background:
    Given que estoy autenticado como usuario
    And me encuentro en la pantalla Home


  Scenario: 01-VT Acceder a Mis Turnos desde el Home
    When selecciono la opción "Mis Turnos"
    Then veo la pantalla "Mis Turnos"


  Scenario: 02-VT Visualizar mis turnos registrados
    Given que tengo turnos registrados
    When accedo a la pantalla "Mis Turnos"
    Then veo una lista con mis turnos

  Scenario: 03-VT Visualizar mensaje cuando no tengo turnos
    Given que no tengo turnos registrados
    When accedo a la pantalla "Mis Turnos"
    Then veo un mensaje indicando que no tengo turnos


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
