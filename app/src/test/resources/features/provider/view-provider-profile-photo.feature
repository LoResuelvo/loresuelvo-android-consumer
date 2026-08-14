# language: es
#
# Especificación ejecutable para la historia de usuario "Ver la foto
# de perfil del prestador". Cubre los tres lugares donde el
# consumidor ve la foto de perfil del prestador con el que
# interactúa: la lista de prestadores (filtro por rubro), el header
# del chat dentro de una conversación, y la ventana de chats que
# lista todas las conversaciones activas.
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

Característica: Ver la foto de perfil del prestador

  Como consumidor
  Quiero ver la foto de perfil del prestador con el que interactúo
  Para saber con quién estoy hablando

  Antecedentes:
    Dado estoy autenticado como consumidor
    Y existen las siguientes categorías:
      | id | name         |
      | 1  | Plomería     |
      | 2  | Electricidad |
    Y existen los siguientes prestadores:
      | id       | name  | surname | category_name | category_id | profile_photo_url                  |
      | prov-001 | Juan  | Pérez   | Plomería      | 1           | https://cdn.loresuelvo.test/jp.jpg |
      | prov-002 | Laura | Gómez   | Electricidad  | 2           | https://cdn.loresuelvo.test/lg.jpg |

  Escenario: 01-VFP La lista de prestadores expone la foto de perfil al filtrar por rubro
    Dado estoy en la pantalla Home del consumidor
    Cuando toco la tarjeta de la categoría "Plomería"
    Entonces llego a la lista de prestadores del rubro "Plomería"
    Y la tarjeta del prestador "Juan Pérez" expone la foto de perfil "https://cdn.loresuelvo.test/jp.jpg"

@wip
Escenario: 02-VFP El header del chat muestra la foto de perfil del prestador con el que converso
  Dado ya tengo una conversación con el prestador "Juan Pérez"
  Cuando accedo a la sección de chats
  Y abro la conversación con "Juan Pérez"
  Entonces el header del chat muestra la foto de perfil "https://cdn.loresuelvo.test/jp.jpg" del prestador "Juan Pérez"

@wip
Escenario: 03-VFP La ventana de chats muestra la foto de perfil de cada prestador con quien tengo conversación
  Dado ya tengo una conversación con el prestador "Juan Pérez"
  Y ya tengo una conversación con el prestador "Laura Gómez"
  Cuando accedo a la sección de chats
  Entonces veo al prestador "Juan Pérez" con la foto de perfil "https://cdn.loresuelvo.test/jp.jpg" en mi lista de chats
  Y veo al prestador "Laura Gómez" con la foto de perfil "https://cdn.loresuelvo.test/lg.jpg" en mi lista de chats