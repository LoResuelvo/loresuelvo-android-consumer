# language: es
#
# Especificación de US-50.2 para el modelo Android de video.
# Los escenarios se publican como @wip antes de implementar el contrato.

Característica: Modelo de video para mensajes del chat

  Como consumidor
  Quiero conservar un video y sus metadatos en Android
  Para poder previsualizarlo, enviarlo y reproducirlo en las siguientes issues

  Escenario: Un video válido se representa como upload con metadata
    Dado que tengo un archivo de video MP4 H.264 de 20 segundos
    Cuando Android lee el archivo para el chat
    Entonces el dominio conserva bytes, MIME, nombre, duración y dimensiones
    Y el video pasa las validaciones de tamaño y formato

  Escenario: Un video recibido conserva su bloque en el dominio
    Dado que el backend devuelve un mensaje con metadata de video
    Cuando Android mapea el mensaje REST o WebSocket
    Entonces el mensaje conserva la referencia de video sin convertirla en imagen

  Escenario: Un video fuera de los límites se rechaza con un error específico
    Dado que tengo un video que supera uno de los límites del chat
    Cuando Android valida el archivo antes del upload
    Entonces obtiene un error tipado de video y no inicia la subida
