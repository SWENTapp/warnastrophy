package com.github.warnastrophy.core.ui.features.dashboard

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.R
import com.github.warnastrophy.core.domain.model.HealthCard
import com.github.warnastrophy.core.ui.components.Loading
import com.github.warnastrophy.core.ui.components.LoadingTestTags
import com.github.warnastrophy.core.ui.components.StandardDashboardButton
import com.github.warnastrophy.core.ui.components.StandardDashboardCard

/** Object holding test tag constants for the Health Card UI. */
object DashboardHealthCardTestTags {
  const val CARD = "dashboardHealthCard"
  const val TITLE = "dashboardHealthCardTitle"
  const val SUBTITLE = "dashboardHealthCardSubtitle"
}

/** Object holding color definitions for the Health Card UI. */
object DashboardHealthCardColors {
  val backgroundColor: Color
    @Composable @ReadOnlyComposable get() = colorResource(id = R.color.light_green)

  val textColor: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onPrimaryContainer
}

/**
 * A stateless composable function that displays the Health Card.
 *
 * This composable renders the Health Card with the provided [healthCard] data. If data is loading,
 * it shows a loading spinner.
 *
 * @param healthCard The health card data to display. Can be null if no data is available.
 * @param onHealthCardClick A callback to be invoked when the Health Card is clicked.
 * @param modifier A [Modifier] to be applied to the Health Card layout.
 * @param isLoading A boolean flag indicating whether the card content is still loading.
 * @param summaryText Optional pre-formatted summary text to display.
 */
@Composable
fun DashboardHealthCardStateless(
    healthCard: HealthCard?,
    onHealthCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    summaryText: String? = null
) {

  StandardDashboardCard(
      modifier =
          modifier.testTag(DashboardHealthCardTestTags.CARD).clickable { onHealthCardClick() },
      backgroundColor = DashboardHealthCardColors.backgroundColor,
      minHeight = 120.dp,
      maxHeight = 150.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween) {
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Health",
                        modifier = Modifier.testTag(DashboardHealthCardTestTags.TITLE),
                        color = DashboardHealthCardColors.textColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp)

                    Box(
                        modifier =
                            Modifier.testTag(DashboardEmergencyContactsTestTags.MANAGE_BUTTON)) {
                          StandardDashboardButton(label = "Manage", onClick = onHealthCardClick)
                        }
                  }

              Spacer(modifier = Modifier.height(8.dp))

              // Content area
              when {
                isLoading -> {
                  Box(
                      modifier = Modifier.fillMaxWidth().padding(8.dp),
                      contentAlignment = Alignment.Center) {
                        Loading(modifier = Modifier.testTag(LoadingTestTags.LOADING_INDICATOR))
                      }
                }
                healthCard != null && summaryText != null -> {
                  Text(
                      text = summaryText,
                      color = DashboardHealthCardColors.textColor,
                      fontSize = 14.sp,
                      modifier = Modifier.testTag(DashboardHealthCardTestTags.SUBTITLE),
                      lineHeight = 16.sp)
                }
                else -> {
                  Text(
                      text = "Add your medical info,\nallergies and meds",
                      color = DashboardHealthCardColors.textColor,
                      fontSize = 14.sp,
                      modifier = Modifier.testTag(DashboardHealthCardTestTags.SUBTITLE),
                      lineHeight = 16.sp)
                }
              }
            }
      }
}

/**
 * A stateful composable function that loads and displays the Health Card.
 *
 * This composable uses a ViewModel to manage the health card state and business logic.
 *
 * @param onHealthCardClick A callback to be invoked when the Health Card is clicked.
 * @param modifier A [Modifier] to be applied to the Health Card layout.
 * @param userId The ID of the user whose health card is being loaded.
 * @param context The [Context] to be used for loading the health card data.
 * @param viewModel The ViewModel managing the health card state.
 */
@Composable
fun DashboardHealthCardStateful(
    onHealthCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    userId: String = "John Doe",
    context: Context = LocalContext.current,
    viewModel: DashboardHealthCardViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsState()

  LaunchedEffect(Unit) { viewModel.loadHealthCard(context, userId) }
  when (val state = uiState) {
    is DashboardHealthCardUiState.Loading -> {
      DashboardHealthCardStateless(
          healthCard = null,
          onHealthCardClick = onHealthCardClick,
          modifier = modifier,
          isLoading = true)
    }
    is DashboardHealthCardUiState.Success -> {
      val summaryText = state.healthCard?.let { viewModel.getEmergencyHealthSummary(it) }
      DashboardHealthCardStateless(
          healthCard = state.healthCard,
          onHealthCardClick = onHealthCardClick,
          modifier = modifier,
          isLoading = false,
          summaryText = summaryText)
    }
    is DashboardHealthCardUiState.Error -> {
      DashboardHealthCardStateless(
          healthCard = null,
          onHealthCardClick = onHealthCardClick,
          modifier = modifier,
          isLoading = false)
    }
  }
}
