# language: es

Característica: Correcciones de UX/UI

  Como consumidor
  Quiero visualizar y utilizar correctamente la aplicación
  Para poder realizar las acciones disponibles de forma clara e intuitiva

  Escenario: 01-UXUI No mostrar el ícono de audio cuando la funcionalidad de IA no está disponible
    Dado que la funcionalidad de audio para IA no está disponible
    Cuando visualizo la pantalla correspondiente
    Entonces el ícono de audio no debe mostrarse
    Y el botón de enviar mensajes se muestra deshabilitado mientras el campo esté vacío

  Escenario: 02-UXUI Visualizar todas las categorías disponibles
    Dado que estoy en la aplicación
    Cuando accedo a la sección de categorías
    Entonces veo una pantalla con todas las categorías disponibles
    Y una barra de búsqueda que me permite encontrar las distintas categorías

  Escenario: 03-UXUI Adjuntar imágenes a una oferta de trabajo
    Dado que estoy creando una oferta de trabajo
    Cuando selecciono una o más imágenes desde el dispositivo
    Entonces las imágenes quedan adjuntadas a la oferta

  Escenario: 04-UXUI Visualizar las imágenes adjuntadas antes de publicar
    Dado que seleccioné una o más imágenes para mi oferta de trabajo
    Cuando continúo con la creación de la oferta
    Entonces veo una vista previa de las imágenes seleccionadas

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

  Escenario: 08-UXUI Visualizar correctamente los bordes y márgenes
    Dado que navego por las distintas pantallas de la aplicación
    Cuando visualizo los componentes de la interfaz
    Entonces los bordes y márgenes se muestran correctamente
    Y ningún elemento aparece cortado
    Y ningún elemento aparece desbordado
    Y ningún elemento aparece fuera de los límites de la pantalla
