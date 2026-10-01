# language: es
#
# Especificación de US-50.2 para reproducir videos en el chat.
# Se publica primero como @wip y se desmarca luego de validar la implementación.

Característica: Reproducir videos dentro del chat

  Como consumidor
  Quiero reproducir videos enviados o recibidos
  Para consultar la evidencia sin salir de la conversación

  @wip
  Escenario: Un video REST se muestra con controles de reproducción
    Dado que la conversación contiene un mensaje de video válido
    Cuando abro la burbuja del video
    Entonces veo el player con play, pausa, progreso y duración
    Y el caption del mensaje conserva su texto

  @wip
  Escenario: Un video recibido por WebSocket se puede reproducir
    Dado que recibo por WebSocket un mensaje de video válido
    Cuando selecciono ese video para reproducirlo
    Entonces solo ese video queda seleccionado para reproducción

  @wip
  Escenario: Un video no disponible muestra recuperación sin afectar el chat
    Dado que la URL privada del video devuelve un error de reproducción
    Cuando intento reproducirlo nuevamente
    Entonces veo un error accesible con una acción de reintento
    Y los demás mensajes del chat siguen disponibles

  @wip
  Escenario: El preview de conversaciones no reproduce el último video
    Dado que una conversación tiene un video como último mensaje
    Cuando se muestra la lista de conversaciones
    Entonces veo un indicador de video en el preview
    Y no se inicia ningún player en la lista
