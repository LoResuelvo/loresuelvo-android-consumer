# language: es
#
# Especificación de US-50.2 para selección y preview de video.
# Se publica primero como @wip y se desmarca al validar la UI y el VM.

Característica: Seleccionar y previsualizar videos en el chat

  Como consumidor
  Quiero revisar un video antes de enviarlo
  Para confirmar que es la evidencia correcta

  @wip
  Escenario: Seleccionar un video válido muestra su preview
    Dado que estoy en una conversación abierta
    Cuando selecciono un video MP4 válido desde el menú de adjuntos
    Entonces veo una única tarjeta de preview con nombre, tamaño, duración y dimensiones
    Y el video reemplaza cualquier media pendiente incompatible

  @wip
  Escenario: Cancelar o rechazar un video no bloquea el chat
    Dado que estoy en una conversación abierta
    Cuando cancelo el picker o selecciono un video ilegible
    Entonces no se agrega un video pendiente
    Y puedo continuar enviando texto o media existente

  @wip
  Escenario: Un error de envío conserva el video para reintentar
    Dado que tengo un video válido en la tarjeta de preview
    Cuando falla la operación de envío
    Entonces la tarjeta de preview conserva el video
    Y puedo descartarlo sin volver a abrir el picker
