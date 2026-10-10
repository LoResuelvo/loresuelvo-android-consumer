# language: es

@wip
Característica: Actualizar Home y Mensajes del consumidor
  Como consumidor
  quiero ver información reciente en Home y Mensajes
  para continuar mi actividad sin cerrar y volver a abrir la aplicación

  Antecedentes:
    Dado que estoy autenticado como consumidor

  @wip
  Escenario: 01-HM Mostrar hasta tres sesiones recientes de IA en Home
    Dado que tengo cuatro sesiones con la IA ordenadas por "updated_on"
    Cuando abro la pantalla Home
    Entonces la sección "Diagnósticos recientes" muestra como máximo tres sesiones
    Y las sesiones se muestran desde la más reciente hasta la más antigua
    Y cada sesión muestra su título, preview del último mensaje y fecha

  @wip
  Escenario: 02-HM Acceder a todas las sesiones de IA desde Home
    Dado que tengo sesiones previas con la IA
    Cuando toco "Ver todas las sesiones" en "Diagnósticos recientes"
    Entonces la aplicación navega a la pantalla "Asistente IA"
    Y puedo consultar la lista completa de mis sesiones

  @wip
  Escenario: 03-HM Mantener el estado vacío de diagnósticos recientes
    Dado que no tengo sesiones previas con la IA
    Cuando abro la pantalla Home
    Entonces veo el estado vacío de "Diagnósticos recientes"
    Y puedo iniciar un nuevo diagnóstico con la IA

  @wip
  Escenario: 04-HM Actualizar Home al regresar de otra pantalla
    Dado que Home cargó una lista inicial de turnos y servicios
    Y existe un turno o servicio nuevo en el backend
    Cuando regreso a Home desde "Mis turnos" o "Mis servicios"
    Entonces Home vuelve a consultar los datos visibles
    Y muestra el turno o servicio nuevo sin reiniciar la aplicación

  @wip
  Escenario: 05-HM No reemplazar datos nuevos con una respuesta antigua
    Dado que Home inició dos cargas consecutivas de sus datos
    Cuando la respuesta antigua llega después de la respuesta más reciente
    Entonces Home conserva los datos de la respuesta más reciente
    Y no vuelve a mostrar información obsoleta

  @wip
  Escenario: 06-HM Aislar el error de una sección de Home
    Dado que Home carga correctamente categorías y sesiones de IA
    Y falla la carga de turnos o servicios
    Cuando visualizo Home
    Entonces continúo viendo las secciones que cargaron correctamente
    Y la sección fallida muestra su estado de error y una acción para reintentar

  @wip
  Escenario: 07-HM Mostrar la búsqueda de Mensajes
    Dado que tengo conversaciones con distintos prestadores
    Cuando abro la pantalla "Mis mensajes"
    Entonces veo una barra de búsqueda en la parte superior
    Y la barra permite buscar por nombre del prestador o contenido visible del último mensaje

  @wip
  Escenario: 08-HM Filtrar conversaciones desde la búsqueda de Mensajes
    Dado que tengo conversaciones con "Juan Pérez" y "Ana Gómez"
    Cuando escribo "Juan" en la barra de búsqueda
    Entonces solo veo la conversación con "Juan Pérez"
    Y puedo limpiar la búsqueda para volver a ver todas las conversaciones

  @wip
  Escenario: 09-HM Informar que la búsqueda no tiene resultados
    Dado que tengo conversaciones con prestadores
    Cuando busco un nombre que no coincide con ninguna conversación
    Entonces veo un estado vacío específico para la búsqueda
    Y puedo limpiar el texto para recuperar la lista completa

  @wip
  Escenario: 10-HM Separar visualmente las conversaciones de Mensajes
    Dado que tengo al menos dos conversaciones con prestadores
    Cuando visualizo la lista de "Mis mensajes"
    Entonces cada conversación aparece en una fila independiente
    Y se muestra una línea divisoria entre una conversación y la siguiente
    Y la línea no se muestra después de la última conversación

  @wip
  Escenario: 11-HM Actualizar Mensajes al volver a la sección
    Dado que estoy viendo la lista de "Mis mensajes"
    Y llega una conversación nueva o se actualiza el último mensaje de una existente
    Cuando vuelvo a la sección "Mis mensajes"
    Entonces la lista refleja la conversación nueva o su preview actualizado
    Y conserva el orden definido por la fecha de actualización

  @wip
  Escenario: 12-HM Mantener navegación y estados de Mensajes
    Dado que Mensajes está cargando, vacío o muestra un error recuperable
    Cuando navego entre Home, Mensajes y una conversación
    Entonces cada pantalla conserva su estado de loading, vacío o error correspondiente
    Y puedo reintentar sin quedar bloqueado en la navegación
