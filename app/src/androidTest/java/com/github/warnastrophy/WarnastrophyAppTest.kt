package com.github.warnastrophy

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.warnastrophy.core.ui.onboard.OnboardingScreenTestTags
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

  companion object {

    @BeforeClass
    @JvmStatic
    fun setupFirebaseMocks() {
      mockkStatic(FirebaseApp::class)
      mockkStatic(FirebaseAuth::class)
      mockkStatic(FirebaseFirestore::class)

      val mockApp: FirebaseApp = mockk(relaxed = true)
      every { FirebaseApp.initializeApp(any()) } returns mockApp
      every { FirebaseApp.getInstance() } returns mockApp
      every { FirebaseApp.getApps(any()) } returns listOf(mockApp)

      val mockAuth: FirebaseAuth = mockk(relaxed = true)
      every { FirebaseAuth.getInstance() } returns mockAuth

      val mockFirestore: FirebaseFirestore = mockk(relaxed = true)
      every { FirebaseFirestore.getInstance() } returns mockFirestore
    }

    @AfterClass
    @JvmStatic
    fun tearDownFirebaseMocks() {
      unmockkAll()
    }
  }

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun mainActivity_walk_through_onBoarding_screens() {
    composeTestRule.onNodeWithTag(OnboardingScreenTestTags.NEXT_BUTTON).assertIsDisplayed()
    composeTestRule.onAllNodesWithTag(OnboardingScreenTestTags.INDICATOR)[0].assertIsDisplayed()

    val nextButtonNode = composeTestRule.onNodeWithTag(OnboardingScreenTestTags.NEXT_BUTTON)

    // --- Page 1 to Page 2 (Button Text: "Next") ---
    nextButtonNode.assertIsDisplayed()
    nextButtonNode.performClick()
    composeTestRule.waitForIdle()

    // --- Page 2 to Page 3 (Button Text: "Next") ---
    nextButtonNode.assertIsDisplayed()
    nextButtonNode.performClick()
    composeTestRule.waitForIdle()

    // --- Final Click (Button Text: "Start") ---
    nextButtonNode.assertIsDisplayed()
    nextButtonNode.assert(hasText("Start"))

    // Perform the final click that calls onFinished()
    nextButtonNode.performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(WarnastrophyAppTestTags.MAIN_SCREEN).assertIsDisplayed()
  }
}
