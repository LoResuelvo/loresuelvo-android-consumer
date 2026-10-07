# language: es
@US-20
Característica: Recibir avisos de mensajes y novedades como consumidor
  Como consumidor
  Quiero enterarme de mis mensajes y servicios desde el teléfono
  Para atenderlos aunque no esté usando LoResuelvo

  Escenario: 01-CPN Habilitar los avisos después de verificar mi cuenta
    Dado que todavía no inicié sesión en este teléfono
    Cuando inicio sesión y LoResuelvo verifica mi cuenta de consumidor
    Entonces este teléfono queda habilitado para recibir avisos de mi cuenta

  @wip
  Esquema del escenario: 02-CPN Recibir mensajes del prestador fuera del chat visible
    Dado que tengo una sesión activa y permití los avisos en este teléfono
    Y estoy "<situacion>"
    Cuando llega un aviso de mensaje con "<contenido>" del prestador
    Entonces veo un único aviso de nuevo mensaje que permite abrir esa conversación
    Y el aviso no expone contenido del mensaje ni datos personales
    Ejemplos:
      | situacion                          | contenido   |
      | usando otra pantalla de LoResuelvo  | texto       |
      | usando otra aplicación             | fotografías |
      | con la pantalla bloqueada          | audio       |
      | sin LoResuelvo en ejecución         | video       |
      | leyendo otra conversación          | texto       |

  @wip
  Esquema del escenario: 03-CPN Recibir las novedades importantes de mis servicios
    Dado que tengo una sesión activa y permití los avisos en este teléfono
    Y no estoy usando LoResuelvo
    Cuando llega un aviso de "<novedad>" de uno de mis servicios
    Entonces veo un aviso de "<aviso>" que permite abrir "<destino>"
    Y el aviso no expone nombres, direcciones ni importes
    Ejemplos:
      | novedad                                  | aviso               | destino                  |
      | una propuesta recibida del prestador     | propuesta recibida  | la propuesta             |
      | un turno dentro de las próximas 24 horas | turno próximo       | la orden del turno       |
      | la finalización reportada por prestador  | servicio finalizado | la orden correspondiente |

  @wip
  Esquema del escenario: 04-CPN Decidir el permiso de avisos sin perder acceso a la aplicación
    Dado que uso Android 13 o posterior y no decidí el permiso de avisos
    Y solicité habilitar los avisos desde LoResuelvo
    Cuando "<decision>" el permiso de notificaciones del teléfono
    Entonces puedo seguir consultando mis mensajes y servicios
    Y no se vuelve a pedir el permiso automáticamente al abrir LoResuelvo
    Y puedo abrir los ajustes de notificaciones del teléfono desde la aplicación
    Ejemplos:
      | decision |
      | concedo  |
      | rechazo  |

  @wip
  Esquema del escenario: 05-CPN Respetar los ajustes de avisos del teléfono
    Dado que tengo una sesión activa
    Y "<ajuste>"
    Cuando llega un aviso válido de "<tipo>"
    Entonces no aparece una notificación del teléfono para ese aviso
    Y puedo consultar la novedad dentro de LoResuelvo
    Ejemplos:
      | ajuste                                  | tipo                |
      | rechacé el permiso de avisos             | nuevo mensaje       |
      | deshabilité el canal de mensajes         | nuevo mensaje       |
      | deshabilité el canal de servicios        | propuesta recibida  |
      | deshabilité las notificaciones de la app | servicio finalizado |

  @wip
  Esquema del escenario: 06-CPN Recuperar la recepción después de una interrupción
    Dado que tengo una sesión verificada de consumidor
    Y "<interrupcion>" al habilitar los avisos
    Cuando vuelvo a usar LoResuelvo con conexión disponible
    Entonces el teléfono queda habilitado para recibir próximos avisos de mi cuenta
    Y puedo consultar mis mensajes y servicios durante la recuperación
    Ejemplos:
      | interrupcion                          |
      | falla la conexión                     |
      | no llega la confirmación del registro |

  @wip
  Escenario: 07-CPN Leer el chat abierto sin un aviso adicional
    Dado que estoy leyendo mi conversación con el prestador
    Y tengo un borrador sin enviar y una posición de lectura elegida
    Cuando llega el aviso de un nuevo mensaje del prestador
    Entonces la conversación se actualiza sin una notificación del teléfono ni un popup adicional
    Y conservo mi borrador y mi posición de lectura

  @wip
  Esquema del escenario: 08-CPN Evitar avisos repetidos o fuera de tiempo
    Dado que tengo una sesión activa y permití los avisos
    Cuando llega "<aviso>"
    Entonces no aparece una nueva notificación ni vuelve a sonar una anterior
    Ejemplos:
      | aviso                                    |
      | un aviso que ya recibí                   |
      | un aviso cuyo plazo para mostrarse venció |

  @wip
  Esquema del escenario: 09-CPN Abrir el destino exacto desde un aviso vigente
    Dado que tengo un aviso vigente de "<aviso>" de mi cuenta actual
    Y LoResuelvo está "<estado>"
    Cuando toco ese aviso
    Entonces veo "<destino>" correspondiente al aviso con información actual de mi cuenta
    Y puedo volver a la aplicación sin abrir pantallas repetidas
    Ejemplos:
      | aviso               | estado  | destino                  |
      | nuevo mensaje       | cerrada | la conversación          |
      | nuevo mensaje       | abierta | la conversación          |
      | propuesta recibida  | cerrada | la propuesta             |
      | propuesta recibida  | abierta | la propuesta             |
      | turno próximo       | cerrada | la orden del turno       |
      | turno próximo       | abierta | la orden del turno       |
      | servicio finalizado | cerrada | la orden correspondiente |
      | servicio finalizado | abierta | la orden correspondiente |

  @wip
  Esquema del escenario: 10-CPN Resolver un aviso que no puedo abrir
    Dado que recibí un aviso y "<situacion>"
    Cuando toco ese aviso
    Entonces "<resultado>"
    Y no veo información privada de otra cuenta ni datos inventados
    Ejemplos:
      | situacion                        | resultado                                          |
      | perdí la conexión                | se informa el problema y puedo reintentar           |
      | el recurso ya no está disponible | se informa que no está disponible y puedo volver    |
      | ya no tengo acceso al recurso    | se informa que no tengo acceso y puedo volver       |
      | mi sesión ya no está activa      | se solicita iniciar sesión sin abrir el aviso viejo |

  @wip
  Esquema del escenario: 11-CPN Dejar de recibir avisos al cerrar sesión
    Dado que tengo avisos visibles de mi cuenta
    Y el teléfono está "<conexion>"
    Cuando confirmo el cierre de sesión
    Entonces la sesión local se cierra inmediatamente
    Y desaparecen los avisos de mi cuenta en este teléfono
    Y los avisos posteriores de esa sesión no se muestran ni abren datos privados
    Ejemplos:
      | conexion     |
      | con conexión |
      | sin conexión |

  @wip
  Escenario: 12-CPN Recibir solamente avisos de la cuenta actual después de reiniciar
    Dado que cerré sesión sin conexión e ingresé con otra cuenta de consumidor
    Y la nueva cuenta quedó habilitada para recibir avisos en este teléfono
    Y reinicié LoResuelvo
    Cuando llega un aviso pendiente de la cuenta anterior
    Entonces no se muestra ese aviso ni permite entrar a la cuenta anterior
    Y sigo pudiendo recibir avisos de mi cuenta actual
