import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.github.warnastrophy.MainActivity
import com.github.warnastrophy.WarnastrophyAppTestTags
import org.junit.Rule
import org.junit.Test

class MainActivityTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun mainActivity_launches_and_displays_content() {
    composeTestRule.onNodeWithTag(WarnastrophyAppTestTags.MAIN_SCREEN).assertIsDisplayed()
  }
}
