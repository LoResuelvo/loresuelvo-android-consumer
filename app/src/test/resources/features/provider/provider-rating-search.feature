# language: es

Característica: Mostrar y buscar prestadores por calificación
  Como consumidor
  quiero conocer y ordenar la calificación de los prestadores de un rubro
  para elegir profesionales mejor valorados

  Antecedentes:
    Dado que estoy autenticado como consumidor
    Y existen los rubros "Plomería" y "Electricidad"
    Y estoy viendo los prestadores del rubro "Plomería"

  Escenario: 01-RP Mostrar rating en la tarjeta del prestador
    Dado que "Juan Pérez" tiene una calificación promedio de 4,8 con 12 reseñas
    Cuando visualizo el listado de prestadores
    Entonces la tarjeta de "Juan Pérez" muestra 4,8 estrellas
    Y muestra que tiene 12 reseñas

  Escenario: 02-RP Mostrar prestador sin calificaciones
    Dado que "Pedro Díaz" no tiene calificaciones recibidas
    Cuando visualizo el listado de prestadores
    Entonces la tarjeta de "Pedro Díaz" muestra "Sin calificaciones"
    Y el prestador continúa disponible en el listado

  Escenario: 03-RP Ordenar prestadores por mejor calificación
    Dado que "Juan Pérez" tiene una calificación promedio de 4,8 con 12 reseñas
    Y "Pedro Díaz" tiene una calificación promedio de 3,9 con 5 reseñas
    Y "Ana Gómez" tiene una calificación promedio de 4,5 con 8 reseñas
    Cuando selecciono ordenar por "Mejor calificación"
    Entonces veo primero a "Juan Pérez"
    Y luego veo a "Ana Gómez"
    Y luego veo a "Pedro Díaz"

  Escenario: 04-RP Filtrar prestadores por calificación mínima
    Dado que "Juan Pérez" tiene una calificación promedio de 4,8 con 12 reseñas
    Y "Pedro Díaz" tiene una calificación promedio de 3,9 con 5 reseñas
    Y "Ana Gómez" tiene una calificación promedio de 4,5 con 8 reseñas
    Cuando filtro por una calificación mínima de 4 estrellas
    Entonces veo a "Juan Pérez" en el listado
    Y veo a "Ana Gómez" en el listado
    Y no veo a "Pedro Díaz" en el listado

  Escenario: 05-RP Aplicar la búsqueda de rating solo al rubro seleccionado
    Dado que "Laura Gómez" de "Electricidad" tiene una calificación promedio de 5,0 con 20 reseñas
    Y "Juan Pérez" de "Plomería" tiene una calificación promedio de 4,8 con 12 reseñas
    Cuando filtro por una calificación mínima de 4 estrellas
    Entonces solo veo prestadores de "Plomería"
    Y no veo a "Laura Gómez" en el listado

  Escenario: 06-RP Quitar el filtro de rating
    Dado que hay prestadores con distintas calificaciones en el rubro "Plomería"
    Y tengo seleccionado ordenar por "Mejor calificación"
    Y tengo aplicado un filtro de calificación mínima de 4 estrellas
    Cuando quito el filtro de rating
    Entonces vuelvo a ver todos los prestadores del rubro "Plomería"
    Y se conserva el orden seleccionado previamente

  Escenario: 07-RP Informar que no hay resultados para el rating elegido
    Dado que ningún prestador del rubro "Plomería" tiene una calificación de al menos 5,0 estrellas
    Cuando filtro por una calificación mínima de 5,0 estrellas
    Entonces veo un estado vacío específico para el filtro de rating
    Y puedo quitar el filtro para volver a ver los prestadores
