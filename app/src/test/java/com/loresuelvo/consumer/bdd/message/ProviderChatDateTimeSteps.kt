package com.loresuelvo.consumer.bdd.message

import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.MediaReference
import com.loresuelvo.consumer.ui.screens.chat.conversationLastMessageListIndex
import com.loresuelvo.consumer.ui.screens.chat.shouldShowConversationDateSeparator
import com.loresuelvo.consumer.ui.screens.chat.components.formatConversationMessageTime
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class ProviderChatDateTimeSteps {

    private lateinit var messages: List<ConversationMessage>
    private var visibleTimes = emptyList<String>()
    private var dateLabels = emptyList<String>()
    private var lastMessageIndex = -1

    @Given("que la conversación contiene un mensaje de texto enviado recientemente")
    fun conversationContainsRecentTextMessage() {
        messages = listOf(message(createdOnEpochMillis = recentTimestamp()))
    }

    @When("se calcula la hora visible del mensaje")
    fun calculateTextMessageTime() {
        visibleTimes = messages.map { formatConversationMessageTime(it.createdOnEpochMillis) }
    }

    @Then("se muestra la hora en formato local")
    fun localTimeIsVisible() {
        assertEquals(1, visibleTimes.size)
        assertEquals(
            DateFormat.getTimeInstance(
                DateFormat.SHORT,
                Locale.getDefault(),
            ).format(Date(messages.single().createdOnEpochMillis)),
            visibleTimes.single(),
        )
        assertTrue(visibleTimes.single().isNotBlank())
    }

    @Given("que la conversación contiene un mensaje multimedia enviado recientemente")
    fun conversationContainsRecentMultimediaMessage() {
        messages = listOf(
            message(
                createdOnEpochMillis = recentTimestamp(),
                media = MediaReference.Video(
                    id = "video-1",
                    url = "https://cdn.loresuelvo.test/video.mp4",
                    mimeType = "video/mp4",
                    originalName = "evidence.mp4",
                    durationMillis = 20_000L,
                    width = 1280,
                    height = 720,
                    videoCodec = "h264",
                    audioCodec = "aac",
                ),
            ),
        )
    }

    @When("se calcula la hora visible del mensaje multimedia")
    fun calculateMultimediaMessageTime() {
        visibleTimes = messages.map { formatConversationMessageTime(it.createdOnEpochMillis) }
    }

    @Then("la referencia multimedia se conserva en el mensaje")
    fun multimediaReferenceIsPreserved() {
        assertTrue(messages.single().media is MediaReference.Video)
    }

    @Given("que la conversación contiene mensajes de dos días locales diferentes")
    fun conversationContainsMessagesFromDifferentDays() {
        messages = listOf(
            message("yesterday", yesterdayTimestamp()),
            message("today", todayTimestamp()),
        )
    }

    @When("se calcula la estructura de la lista del chat")
    fun calculateChatListStructure() {
        lastMessageIndex = conversationLastMessageListIndex(messages)
    }

    @Then("se crea un separador para cada día")
    fun oneSeparatorIsCreatedPerDay() {
        val separatorCount = messages.withIndex().count { (index, current) ->
            shouldShowConversationDateSeparator(messages.getOrNull(index - 1), current)
        }
        assertEquals(2, separatorCount)
    }

    @Then("el último índice apunta al último mensaje y no al separador")
    fun lastIndexPointsToLastMessage() {
        assertEquals(3, lastMessageIndex)
        assertTrue(lastMessageIndex > messages.lastIndex)
    }

    @Given("que la conversación contiene un mensaje de hoy y otro de ayer")
    fun conversationContainsTodayAndYesterdayMessages() {
        messages = listOf(
            message("today", todayTimestamp()),
            message("yesterday", yesterdayTimestamp()),
        )
    }

    @When("se obtienen las etiquetas de fecha del chat")
    fun getChatDateLabels() {
        dateLabels = messages.map { dateLabel(it.createdOnEpochMillis) }
    }

    @Then("el primer mensaje se identifica como {string}")
    fun firstMessageHasDateLabel(expected: String) {
        assertEquals(expected, dateLabels.first())
    }

    @Then("el segundo mensaje se identifica como {string}")
    fun secondMessageHasDateLabel(expected: String) {
        assertEquals(expected, dateLabels[1])
    }

    @Given("que la conversación contiene un mensaje de un día anterior a ayer")
    fun conversationContainsOlderMessage() {
        messages = listOf(message("older", olderTimestamp()))
    }

    @When("se obtiene la etiqueta de fecha del chat")
    fun getOlderDateLabel() {
        dateLabels = messages.map { dateLabel(it.createdOnEpochMillis) }
    }

    @Then("se muestra la fecha usando el formato local del dispositivo")
    fun olderDateUsesLocalFormat() {
        assertEquals(
            DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())
                .format(Date(messages.single().createdOnEpochMillis)),
            dateLabels.single(),
        )
    }

    @Given("que el consumidor está al final de una conversación con mensajes de varios días")
    fun consumerIsAtConversationEnd() {
        messages = listOf(
            message("older", olderTimestamp()),
            message("yesterday", yesterdayTimestamp()),
            message("today", todayTimestamp()),
        )
    }

    @When("se calcula la posición de desplazamiento del último mensaje")
    fun calculateLastMessageScrollPosition() {
        lastMessageIndex = conversationLastMessageListIndex(messages)
    }

    @Then("la posición incluye los separadores anteriores")
    fun scrollPositionIncludesDateSeparators() {
        assertEquals(5, lastMessageIndex)
    }

    @Then("el resultado corresponde al último mensaje de la lista")
    fun scrollPositionTargetsLastMessage() {
        assertTrue(lastMessageIndex >= messages.lastIndex)
    }

    private fun message(
        id: String = "message-1",
        createdOnEpochMillis: Long,
        media: MediaReference? = null,
    ) = ConversationMessage(
        id = id,
        sender = ConversationSender.Consumer,
        content = "Mensaje de prueba",
        createdOnEpochMillis = createdOnEpochMillis,
        media = media,
    )

    private fun recentTimestamp(): Long = todayTimestamp()

    private fun todayTimestamp(): Long = Calendar.getInstance().timeInMillis

    private fun yesterdayTimestamp(): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis

    private fun olderTimestamp(): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -3)
    }.timeInMillis

    private fun dateLabel(epochMillis: Long): String {
        return when {
            isToday(epochMillis) -> "Hoy"
            isYesterday(epochMillis) -> "Ayer"
            else -> DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())
                .format(Date(epochMillis))
        }
    }

    private fun isToday(epochMillis: Long): Boolean =
        isSameLocalDay(epochMillis, System.currentTimeMillis())

    private fun isYesterday(epochMillis: Long): Boolean {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val messageDate = Calendar.getInstance().apply {
            timeInMillis = epochMillis
        }
        return yesterday.get(Calendar.ERA) == messageDate.get(Calendar.ERA) &&
            yesterday.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == messageDate.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameLocalDay(firstEpochMillis: Long, secondEpochMillis: Long): Boolean {
        val firstDate = Calendar.getInstance().apply { timeInMillis = firstEpochMillis }
        val secondDate = Calendar.getInstance().apply { timeInMillis = secondEpochMillis }
        return firstDate.get(Calendar.ERA) == secondDate.get(Calendar.ERA) &&
            firstDate.get(Calendar.YEAR) == secondDate.get(Calendar.YEAR) &&
            firstDate.get(Calendar.DAY_OF_YEAR) == secondDate.get(Calendar.DAY_OF_YEAR)
    }

}
