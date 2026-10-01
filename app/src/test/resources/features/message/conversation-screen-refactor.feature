# language: es
#
# Especificación ejecutable de la modularización de ConversationScreen
# (US-48). El primer commit la publica como @wip; se quita la marca
# cuando la composición con contratos agrupados queda validada.

Característica: Composición modular de la conversación

  Como consumidor
  Quiero que la pantalla de conversación conserve sus acciones al modularizarse
  Para poder incorporar nuevas acciones de media sin ampliar su firma

  Escenario: La conversación conserva las acciones existentes al recomponerse
    Dado que el consumidor está viendo una conversación existente
    Cuando la pantalla se recompone con acciones agrupadas
    Entonces puede escribir y enviar texto
    Y puede reproducir audio, abrir imágenes y reintentar un envío
    Y puede navegar al detalle de la orden sin perder el estado del chat

  Escenario: El contrato de media admite una acción de video sin nuevos parámetros
    Dado que el consumidor está viendo una conversación existente
    Cuando se incorpora una acción de video al contrato de media
    Entonces la firma pública de la pantalla no agrega callbacks individuales
    Y la lista, el composer y los overlays mantienen interfaces pequeñas
