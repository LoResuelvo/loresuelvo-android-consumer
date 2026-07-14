# language: es
Característica: Autenticación y restauración de sesión del consumidor
  Como consumidor
  Quiero iniciar y cerrar sesión de forma explícita
  Para volver a mi cuenta sin repetir el onboarding

  Escenario: 01-AUT Iniciar sesión usa el flujo de login
    Dado que no tengo una sesión local
    Cuando elijo iniciar sesión
    Entonces se abre el flujo de login de Auth0
    Y no se abre el flujo de registro de Auth0

  Escenario: 02-AUT Una cuenta existente recupera su perfil
    Dado que Auth0 autentica una cuenta existente
    Y la API devuelve el perfil completo del consumidor
    Cuando finaliza la autenticación
    Entonces la sesión usa el perfil persistido por la API
    Y el consumidor puede entrar al inicio sin completar su perfil otra vez

  Escenario: 03-AUT Una cuenta nueva completa su perfil una sola vez
    Dado que Auth0 autentica una cuenta nueva
    Y la API indica que el consumidor todavía no existe
    Cuando finaliza la autenticación
    Entonces la sesión conserva la identidad de Auth0
    Y el consumidor debe completar su perfil

  Escenario: 04-AUT Cerrar sesión elimina también la sesión de Auth0
    Dado que tengo una sesión local autenticada
    Cuando cierro sesión correctamente en Auth0
    Entonces se elimina la sesión local
