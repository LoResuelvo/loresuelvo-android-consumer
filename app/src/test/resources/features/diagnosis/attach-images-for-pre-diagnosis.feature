# language: es
#
# Especificación ejecutable para la historia de usuario "Adjuntar
# imágenes del problema para obtener un pre diagnóstico". Cubre
# los criterios de aceptación:
#   - adjuntar imágenes desde la galería y la cámara del dispositivo,
#   - visualizar las imágenes seleccionadas antes de enviarlas,
#   - eliminar imágenes adjuntas antes del envío (individuales y en masa),
#   - procesar las imágenes adjuntas para generar un pre diagnóstico
#     (assessment + categoría detectada + nivel de confianza),
#   - informar errores de carga (subida a `/files/presign` →
#     PUT al storage → `POST /files/{id}/confirm`) y errores de
#     procesamiento del servicio de IA, con reintento explícito.
#
# Las imágenes viajan como mensajes de la conversación IA
# (`POST /chatbot/conversations/{id}/messages` con `image_file_ids[]`),
# reusando la infraestructura de `FileRepository` (presign → upload →
# confirm) ya implementada para el chat con prestadores. La superficie
# es `ChatViewModel` del flujo de diagnóstico con IA: este feature
# añade el estado `pendingAttachments`, el orquestador
# `UploadAttachmentsAndSendUseCase` y los callbacks `onAttachImage*`,
# `onRemoveAttachment`, `onClearAttachments`, `onRetryAttachClick`.
#
# Cada escenario arranca marcado como `@wip` (salteado). Cada commit
# remueve el `@wip` de exactamente un escenario, deja sus aserciones
# en verde y conserva el resto en `@wip`. Cuando se remueve el
# último `@wip`, la feature está completa. El runner de Cucumber JVM
# filtra `@wip` mediante la system property
# `cucumber.filter.tags` configurada en `app/build.gradle.kts`.
#
# Convenciones:
#  - IDs `NN-AIP` correlativos. AIP = Adjuntar Imágenes
#    Pre-diagnóstico.
#  - Los textos visibles en español se asertan en los acceptance
#    tests de Compose (`src/androidTest/.../acceptance/diagnosis/`),
#    no aquí. La capa BDD pinea el contrato del state machine.
#  - BDD+TDD outside-in: este `.feature` precede al
#    `*ChatViewModelTest` unitario, que precede a la impl. Cada commit
#    sigue RED → GREEN → REFACTOR estricto.
#  - Comandos de validación por commit:
#      ./gradlew :app:testDevDebugUnitTest --tests "*AiDiagnosis*"
#      ./gradlew :app:testDevDebugUnitTest --tests "*PreDiagnosisImages*"
#    Antes de PR: `make lint && make test && make build`.
#
# Actualizar este archivo junto con `strings.xml` y las pantallas
# cada vez que cambia el copy o el comportamiento visible.

Característica: Adjuntar imágenes del problema al chat con IA

  Como consumidor
  Quiero adjuntar imágenes del problema al chat con el asistente
  Para obtener un pre diagnóstico más preciso y facilitar la evaluación del profesional

  Antecedentes:
    Dado estoy autenticado como consumidor
    Y me encuentro en la pantalla de conversación con el asistente
    Y el campo de mensaje está vacío
  @wip 
  Escenario: 01-AIP Adjuntar una imagen desde la galería del dispositivo
    Cuando toco el botón de adjuntar imagen desde la galería
    Y selecciono la imagen "gotera-baño.jpg"
    Entonces la imagen queda pendiente de envío en la conversación
    Y puedo ver la vista previa de la imagen seleccionada
    Y puedo confirmar el envío o descartarla
  @wip 
  Escenario: 02-AIP Capturar una imagen con la cámara del dispositivo
    Cuando toco el botón de adjuntar imagen desde la cámara
    Y capturo la foto "fuga-cocina.jpg"
    Entonces la imagen queda pendiente de envío en la conversación
    Y puedo ver la vista previa de la foto capturada
    Y puedo confirmar el envío o descartarla
  @wip 
  Escenario: 03-AIP Adjuntar múltiples imágenes en una misma consulta
    Dado que no tengo imágenes pendientes de envío
    Cuando toco el botón de adjuntar imagen desde la galería
    Y selecciono la imagen "gotera-1.jpg"
    Y selecciono la imagen "gotera-2.jpg"
    Y selecciono la imagen "gotera-3.jpg"
    Entonces tengo 3 imágenes pendientes de envío en la conversación
    Y la vista previa muestra las 3 imágenes en orden de selección
  @wip 
  Escenario: 04-AIP Eliminar una imagen adjunta antes del envío
    Dado que tengo 3 imágenes pendientes de envío en la conversación
    Cuando descarto la segunda imagen pendiente
    Entonces me quedan 2 imágenes pendientes de envío en la conversación
    Y las imágenes restantes conservan su orden original
  @wip 
  Escenario: 05-AIP Cancelar todas las imágenes pendientes de envío
    Dado que tengo 3 imágenes pendientes de envío en la conversación
    Cuando cancelo todas las imágenes adjuntas
    Entonces no tengo imágenes pendientes de envío en la conversación
    Y el campo de mensaje sigue vacío
  @wip 
  Escenario: 06-AIP Enviar las imágenes para obtener un pre diagnóstico
    Dado que tengo la imagen "gotera-baño.jpg" pendiente de envío
    Y el backend acepta el flujo presign → upload → confirm con éxito
    Y el backend acepta POST /chatbot/conversaciones/{convId}/messages con 200
    Y la IA devuelve un assessment con categoría "Plomería"
    Cuando escribo el mensaje "Tengo una gotera en el baño" en el campo de diagnóstico
    Y presiono "Diagnosticar"
    Entonces el backend recibió presign + upload + confirm con la imagen "gotera-baño.jpg"
    Y el backend recibió POST /chatbot/conversaciones/{convId}/messages con image_file_ids no vacío
    Y la conversación muestra la burbuja con mi mensaje de texto
    Y las imágenes adjuntas ya no quedan pendientes de envío
  @wip 
  Escenario: 07-AIP Pre diagnóstico generado tras el envío de imágenes
    Dado que envié la imagen "gotera-baño.jpg" y la IA devolvió un pre diagnóstico
    Cuando visualizo la respuesta del asistente
    Entonces veo la explicación del problema detectado por la IA
    Y veo la categoría detectada "Plomería"
    Y veo el nivel de confianza del pre diagnóstico
  @wip 
  Escenario: 08-AIP Error de red al subir una imagen
    Dado que el backend no responde
    Y tengo la imagen "gotera-baño.jpg" pendiente de envío
    Cuando escribo el mensaje "Tengo una gotera en el baño" en el campo de diagnóstico
    Y presiono "Diagnosticar"
    Entonces veo un error de carga de la imagen
    Y la imagen continúa pendiente de envío para poder reintentar
    Y no se envía el mensaje al backend
  @wip 
  Escenario: 09-AIP Reintentar la subida de imágenes tras una falla de red
    Dado que la subida de la imagen "gotera-baño.jpg" falló por error de red
    Cuando toco "Reintentar la carga de imágenes"
    Entonces se reintenta el flujo presign → upload → confirm con la imagen pendiente
    Y el mensaje se envía al backend al completarse la subida exitosamente
  @wip 
  Escenario: 10-AIP Error del servicio de IA al procesar las imágenes
    Dado que tengo la imagen "gotera-baño.jpg" pendiente de envío
    Y el backend acepta la subida de archivos con éxito
    Y el servicio de IA falla al procesar el mensaje con las imágenes
    Cuando escribo el mensaje "Tengo una gotera en el baño" en el campo de diagnóstico
    Y presiono "Diagnosticar"
    Entonces veo un error de procesamiento del pre diagnóstico
    Y la burbuja con las imágenes enviadas se conserva en la conversación
    Y puedo reintentar el procesamiento del pre diagnóstico
  @wip 
  Escenario: 11-AIP Reintentar el procesamiento del pre diagnóstico tras una falla del servicio de IA
    Dado que el procesamiento del pre diagnóstico falló por un error del servicio de IA
    Cuando toco "Reintentar el procesamiento"
    Entonces se reenvía el mensaje con las mismas imágenes adjuntas al servicio de IA
    Y veo una respuesta del asistente en la conversación
