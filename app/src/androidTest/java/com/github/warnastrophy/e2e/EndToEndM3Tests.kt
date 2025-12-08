package com.github.warnastrophy.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.warnastrophy.core.ui.features.dashboard.DashboardHealthCardTestTags
import com.github.warnastrophy.core.ui.features.health.HealthCardPopUpTestTags
import org.junit.Test

class EndToEndM3Tests : EndToEndUtils() {
  @Test
  fun update_and_delete_health_card() {
    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.CARD)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.EDIT_BUTTON).performClick()
    createHealthCard()
    deleteHealthCard()
  }
}
