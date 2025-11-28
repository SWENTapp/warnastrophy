package com.github.warnastrophy.core.ui.features.activity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import com.github.warnastrophy.core.data.repository.ActivityRepository
import com.github.warnastrophy.core.data.repository.MockActivityRepository
import com.github.warnastrophy.core.model.Activity
import com.github.warnastrophy.core.ui.features.dashboard.activity.ActivityListScreen
import com.github.warnastrophy.core.ui.features.dashboard.activity.ActivityListScreenTestTags
import com.github.warnastrophy.core.ui.features.dashboard.activity.ActivityListViewModel
import com.github.warnastrophy.core.util.AppConfig
import com.github.warnastrophy.core.util.BaseAndroidComposeTest
import kotlin.collections.forEach
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ActivityListScreenTest : BaseAndroidComposeTest() {
  private val mockActivities = listOf(Activity("1", "Surfing"), Activity("2", "Climing"))

  private fun setContent(withInitialActivities: List<Activity> = emptyList()) {
    val userId = AppConfig.defaultUserId
    val repository: ActivityRepository = MockActivityRepository()
    runTest { withInitialActivities.forEach { repository.addActivity(activity = it) } }
    val mockViewModel = ActivityListViewModel(repository = repository, userId = userId)
    composeTestRule.setContent { ActivityListScreen(activityListViewModel = mockViewModel) }
  }

  @Test
  fun testTagsCorrectlySetWhenListIsEmpty() {
    setContent()
    composeTestRule.onNodeWithTag(ActivityListScreenTestTags.ACTIVITIES_LIST).assertIsNotDisplayed()
  }

  @Test
  fun testTagsCorrectlySetWhenListIsNotEmpty() {
    setContent(withInitialActivities = mockActivities)
    composeTestRule.onNodeWithTag(ActivityListScreenTestTags.ACTIVITIES_LIST).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(ActivityListScreenTestTags.getTestTagForActivityItem(mockActivities[0]))
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(ActivityListScreenTestTags.getTestTagForActivityItem(mockActivities[1]))
        .assertIsDisplayed()
  }

  @Test
  fun activitiesListDisplaysActivityName() {
    val sampleActivity = mockActivities[0]
    val activitiesList = listOf(sampleActivity)
    setContent(withInitialActivities = activitiesList)
    composeTestRule
        .onNode(
            hasTestTag(ActivityListScreenTestTags.getTestTagForActivityItem(sampleActivity))
                .and(hasAnyDescendant(hasText(sampleActivity.activityName))),
            useUnmergedTree = true)
        .assertIsDisplayed()
  }
}
