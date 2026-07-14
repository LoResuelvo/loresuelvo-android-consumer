package com.loresuelvo.consumer.bdd.authentication

import io.cucumber.java.After
import io.cucumber.java.es.Dado
import io.cucumber.java.es.Entonces
import io.cucumber.java.es.Cuando
import io.cucumber.java.es.Y
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

class AuthenticationSessionSteps {

    private val world = AuthenticationSessionWorld()

    @After
    fun teardown() = world.close()

    @Dado("que no tengo una sesión local")
    fun no_local_session() = world.seedNoSession()

    @Cuando("elijo iniciar sesión")
    fun choose_login() = world.chooseLogin()

    @Entonces("se abre el flujo de login de Auth0")
    fun auth0_login_opens() = assertEquals(1, world.loginCalls())

    @Y("no se abre el flujo de registro de Auth0")
    fun signup_does_not_open() = assertEquals(0, world.signupCalls())

    @Dado("que Auth0 autentica una cuenta existente")
    fun existing_account() = world.configureExistingAccount()

    @Dado("que Auth0 autentica una cuenta nueva")
    fun new_account() = world.configureNewAccount()

    @Y("la API devuelve el perfil completo del consumidor")
    fun complete_backend_profile() = world.configureCompleteBackendProfile()

    @Y("la API indica que el consumidor todavía no existe")
    fun missing_backend_profile() = world.configureMissingBackendProfile()

    @Cuando("finaliza la autenticación")
    fun authentication_finishes() = world.finishAuthentication()

    @Entonces("la sesión usa el perfil persistido por la API")
    fun session_uses_backend_profile() {
        assertEquals("Ana", world.session()?.user?.firstName)
        assertEquals("Perez", world.session()?.user?.lastName)
    }

    @Y("el consumidor puede entrar al inicio sin completar su perfil otra vez")
    fun consumer_can_enter_home() =
        assertTrue(world.session()?.user?.isProfileComplete() == true)

    @Entonces("la sesión conserva la identidad de Auth0")
    fun session_keeps_auth0_identity() =
        assertEquals("ana@example.com", world.session()?.user?.email)

    @Y("el consumidor debe completar su perfil")
    fun consumer_needs_profile() =
        assertFalse(world.session()?.user?.isProfileComplete() == true)

    @Dado("que tengo una sesión local autenticada")
    fun authenticated_local_session() = world.seedAuthenticatedSession()

    @Cuando("cierro sesión correctamente en Auth0")
    fun logout_successfully() = world.logoutSuccessfully()

    @Entonces("se elimina la sesión local")
    fun local_session_is_removed() = assertNull(world.session())
}
