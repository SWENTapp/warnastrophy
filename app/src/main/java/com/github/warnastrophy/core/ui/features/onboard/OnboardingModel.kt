package com.github.warnastrophy.core.ui.features.onboard

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.github.warnastrophy.R

/**
 * Represents a single page of the onboarding flow in the app.
 *
 * Each onboarding page has a [title] and [description] explaining the feature or permissions
 * required to the user.
 *
 * @property title The main title displayed for the onboarding page.
 * @property description A descriptive text explaining the feature or permission.
 */
sealed class OnboardingModel(
    @DrawableRes val image: Int,
    @StringRes val title: Int,
    @StringRes val description: Int
) {
  data object FirstPage :
      OnboardingModel(
          image = R.drawable.onboarding_screen_1,
          title = R.string.title_first_page,
          description = R.string.description_first_page)

  data object SecondPage :
      OnboardingModel(
          image = R.drawable.onboarding_screen_2,
          title = R.string.title_second_page,
          description = R.string.description_second_page)
}
