# language: es

@wip
Característica: Mejorar Mis servicios y Mis turnos
  Como consumidor
  quiero consultar mis servicios y turnos en pantallas claras
  para entender rápidamente el estado de mi actividad y acceder a sus acciones

  Antecedentes:
    Dado que estoy autenticado como consumidor

  @wip
  Escenario: 01-MUI Mostrar la jerarquía de Mis servicios
    Dado que tengo propuestas de servicio en distintos estados
    Cuando abro "Mis servicios"
    Entonces veo el título "Mis servicios"
    Y veo una barra de búsqueda redondeada
    Y veo un botón compacto para abrir los filtros
    Y cada propuesta muestra prestador, rubro, estado, monto y fecha
    Y cada propuesta tiene una acción principal claramente distinguible

  @wip
  Escenario: 02-MUI Buscar propuestas de servicio
    Dado que tengo propuestas de distintos prestadores y rubros
    Cuando escribo un texto en la búsqueda de "Mis servicios"
    Entonces solo veo las propuestas que coinciden por prestador, rubro o descripción
    Y puedo limpiar la búsqueda para recuperar todas las propuestas

  @wip
  Escenario: 03-MUI Filtrar Mis servicios desde el control compacto
    Dado que tengo propuestas pendientes, aceptadas y rechazadas
    Cuando abro el control de filtros y selecciono un estado
    Entonces el filtro seleccionado queda visualmente destacado
    Y solo veo propuestas del estado elegido
    Y puedo quitar el filtro para volver a ver todos los servicios

  @wip
  Escenario: 04-MUI Mostrar el estado vacío de Mis servicios
    Dado que no tengo propuestas de servicio para el filtro seleccionado
    Cuando abro "Mis servicios"
    Entonces veo un estado vacío claro y centrado
    Y el mensaje explica qué aparecerá en esta pantalla
    Y la búsqueda y los filtros siguen disponibles

  @wip
  Escenario: 05-MUI Recuperar Mis servicios ante carga o error
    Dado que Mis servicios está cargando o no pudo obtener sus datos
    Cuando visualizo la pantalla
    Entonces veo un indicador de carga o un mensaje de error comprensible
    Y cuando ocurre un error veo una acción para reintentar
    Y puedo mantener la navegación hacia otras secciones

  @wip
  Escenario: 06-MUI Mostrar la información principal de Mis turnos
    Dado que tengo turnos registrados
    Cuando abro "Mis turnos"
    Entonces veo un encabezado claro de la pantalla
    Y veo una barra de búsqueda redondeada
    Y veo un botón compacto para abrir los filtros
    Y cada turno muestra prestador, rubro, descripción, monto y fecha
    Y el estado del turno se distingue mediante una insignia legible
    Y la acción "Ver detalles" queda accesible sin competir con la información

  @wip
  Escenario: 07-MUI Buscar turnos por prestador o servicio
    Dado que tengo turnos de distintos prestadores y servicios
    Cuando escribo un texto en la búsqueda de "Mis turnos"
    Entonces solo veo los turnos que coinciden por prestador, rubro o descripción
    Y puedo limpiar la búsqueda para recuperar todos los turnos

  @wip
  Escenario: 08-MUI Filtrar Mis turnos desde el control compacto
    Dado que tengo turnos en distintos estados
    Cuando abro el control de filtros y selecciono un estado
    Entonces solo veo turnos del estado elegido
    Y puedo quitar el filtro para volver a ver todos los turnos

  @wip
  Escenario: 09-MUI Mantener estados de Mis turnos
    Dado que no tengo turnos o el backend no responde
    Cuando abro "Mis turnos"
    Entonces veo un estado vacío o un error con composición consistente
    Y el contenido no queda pegado a los bordes de la pantalla
    Y puedo reintentar cuando la carga falla
