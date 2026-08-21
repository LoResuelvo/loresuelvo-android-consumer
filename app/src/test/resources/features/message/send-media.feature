# language: es
#
# Especificación ejecutable para la historia de usuario "Enviar
# fotos o audios en el chat". Cubre los criterios de aceptación:
# adjuntar imágenes desde la galería y la cámara del dispositivo,
# grabar y enviar audios, previsualizar los archivos antes de
# enviarlos, mostrar correctamente las imágenes y audios enviados
# dentro de la conversación, permitir visualizar/descargar los
# archivos recibidos, e informar errores de carga o envío.
#
# Cada escenario arranca marcado como `@wip` (salteado). Cada commit
# remueve el `@wip` de exactamente un escenario, deja sus
# aserciones en verde y conserva el resto en `@wip`. Cuando se
# remueve el último `@wip`, la feature está completa. El runner de
# Cucumber JVM filtra `@wip` mediante la system property
# `cucumber.filter.tags` configurada en `app/build.gradle.kts`.
#
# Actualizar este archivo junto con `strings.xml` y las pantallas
# cada vez que cambia el copy o el comportamiento visible.
# Los textos visibles en español se asertan en los acceptance tests
# de Compose, no aquí.

Característica: Enviar fotos o audios en el chat

  Como consumidor
  Quiero enviar fotos o audios adjuntos en el chat con un prestador
  Para explicar mejor mi problema y facilitar la comunicación

  Antecedentes:
    Dado estoy autenticado como consumidor
    Y tengo una conversación abierta con el prestador "Juan Pérez"

  Escenario: 01-MM Adjuntar una imagen desde la galería del dispositivo
    Dado que estoy en la conversación con "Juan Pérez"
    Cuando toco el botón de adjuntar imagen desde la galería
    Y selecciono la imagen "foto-baño.jpg"
    Entonces veo la vista previa de la imagen seleccionada
    Y puedo confirmar el envío o descartarla

  Escenario: 02-MM Capturar una imagen con la cámara del dispositivo
    Dado que estoy en la conversación con "Juan Pérez"
    Cuando toco el botón de adjuntar imagen desde la cámara
    Y capturo la foto "gotera-baño.jpg"
    Entonces veo la vista previa de la foto capturada
    Y puedo confirmar el envío o descartarla

  Escenario: 03-MM Grabar un audio y enviarlo
    Dado que estoy en la conversación con "Juan Pérez"
    Cuando toco el botón de grabar audio
    Y grabo un audio de 5 segundos
    Entonces veo la vista previa del audio grabado
    Y puedo reproducirlo antes de enviarlo
    Y puedo confirmar el envío

  Escenario: 04-MM La imagen enviada se muestra correctamente en la conversación
    Dado que envié una imagen en la conversación con "Juan Pérez"
    Cuando accedo a esa conversación
    Entonces veo la burbuja de la imagen enviada en el hilo
    Y la burbuja expone la miniatura de la imagen enviada

  Escenario: 05-MM El audio enviado se muestra correctamente en la conversación
    Dado que envié un audio en la conversación con "Juan Pérez"
    Cuando accedo a esa conversación
    Entonces veo la burbuja del audio enviado en el hilo
    Y la burbuja muestra la duración del audio enviado

  @wip
  Escenario: 06-MM Visualizar una imagen recibida del prestador
    Dado que el prestador "Juan Pérez" me envió una imagen
    Cuando accedo a la conversación con "Juan Pérez"
    Y toco la burbuja de la imagen recibida
    Entonces la imagen se abre en pantalla completa

  @wip
  Escenario: 07-MM Reproducir un audio recibido del prestador
    Dado que el prestador "Juan Pérez" me envió un audio
    Cuando accedo a la conversación con "Juan Pérez"
    Y toco la burbuja del audio recibido
    Entonces el audio comienza a reproducirse

  @wip
  Escenario: 08-MM Error de red al enviar una imagen
    Dado que el backend no responde
    Cuando adjunto una imagen desde la galería y confirmo el envío
    Entonces veo un error de red
    Y la imagen NO aparece en la conversación

  @wip
  Escenario: 09-MM Error al enviar un audio que excede el tamaño máximo permitido
    Dado que grabé un audio que excede el tamaño máximo permitido
    Cuando intento confirmar el envío
    Entonces veo un error de tamaño excedido
    Y el audio NO aparece en la conversación

  @wip
  Escenario: 10-MM Cancelar el envío de una imagen antes de confirmar
    Dado que seleccioné una imagen de la galería
    Y veo la vista previa de la imagen seleccionada
    Cuando descarto la vista previa
    Entonces NO se crea ninguna burbuja en la conversación