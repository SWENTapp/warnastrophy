package com.github.warnastrophy.core.ui.features.activity

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import com.github.warnastrophy.core.data.repository.MockActivityRepository
import com.github.warnastrophy.core.ui.features.UITest
import com.github.warnastrophy.core.ui.features.dashboard.activity.AddActivityScreen
import com.github.warnastrophy.core.ui.features.dashboard.activity.AddActivityTestTags
import com.github.warnastrophy.core.ui.features.dashboard.activity.AddActivityViewModel
import com.github.warnastrophy.core.util.AppConfig
import org.junit.Before
import org.junit.Test

class AddActivityScreenTest : UITest() {
  private val userId = AppConfig.defaultUserId

  @Before
  override fun setUp() {
    super.setUp()
    activityRepository = MockActivityRepository()
    val mockViewModel = AddActivityViewModel(repository = activityRepository, userId = userId)
    composeTestRule.setContent { AddActivityScreen(addActivityViewModel = mockViewModel) }
  }

  @Test
  fun displayAllComponents() {
    composeTestRule.onNodeWithTag(AddActivityTestTags.SAVE_BUTTON).assertTextContains("Save")
    composeTestRule.onNodeWithTag(AddActivityTestTags.INPUT_ACTIVITY_NAME).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddActivityTestTags.PRE_DANGER_THRESHOLD_INPUT)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddActivityTestTags.PRE_DANGER_TIMEOUT_INPUT).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddActivityTestTags.DANGER_AVERAGE_THRESHOLD_INPUT)
        .assertIsDisplayed()
  }

  @Test
  fun canEnterActivityName() {
    val actName = "Surfing"
    composeTestRule.enterAddActivityName(actName)
    composeTestRule
        .onNodeWithTag(AddActivityTestTags.INPUT_ACTIVITY_NAME)
        .assertTextContains(actName)
  }

  @Test
  fun enteringEmptyActivityNameShowsErrorMessage() {
    val invalidText = " "
    composeTestRule.enterAddActivityName(invalidText)
    composeTestRule
        .onNodeWithTag(AddActivityTestTags.INPUT_ACTIVITY_NAME)
        .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
  }

  @Test
  fun savingWithEmptyActivityNameShouldDoNothing() =
      checkNoEntityWasAdded(
          {
            composeTestRule.enterAddActivityName(" ")

            composeTestRule.clickOnSaveButton(testTag = AddActivityTestTags.SAVE_BUTTON)

            composeTestRule.onNodeWithTag(AddActivityTestTags.SAVE_BUTTON).assertIsDisplayed()
          },
          userId,
          getAllEntities = { userId -> activityRepository.getAllActivities(userId) })

  @Test
  fun movementConfigFieldsHaveDefaultValues() {
    composeTestRule
        .onNodeWithTag(AddActivityTestTags.PRE_DANGER_THRESHOLD_INPUT)
        .assertTextContains("50.0")
    composeTestRule
        .onNodeWithTag(AddActivityTestTags.PRE_DANGER_TIMEOUT_INPUT)
        .assertTextContains("10s")
    composeTestRule
        .onNodeWithTag(AddActivityTestTags.DANGER_AVERAGE_THRESHOLD_INPUT)
        .assertTextContains("1.0")
  }
}
