package com.loresuelvo.consumer.acceptance.auth

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loresuelvo.consumer.MainActivity
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@Ignore("Peding")
class CompleteProfileAcceptanceTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // Scenario: 01-CPC Mostrar formulario de completar perfil
    @Test
    fun displays_complete_profile_form() {
        // Given que estoy autenticado
        // And mi perfil está incompleto
        // When ingreso a la aplicación
        // Then veo la pantalla "Completar perfil"
        // And veo el campo "Nombre"
        // And veo el campo "Apellido"
        // And veo el botón "Continuar"
    }

    // Scenario: 02-CPC Completar perfil exitosamente
    @Test
    fun completes_profile_successfully() {
        // Given que estoy autenticado
        // And mi perfil está incompleto
        // When ingreso "Andres" como nombre
        // And ingreso "Colina" como apellido
        // And presiono "Continuar"
        // Then mi perfil queda completo
        // And veo la pantalla principal
    }

    // Scenario: 03-CPC Nombre obligatorio
    @Test
    fun requires_first_name() {
        // Given que estoy autenticado
        // And mi perfil está incompleto
        // When dejo vacío el campo "Nombre"
        // And ingreso "Colina" como apellido
        // And presiono "Continuar"
        // Then veo el mensaje "El nombre es obligatorio"
    }

    // Scenario: 04-CPC Apellido obligatorio
    @Test
    fun requires_last_name() {
        // Given que estoy autenticado
        // And mi perfil está incompleto
        // When ingreso "Andres" como nombre
        // And dejo vacío el campo "Apellido"
        // And presiono "Continuar"
        // Then veo el mensaje "El apellido es obligatorio"
    }

    // Scenario: 05-CPC Persistir perfil completado
    @Test
    fun keeps_completed_profile_after_reopening_app() {
        // Given que completé mi perfil correctamente
        // When vuelvo a abrir la aplicación
        // Then no veo la pantalla "Completar perfil"
        // And veo la pantalla principal
    }
}