package com.github.warnastrophy

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
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
  fun mainActivity_launches_and_displays_content() {
    composeTestRule.onNodeWithTag(WarnastrophyAppTestTags.MAIN_SCREEN).assertIsDisplayed()
  }
}
