# language: es

Característica: Fecha y hora en el chat con prestadores

  Como consumidor
  Quiero identificar cuándo se enviaron los mensajes
  Para seguir el contexto de la conversación

  Escenario: 01-MFH Mostrar la hora en un mensaje de texto
    Dado que la conversación contiene un mensaje de texto enviado recientemente
    Cuando se calcula la hora visible del mensaje
    Entonces se muestra la hora en formato local

  Escenario: 02-MFH Mostrar la hora en un mensaje multimedia
    Dado que la conversación contiene un mensaje multimedia enviado recientemente
    Cuando se calcula la hora visible del mensaje multimedia
    Entonces se muestra la hora en formato local
    Y la referencia multimedia se conserva en el mensaje

  Escenario: 03-MFH Separar mensajes de días diferentes
    Dado que la conversación contiene mensajes de dos días locales diferentes
    Cuando se calcula la estructura de la lista del chat
    Entonces se crea un separador para cada día
    Y el último índice apunta al último mensaje y no al separador

  Escenario: 04-MFH Etiquetar los mensajes de hoy y ayer
    Dado que la conversación contiene un mensaje de hoy y otro de ayer
    Cuando se obtienen las etiquetas de fecha del chat
    Entonces el primer mensaje se identifica como "Hoy"
    Y el segundo mensaje se identifica como "Ayer"

  Escenario: 05-MFH Localizar la fecha de un mensaje anterior
    Dado que la conversación contiene un mensaje de un día anterior a ayer
    Cuando se obtiene la etiqueta de fecha del chat
    Entonces se muestra la fecha usando el formato local del dispositivo

  Escenario: 06-MFH Mantener el auto-scroll al recibir un mensaje nuevo
    Dado que el consumidor está al final de una conversación con mensajes de varios días
    Cuando se calcula la posición de desplazamiento del último mensaje
    Entonces la posición incluye los separadores anteriores
    Y el resultado corresponde al último mensaje de la lista
