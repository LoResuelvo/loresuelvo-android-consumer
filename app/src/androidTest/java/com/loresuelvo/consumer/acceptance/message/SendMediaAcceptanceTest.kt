@HiltAndroidTest
@UninstallModules(RepositoryModule::class)
class SendMediaAcceptanceTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule =
        createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var conversationRepository: FakeConversationRepository
}