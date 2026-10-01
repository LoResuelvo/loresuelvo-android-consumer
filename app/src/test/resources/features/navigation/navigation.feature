# language: es

Característica: Composición de navegación del consumidor

  Como consumidor autenticado
  Quiero que la navegación se componga por funcionalidades
  Para que cada feature mantenga sus efectos y launchers aislados

  @wip
  Escenario: El retorno de pago conserva su ruta y oculta la barra inferior
    Dado que la aplicación inicia en Home para un consumidor autenticado
    Cuando se procesa un deep link de retorno de pago con referencia "pay-123"
    Entonces se muestra la ruta interna de resultado de pago
    Y la barra inferior permanece oculta en esa ruta

  @wip
  Escenario: El chat conserva sus launchers al cambiar la composición
    Dado que el consumidor abre una conversación existente
    Cuando vuelve a componerse la ruta de conversación
    Entonces los launchers de galería, cámara y permiso de audio siguen asociados al chat
    Y el estado de la conversación no se reinicia por una recomposición
