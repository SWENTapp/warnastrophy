package com.github.warnastrophy.core.ui.features.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.local.HealthCardStorage
import com.github.warnastrophy.core.data.local.StorageResult
import com.github.warnastrophy.core.domain.model.HealthCard
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Dashboard Health Card component.
 *
 * Manages the loading and state of the health card data, separating business logic from the UI
 * layer.
 */
class DashboardHealthCardViewModel(private val dispatcher: CoroutineDispatcher = Dispatchers.Main) :
    ViewModel() {
  private val _uiState =
      MutableStateFlow<DashboardHealthCardUiState>(DashboardHealthCardUiState.Loading)
  val uiState: StateFlow<DashboardHealthCardUiState> = _uiState.asStateFlow()

  /**
   * Loads the health card for the specified user.
   *
   * @param context Android context to access DataStore.
   * @param userId Unique identifier for the user.
   */
  fun loadHealthCard(context: Context, userId: String) {
    viewModelScope.launch(dispatcher) {
      _uiState.value = DashboardHealthCardUiState.Loading

      when (val result = HealthCardStorage.loadHealthCard(context, userId)) {
        is StorageResult.Success -> {
          _uiState.value = DashboardHealthCardUiState.Success(result.data)
        }
        is StorageResult.Error -> {
          _uiState.value =
              DashboardHealthCardUiState.Error(result.exception.message ?: "Unknown error")
        }
      }
    }
  }

  /**
   * Generates a summary of emergency health information for display.
   *
   * @param healthCard The [HealthCard] containing the user's health information.
   * @return A formatted string containing the emergency health summary.
   */
  fun getEmergencyHealthSummary(healthCard: HealthCard): String {
    val lines = mutableListOf<String>()

    fun formatList(items: List<String>): String =
        if (items.size <= 2) items.joinToString(", ")
        else "${items.take(2).joinToString(", ")} + ${items.size - 2} more"

    val bloodTypeLine = healthCard.bloodType?.let { "Blood: $it" } ?: "Unknown"
    lines.add(bloodTypeLine)

    if (healthCard.allergies.isNotEmpty()) {
      lines.add(formatList(healthCard.allergies))
    }

    val additionalLines = mutableListOf<String>()

    if (healthCard.medications.isNotEmpty()) {
      val medsText = if (healthCard.medications.size == 1) "med" else "meds"
      additionalLines.add("${healthCard.medications.size} $medsText")
    }

    if (healthCard.organDonor) {
      additionalLines.add("Organ donor")
    }

    if (additionalLines.isNotEmpty()) {
      lines.add(additionalLines.joinToString(" • "))
    }

    if (lines.size == 1 && healthCard.bloodType == null) {
      lines.add("No critical info added")
    }

    return lines.joinToString("\n")
  }
}

sealed class DashboardHealthCardUiState {
  data object Loading : DashboardHealthCardUiState()

  data class Success(val healthCard: HealthCard?) : DashboardHealthCardUiState()

  data class Error(val message: String) : DashboardHealthCardUiState()
}
