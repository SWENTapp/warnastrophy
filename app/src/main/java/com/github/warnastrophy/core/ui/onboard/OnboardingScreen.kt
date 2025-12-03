package com.github.warnastrophy.core.ui.onboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** Test tag for onboarding screen */
object OnboardingScreenTestTags {
  const val NEXT_BUTTON = "NextButton"
  const val INDICATOR = "indicator"
}

/**
 * Represents the Back and Next button labels for a specific onboarding page.
 *
 * @property back Label for the back button.
 * @property next Label for the next button.
 */
data class OnboardingButtons(val back: String, val next: String)

/**
 * Returns the Back and Next button labels for the given onboarding page index.
 *
 * @param page Current page index (0-based).
 * @return An [OnboardingButtons] instance containing labels for the current page.
 */
private fun getButtonState(page: Int): OnboardingButtons =
    when (page) {
      0 -> OnboardingButtons("", "Next")
      1 -> OnboardingButtons("Back", "Next")
      2 -> OnboardingButtons("Back", "Start")
      else -> OnboardingButtons("", "")
    }

/**
 * Handles the Next button click behavior.
 * - If the user is not on the last page, it advances to the next page.
 * - If the user is on the last page, it triggers the onboarding completion callback.
 *
 * @param page Current page index.
 * @param pageCount Total number of onboarding pages.
 * @param pagerState Pager state used for animated scrolling.
 * @param onFinished Callback invoked when onboarding finishes.
 */
private suspend fun handleNextClick(
    page: Int,
    pageCount: Int,
    pagerState: PagerState,
    onFinished: () -> Unit
) {
  if (page < pageCount - 1) {
    pagerState.animateScrollToPage(page + 1)
  } else {
    onFinished()
  }
}

/**
 * Handles the Back button click behavior.
 *
 * @param page Current page index.
 * @param pagerState Pager state used for animated scrolling.
 */
private suspend fun handleBackClick(page: Int, pagerState: PagerState) {
  if (page > 0) {
    pagerState.animateScrollToPage(page - 1)
  }
}

/**
 * Composable that displays the onboarding flow of the app using a horizontal pager.
 *
 * @param onFinished Lambda that is invoked when the user completes the onboarding walkthrough.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
  val pages =
      listOf(OnboardingModel.FirstPage, OnboardingModel.SecondPage, OnboardingModel.ThirdPage)

  val pagerState = rememberPagerState(initialPage = 0) { pages.size }

  val buttons by remember { derivedStateOf { getButtonState(pagerState.currentPage) } }

  val scope = rememberCoroutineScope()

  Scaffold(
      bottomBar = {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp, 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (buttons.back.isNotEmpty()) {
                  // Back button
                  ButtonUi(
                      text = buttons.back,
                      backgroundColor = Color.Transparent,
                      textColor = Color.Gray) {
                        scope.launch {
                          handleBackClick(page = pagerState.currentPage, pagerState = pagerState)
                        }
                      }
                }
              }
              Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                IndicatorUI(pageSize = pages.size, currentPage = pagerState.currentPage)
              }

              Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                // Next button
                ButtonUi(
                    modifier = Modifier.testTag(OnboardingScreenTestTags.NEXT_BUTTON),
                    text = buttons.next,
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    textColor = MaterialTheme.colorScheme.onPrimary) {
                      scope.launch {
                        handleNextClick(
                            page = pagerState.currentPage,
                            pageCount = pages.size,
                            pagerState = pagerState,
                            onFinished = onFinished)
                      }
                    }
              }
            }
      },
      content = {
        Column(Modifier.padding(it)) {
          HorizontalPager(state = pagerState) { index ->
            OnboardingGraphUI(onboardingModel = pages[index])
          }
        }
      })
}

/**
 * A reusable button composable for onboarding or other UI screens.
 *
 * @param text The text displayed on the button.
 * @param backgroundColor The background color of the button.
 * @param textColor The color of the text.
 * @param textStyle The [TextStyle] to apply to the text.
 * @param fontSize The font size in sp.
 * @param modifier Optional [Modifier] for styling or test tags.
 * @param onClick Lambda invoked when the button is clicked.
 */
@Composable
fun ButtonUi(
    text: String = "Next",
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onPrimary,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    fontSize: Int = 14,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

  Button(
      modifier = modifier,
      onClick = onClick,
      colors =
          ButtonDefaults.buttonColors(containerColor = backgroundColor, contentColor = textColor),
      shape = RoundedCornerShape(10.dp)) {
        Text(text = text, fontSize = fontSize.sp, style = textStyle)
      }
}

/**
 * Displays a horizontal row of indicators to show the current onboarding page.
 *
 * The current page is highlighted with [selectedColor], while others use [unselectedColor].
 *
 * @param pageSize Total number of pages in the onboarding flow.
 * @param currentPage Index of the currently visible page.
 * @param selectedColor Color of the active page indicator.
 * @param unselectedColor Color of inactive page indicators.
 */
@Composable
fun IndicatorUI(
    pageSize: Int,
    currentPage: Int,
    selectedColor: Color = MaterialTheme.colorScheme.secondary,
    unselectedColor: Color = MaterialTheme.colorScheme.secondaryContainer,
) {
  Row(horizontalArrangement = Arrangement.SpaceBetween) {
    repeat(pageSize) {
      Spacer(modifier = Modifier.size(2.5.dp))

      Box(
          modifier =
              Modifier.height(14.dp)
                  .width(width = if (it == currentPage) 32.dp else 14.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(color = if (it == currentPage) selectedColor else unselectedColor)
                  .testTag(OnboardingScreenTestTags.INDICATOR))

      Spacer(modifier = Modifier.size(2.5.dp))
    }
  }
}
