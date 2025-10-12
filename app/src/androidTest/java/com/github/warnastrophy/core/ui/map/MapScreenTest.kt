package com.github.warnastrophy.core.ui.map

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.github.warnastrophy.core.ui.viewModel.MapViewModel
import org.junit.Rule
import org.junit.Test

class MapScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  val viewModel = MapViewModel()

  @Test
  fun testMapScreenIsDisplayed() {
    composeTestRule.setContent { MapScreen(viewModel) }
    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertExists()
  }
}
