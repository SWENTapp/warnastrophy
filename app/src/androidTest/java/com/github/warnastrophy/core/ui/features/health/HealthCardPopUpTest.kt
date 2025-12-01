package com.github.warnastrophy.core.ui.features.health

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.warnastrophy.core.ui.theme.MainAppTheme
import com.github.warnastrophy.core.util.BaseAndroidComposeTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [BaseAndroidComposeTest] for the [HealthCardPopUp] composable. This class contains UI tests to
 * verify the behavior and appearance of the Health Card pop-up.
 */
class HealthCardPopUpTest : BaseAndroidComposeTest() {

  @Test
  fun healthCardPopUp_displaysCorrectTextContent_whenEmpty() {
    composeTestRule.setContent {
      MainAppTheme { HealthCardPopUp(onDismissRequest = {}, onClick = {}) }
    }

    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.ROOT_CARD)
    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.EDIT_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.CONTENT_CARD)
    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.EMPTY_STATE_TEXT).assertIsDisplayed()
  }

  @Test
  fun healthCardPopUp_editButtonTriggersOnClickCallback() {
    var isClicked = false

    composeTestRule.setContent {
      MainAppTheme { HealthCardPopUp(onDismissRequest = {}, onClick = { isClicked = true }) }
    }

    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.EDIT_BUTTON).performClick()

    assertTrue("The onClick callback should have been triggered.", isClicked)
  }
}
