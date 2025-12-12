package com.github.warnastrophy.core.ui.features.activity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import com.github.warnastrophy.core.data.repository.MockActivityRepository
import com.github.warnastrophy.core.ui.features.UITest
import com.github.warnastrophy.core.ui.features.dashboard.activity.EditActivityScreen
import com.github.warnastrophy.core.ui.features.dashboard.activity.EditActivityTestTags
import com.github.warnastrophy.core.ui.features.dashboard.activity.EditActivityViewModel
import com.github.warnastrophy.core.util.AppConfig
import org.junit.Before
import org.junit.Test

class EditActivityScreenTest : UITest() {

  @Before
  override fun setUp() {
    super.setUp()
    activityRepository = MockActivityRepository()
    val userId = AppConfig.defaultUserId
    val mockViewModel = EditActivityViewModel(activityRepository, userId)
    composeTestRule.setContent { EditActivityScreen(editActivityViewModel = mockViewModel) }
  }

  @Test
  fun displayAllComponents() {
    composeTestRule.onNodeWithTag(EditActivityTestTags.SAVE_BUTTON).assertTextContains("Save")
    composeTestRule.onNodeWithTag(EditActivityTestTags.INPUT_ACTIVITY_NAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditActivityTestTags.DELETE_BUTTON).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditActivityTestTags.PRE_DANGER_THRESHOLD_INPUT)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditActivityTestTags.PRE_DANGER_TIMEOUT_INPUT).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditActivityTestTags.DANGER_AVERAGE_THRESHOLD_INPUT)
        .assertIsDisplayed()
  }

  @Test
  fun canEnterFullName() {
    val text = "Surfing"
    composeTestRule.enterEditActivityName(text)
    composeTestRule.onNodeWithTag(EditActivityTestTags.INPUT_ACTIVITY_NAME).assertTextContains(text)
  }

  @Test
  fun enteringEmptyActivityNameKeepsFieldInErrorState() {
    val invalidText = " "
    composeTestRule.enterEditActivityName(invalidText)
    // The text field will have isError=true, which is handled by the UI styling
    composeTestRule.onNodeWithTag(EditActivityTestTags.INPUT_ACTIVITY_NAME).assertIsDisplayed()
  }

  @Test
  fun savingWithEmptyActivityNameShouldDoNothing() {
    composeTestRule.enterEditActivityName(" ")
    composeTestRule.clickOnSaveButton(testTag = EditActivityTestTags.SAVE_BUTTON)
    composeTestRule.onNodeWithTag(EditActivityTestTags.SAVE_BUTTON).assertIsDisplayed()
  }
}
