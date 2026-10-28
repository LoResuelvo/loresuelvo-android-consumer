# language: es

@wip
Característica: Mejorar cámara, multimedia, pagos y ratings de prestadores

  Como consumidor
  Quiero capturar contenido, consultar multimedia y ver información confiable
  Para completar mi perfil y comunicarme con prestadores de forma clara

  Antecedentes:
    Dado que estoy autenticado como consumidor

  @wip
  Escenario: 01-CMPR Capturar una foto desde Completar perfil
    Dado que estoy completando mi perfil
    Cuando elijo tomar una foto con la cámara
    Y capturo una imagen válida
    Entonces veo la imagen en el recorte circular del perfil
    Y puedo confirmar o descartar la imagen

  @wip
  Escenario: 02-CMPR Solicitar permiso de cámara desde Completar perfil
    Dado que el permiso de cámara no fue concedido
    Cuando elijo tomar una foto desde Completar perfil
    Entonces la aplicación solicita permiso para usar la cámara
    Y no abre la cámara antes de resolver el permiso

  @wip
  Escenario: 03-CMPR Continuar después de denegar el permiso de cámara
    Dado que la aplicación solicitó permiso para usar la cámara
    Cuando rechazo el permiso
    Entonces veo un mensaje explicando que el permiso es necesario para tomar fotos
    Y puedo continuar usando la galería
    Y puedo volver a intentar la captura desde la cámara

  @wip
  Escenario: 04-CMPR Reutilizar el permiso de cámara desde otro flujo
    Dado que estoy en una pantalla que permite capturar una imagen
    Y el permiso de cámara no fue concedido
    Cuando elijo abrir la cámara
    Entonces la aplicación utiliza el mismo flujo de solicitud de permiso
    Y después de concederlo abre la cámara

  @wip
  Escenario: 05-CMPR Previsualizar una imagen o video adjunto
    Dado que estoy en una conversación abierta con el prestador "Juan Pérez"
    Cuando selecciono una imagen o un video válido
    Entonces veo una preview clara del archivo seleccionado
    Y puedo confirmar el envío o descartarlo

  @wip
  Escenario: 06-CMPR Mostrar un estado recuperable al fallar la preview multimedia
    Dado que estoy en una conversación abierta con el prestador "Juan Pérez"
    Cuando selecciono un archivo multimedia ilegible
    Entonces veo un mensaje de error específico
    Y el archivo no queda adjuntado
    Y puedo continuar enviando mensajes o seleccionar otro archivo

  @wip
  Escenario: 07-CMPR Grabar un audio sin preview visual innecesaria
    Dado que estoy en una conversación abierta con el prestador "Juan Pérez"
    Cuando grabo un audio válido
    Entonces veo el control de audio listo para confirmar el envío
    Y no veo una preview de imagen o video para el audio
    Y puedo descartar o enviar el audio

  @wip
  Escenario: 08-CMPR Reproducir un audio con controles del chat
    Dado que la conversación contiene un audio enviado o recibido
    Cuando toco el control de reproducción del audio
    Entonces el audio comienza a reproducirse
    Y veo su duración y progreso
    Y puedo pausar y continuar la reproducción

  @wip
  Escenario: 09-CMPR Mostrar Pagar seña para un depósito pendiente
    Dado que tengo un servicio con una seña pendiente de pago
    Cuando visualizo el detalle del servicio
    Entonces veo la acción "Pagar seña"
    Y no veo la acción "Pagar ahora"

  @wip
  Escenario: 10-CMPR Colorear correctamente las estrellas del prestador
    Dado que "Juan Pérez" tiene una calificación promedio de 4,8 con 12 reseñas
    Cuando visualizo una tarjeta o perfil de "Juan Pérez"
    Entonces veo el puntaje 4,8
    Y las estrellas representan el puntaje real
    Y no se muestran cinco estrellas completas por defecto

  @wip
  Escenario: 11-CMPR Mostrar el estado de un prestador sin reseñas
    Dado que "Pedro Díaz" no tiene reseñas
    Cuando visualizo una tarjeta o perfil de "Pedro Díaz"
    Entonces veo el estado "Sin calificaciones"
    Y no se muestran cinco estrellas completas como si tuviera puntaje

  @wip
  Escenario: 12-CMPR Mostrar el rating del prestador recomendado por la IA
    Dado que la IA recomienda a "Juan Pérez" con una calificación promedio de 4,8
    Cuando visualizo la recomendación en el chat con la IA
    Entonces veo el nombre de "Juan Pérez"
    Y veo su puntaje y sus estrellas

  @wip
  Escenario: 13-CMPR Mantener un estado neutro si la recomendación no tiene rating
    Dado que la IA recomienda a "Pedro Díaz" sin información de reseñas
    Cuando visualizo la recomendación en el chat con la IA
    Entonces veo la recomendación de "Pedro Díaz"
    Y no se muestran estrellas completas engañosas
    Y veo un estado neutro para la calificación

  @wip
  Escenario: 14-CMPR Respetar la orientación de una foto al completar el perfil
    Dado que estoy completando mi perfil
    Y capturé una foto cuya orientación está registrada por la cámara
    Cuando abro el recorte de la foto
    Entonces veo la foto con la orientación correcta
    Y la orientación se conserva al confirmar el recorte

  @wip
  Escenario: 15-CMPR Girar la foto antes de confirmar el recorte
    Dado que estoy ajustando una foto de perfil
    Cuando toco "Girar 90°"
    Entonces la foto gira un cuarto de vuelta
    Y puedo volver a ajustar el encuadre circular
    Y la orientación elegida se conserva al confirmar

  @wip
  Escenario: 16-CMPR Cargar una foto grande sin zonas vacías
    Dado que seleccioné una foto de cámara de alta resolución
    Cuando abro el recorte de la foto
    Entonces se decodifica una versión adecuada para el editor
    Y todas las zonas de la foto están disponibles para elegir el encuadre
