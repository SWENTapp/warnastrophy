package com.github.warnastrophy.core.ui.dashboard

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.warnastrophy.core.data.local.HealthCardStorage
import com.github.warnastrophy.core.data.local.StorageResult
import com.github.warnastrophy.core.model.HealthCard
import com.github.warnastrophy.core.ui.components.Loading
import com.github.warnastrophy.core.ui.components.LoadingTestTags
import com.github.warnastrophy.core.ui.components.StandardDashboardCard
import kotlinx.coroutines.launch

/** Object holding test tag constants for the Health Card UI. */
object DashboardHealthCardTestTags {
  const val CARD = "dashboardHealthCard"
  const val TITLE = "dashboardHealthCardTitle"
  const val SUBTITLE = "dashboardHealthCardSubtitle"
}

/** Object holding color definitions for the Health Card UI. */
object DashboardHealthCardColors {
  val backgroundColor: Color
    @Composable
    @ReadOnlyComposable
    get() =
        MaterialTheme.colorScheme.primaryContainer.copy(
            red = 0.908f, green = 0.960f, blue = 0.913f, alpha = 1f)

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
 */
@Composable
fun DashboardHealthCardStateless(
    healthCard: HealthCard?,
    onHealthCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
  StandardDashboardCard(
      modifier =
          modifier.testTag(DashboardHealthCardTestTags.CARD).clickable { onHealthCardClick() },
      backgroundColor = DashboardHealthCardColors.backgroundColor,
      minHeight = 120.dp,
      maxHeight = 150.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween) {
              Text(
                  text = "Health",
                  modifier = Modifier.testTag(DashboardHealthCardTestTags.TITLE),
                  color = DashboardHealthCardColors.textColor,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 16.sp)

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
                healthCard != null -> {
                  Text(
                      text = getEmergencyHealthSummary(healthCard),
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
 * A private helper function that generates a summary of emergency health information.
 *
 * This function generates a formatted string summarizing the user's health information such as
 * blood type, allergies, chronic conditions, medications, and organ donor status.
 *
 * @param healthCard The [HealthCard] containing the user's health information.
 * @return A formatted string containing the emergency health summary.
 */
private fun getEmergencyHealthSummary(healthCard: HealthCard): String {
  val lines = mutableListOf<String>()

  val bloodType = healthCard.bloodType?.let { "Blood: $it" } ?: "Unknown"
  lines.add(bloodType)

  if (healthCard.allergies.isNotEmpty()) {
    val allergyText =
        if (healthCard.allergies.size <= 2) {
          healthCard.allergies.joinToString(", ")
        } else {
          val displayAllergies = healthCard.allergies.take(2).joinToString(", ")
          "$displayAllergies + ${healthCard.allergies.size - 2} more"
        }
    lines.add(allergyText)
  }

  val additionalInfo = mutableListOf<String>()

  if (healthCard.chronicConditions.isNotEmpty()) {
    val conditions =
        if (healthCard.chronicConditions.size <= 2) {
          healthCard.chronicConditions.joinToString(", ")
        } else {
          val display = healthCard.chronicConditions.take(2).joinToString(", ")
          "$display + ${healthCard.chronicConditions.size - 2}"
        }

    additionalInfo.add(conditions)
  }

  if (healthCard.medications.isNotEmpty()) {
    additionalInfo.add(
        "${healthCard.medications.size} ${if (healthCard.medications.size == 1) "med" else "meds"}")
  }

  if (healthCard.organDonor) {
    additionalInfo.add("Organ Donor")
  }

  if (additionalInfo.isNotEmpty()) {
    lines.add(additionalInfo.joinToString(" • "))
  }

  if (lines.size == 1 && healthCard.bloodType == null) {
    lines.add("No critical info added")
  }

  return lines.joinToString("\n")
}

/**
 * A stateful composable function that loads and displays the Health Card.
 *
 * This composable loads the health card data asynchronously using the [HealthCardStorage] and
 * displays the Health Card once the data is loaded.
 *
 * @param onHealthCardClick A callback to be invoked when the Health Card is clicked.
 * @param modifier A [Modifier] to be applied to the Health Card layout.
 * @param userId The ID of the user whose health card is being loaded.
 * @param context The [Context] to be used for loading the health card data.
 */
@Composable
fun DashboardHealthCardStateful(
    onHealthCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    userId: String = "John Doe",
    context: Context = LocalContext.current
) {
  var healthCard by remember { mutableStateOf<HealthCard?>(null) }
  var isLoading by remember { mutableStateOf(true) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(userId) {
    scope.launch {
      isLoading = true
      when (val result = HealthCardStorage.loadHealthCard(context, userId)) {
        is StorageResult.Success -> {
          healthCard = result.data
          isLoading = false
        }
        is StorageResult.Error -> {
          healthCard = null
          isLoading = false
        }
      }
    }
  }

  DashboardHealthCardStateless(
      healthCard = healthCard,
      onHealthCardClick = onHealthCardClick,
      modifier = modifier,
      isLoading = isLoading)
}
