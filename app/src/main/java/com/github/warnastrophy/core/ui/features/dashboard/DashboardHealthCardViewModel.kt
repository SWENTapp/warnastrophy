package com.github.warnastrophy.core.ui.features.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.local.HealthCardStorage
import com.github.warnastrophy.core.data.local.StorageResult
import com.github.warnastrophy.core.data.repository.HealthCardRepository
import com.github.warnastrophy.core.data.repository.HealthCardRepositoryProvider
import com.github.warnastrophy.core.model.HealthCard
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Dashboard Health Card component.
 *
 * Manages the loading and state of the health card data, separating business logic from the UI
 * layer.
 */
class DashboardHealthCardViewModel(
  private val repo: HealthCardRepository = HealthCardRepositoryProvider.repository,
  private val dispatcher: CoroutineDispatcher = Dispatchers.Main) :
    ViewModel() {
  val uiState:
      StateFlow<DashboardHealthCardUiState> = repo.observeMyHealthCard()
        .map<HealthCard?, DashboardHealthCardUiState> { DashboardHealthCardUiState.Success(it) }
        .onStart { emit(DashboardHealthCardUiState.Loading) }
        .catch { emit(DashboardHealthCardUiState.Error(it.message ?: "Error")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardHealthCardUiState.Loading)

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

    if (healthCard.organDonor == true) {
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
