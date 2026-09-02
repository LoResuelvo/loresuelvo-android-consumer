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
#    tests de Compose (`src/androidTest/.../instrumented/diagnosis/`),
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

  Escenario: 01-AIP Adjuntar una imagen desde la galería del dispositivo
    Cuando toco el botón de adjuntar imagen desde la galería
    Y selecciono la imagen "gotera-baño.jpg"
    Entonces la imagen queda pendiente de envío en la conversación
    Y puedo ver la vista previa de la imagen seleccionada
    Y puedo confirmar el envío o descartarla
    
  Escenario: 02-AIP Capturar una imagen con la cámara del dispositivo
    Cuando toco el botón de adjuntar imagen desde la cámara
    Y capturo la foto "fuga-cocina.jpg"
    Entonces la imagen queda pendiente de envío en la conversación
    Y puedo ver la vista previa de la foto capturada
    Y puedo confirmar el envío o descartarla
    
Escenario: 03-AIP Adjuntar múltiples imágenes en una misma consulta
    Dado que no tengo imágenes pendientes de envío
    Cuando toco el botón de adjuntar imagen desde la galería
    Y selecciono la imagen "gotera-1.jpg"
    Y selecciono la imagen "gotera-2.jpg"
    Y selecciono la imagen "gotera-3.jpg"
    Entonces tengo 3 imágenes pendientes de envío en la conversación
    Y la vista previa muestra las 3 imágenes en orden de selección

  Escenario: 04-AIP Eliminar una imagen pendiente antes del envío
    Dado que tengo las imágenes "gotera-1.jpg", "gotera-2.jpg" y "gotera-3.jpg" pendientes de envío
    Cuando elimino la imagen "gotera-2.jpg"
    Entonces tengo 2 imágenes pendientes de envío
    Y las imágenes pendientes son "gotera-1.jpg" y "gotera-3.jpg"
    Y conservan su orden original

  Escenario: 05-AIP Eliminar todas las imágenes pendientes antes del envío
    Dado que tengo 3 imágenes pendientes de envío
    Cuando elimino todas las imágenes pendientes
    Entonces no tengo imágenes pendientes de envío

  Escenario: 06-AIP Enviar imágenes adjuntas para obtener un pre diagnóstico
    Dado que tengo la imagen "gotera-baño.jpg" pendiente de envío
    Y la subida de archivos está disponible
    Y la IA acepta el mensaje con la imagen
    Cuando escribo "Tengo una gotera en el baño"
    Y presiono "Diagnosticar"
    Entonces se sube la imagen "gotera-baño.jpg"
    Y se envía el mensaje con la imagen adjunta
    Y no tengo imágenes pendientes de envío

  Escenario: 07-AIP Pre diagnóstico generado tras el envío de imágenes
    Dado que envié la imagen "gotera-baño.jpg" y la IA devolvió un pre diagnóstico
    Cuando visualizo la respuesta del asistente
    Entonces veo la explicación del problema detectado
    Y veo la categoría detectada "Plomería"

  Escenario: 08-AIP Mantener las imágenes pendientes cuando falla la subida
    Dado que tengo la imagen "gotera-baño.jpg" pendiente de envío
    Y la subida de la imagen falla por un error de red
    Cuando escribo "Tengo una gotera en el baño"
    Y presiono "Diagnosticar"
    Entonces veo un error al subir la imagen
    Y la imagen continúa pendiente de envío
    Y el mensaje no se envía

  Escenario: 09-AIP Reintentar la subida de una imagen
    Dado que la subida de "gotera-baño.jpg" falló
    Y la imagen continúa pendiente de envío
    Cuando reintento la carga de imágenes
    Entonces la imagen se sube nuevamente
    Y el mensaje se envía al completar la subida
    Y la imagen deja de estar pendiente de envío

  Escenario: 10-AIP Informar un error al procesar el pre diagnóstico
    Dado que envié la imagen "gotera-baño.jpg"
    Y el servicio de IA falla al procesar el mensaje
    Cuando visualizo el resultado del envío
    Entonces veo un error de procesamiento del pre diagnóstico
    Y la imagen enviada permanece en la conversación
    Y puedo reintentar el procesamiento

  Escenario: 11-AIP Reintentar el procesamiento del pre diagnóstico
    Dado que el procesamiento del pre diagnóstico falló
    Y la imagen "gotera-baño.jpg" ya fue enviada
    Cuando reintento el procesamiento
    Entonces se vuelve a procesar el mensaje enviado
    Y no se vuelve a subir la imagen
    Y veo una respuesta del asistente en la conversación
