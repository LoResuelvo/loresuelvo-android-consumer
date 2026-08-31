# language: es
#
# Especificación ejecutable para la historia de usuario "UX/UI Fixes".
# Cubre los criterios de aceptación:
# ocultar el ícono de audio de IA cuando la funcionalidad no está
# disponible, visualizar todas las categorías disponibles, adjuntar
# imágenes en las ofertas de trabajo, previsualizar y eliminar
# imágenes antes de publicar, indicar nuevos mensajes en la lista
# de chats, actualizar el indicador al leer los mensajes y corregir
# bordes y márgenes de las pantallas.
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

Característica: Correcciones de UX/UI

  Como consumidor
  Quiero visualizar y utilizar correctamente la aplicación
  Para poder realizar las acciones disponibles de forma clara e intuitiva

  @wip
  Escenario: 01-UXUI No mostrar el ícono de audio cuando la funcionalidad de IA no está disponible
    Dado que la funcionalidad de audio para IA no está disponible
    Cuando visualizo la pantalla correspondiente
    Entonces el ícono de audio no debe mostrarse

  @wip
  Escenario: 02-UXUI Visualizar todas las categorías disponibles
    Dado que estoy en la aplicación
    Cuando accedo a la sección de categorías
    Entonces veo una pantalla con todas las categorías disponibles

  @wip
  Escenario: 03-UXUI Adjuntar imágenes a una oferta de trabajo
    Dado que estoy creando una oferta de trabajo
    Cuando selecciono una o más imágenes desde el dispositivo
    Entonces las imágenes quedan adjuntadas a la oferta

  @wip
  Escenario: 04-UXUI Visualizar las imágenes adjuntadas antes de publicar
    Dado que seleccioné una o más imágenes para mi oferta de trabajo
    Cuando continúo con la creación de la oferta
    Entonces veo una vista previa de las imágenes seleccionadas

  @wip
  Escenario: 05-UXUI Eliminar una imagen antes de publicar la oferta
    Dado que tengo una o más imágenes seleccionadas para mi oferta de trabajo
    Cuando elimino una de las imágenes
    Entonces la imagen deja de estar adjuntada a la oferta

  @wip
  Escenario: 06-UXUI Indicar nuevos mensajes en la lista de chats
    Dado que tengo un chat con mensajes nuevos sin leer
    Cuando visualizo la lista de chats
    Entonces veo una indicación visual de que el chat tiene nuevos mensajes

  @wip
  Escenario: 07-UXUI Actualizar el indicador al leer los mensajes
    Dado que tengo un chat con mensajes nuevos sin leer
    Cuando ingreso al chat
    Y visualizo los mensajes pendientes
    Entonces el indicador de nuevos mensajes deja de mostrarse para ese chat

  @wip
  Escenario: 08-UXUI Visualizar correctamente los bordes y márgenes
    Dado que navego por las distintas pantallas de la aplicación
    Cuando visualizo los componentes de la interfaz
    Entonces los bordes y márgenes se muestran correctamente
    Y ningún elemento aparece cortado
    Y ningún elemento aparece desbordado
    Y ningún elemento aparece fuera de los límites de la pantalla