# language: es
#
# Especificación de US-50.2 para el envío de videos.
# Se publica primero como @wip y se desmarca después de validar el flujo.

Característica: Enviar videos al prestador desde el chat

  Como consumidor
  Quiero enviar un video con o sin texto
  Para explicar mejor el problema al prestador

  Escenario: Enviar un video con caption completa presign, upload y confirm
    Dado que tengo un video válido pendiente en una conversación activa
    Y escribí un caption opcional para el prestador
    Cuando confirmo el envío del video
    Entonces se presigna y confirma un archivo con propósito de video
    Y se envía el video con el caption en el mensaje
    Y la preview se limpia y aparece una sola burbuja persistida

  Escenario: Un video sin caption también puede enviarse
    Dado que tengo un video válido pendiente en una conversación pendiente
    Cuando confirmo el envío del video sin caption
    Entonces se envía solo el video sin combinarlo con imágenes o audio
    Y la preview se limpia al recibir la respuesta del servidor

  Escenario: Un fallo del flujo conserva el video para reintentar
    Dado que tengo un video válido pendiente en una conversación activa
    Cuando falla el presign, upload, confirm o envío del mensaje
    Entonces el video sigue pendiente
    Y puedo reintentar sin volver a abrir el picker
