package com.github.warnastrophy.core.ui.features.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.repository.HealthCardRepository
import com.github.warnastrophy.core.data.repository.HealthCardRepositoryProvider
import com.github.warnastrophy.core.model.HealthCard
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the health card feature.
 *
 * Handles loading, saving, updating, and deleting a HealthCard. Exposes the current card and UI
 * state as [StateFlow] for Compose or other observers.
 *
 * @param dispatcher Dispatcher attribute for testing purposes
 */
class HealthCardViewModel(
    private val repo: HealthCardRepository = HealthCardRepositoryProvider.repository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

  private val _uiState = MutableStateFlow<HealthCardUiState>(HealthCardUiState.Idle)
  val uiState: StateFlow<HealthCardUiState> = _uiState.asStateFlow()

  // Live, offline-friendly snapshot of the user's HealthCard
  val currentCard: StateFlow<HealthCard?> =
      repo
          .observeMyHealthCard()
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  /** One-shot refresh (optional; cache-first by default) */
  fun refreshOnce() =
      viewModelScope.launch(dispatcher) {
        _uiState.value = HealthCardUiState.Loading
        runCatching { repo.getMyHealthCardOnce(true) }
            .onSuccess { _uiState.value = HealthCardUiState.Success("Loaded") }
            .onFailure { _uiState.value = HealthCardUiState.Error(it.message ?: "Loading error") }
      }

  /** Create or update (upsert) the HealthCard */
  fun saveHealthCardDB(card: HealthCard) =
      viewModelScope.launch(dispatcher) {
        _uiState.value = HealthCardUiState.Loading
        runCatching { repo.upsertMyHealthCard(card) }
            .onSuccess { _uiState.value = HealthCardUiState.Success("Saved") }
            .onFailure { _uiState.value = HealthCardUiState.Error(it.message ?: "Saving error") }
      }

  /** Delete the HealthCard */
  fun deleteHealthCardDB() =
      viewModelScope.launch(dispatcher) {
        _uiState.value = HealthCardUiState.Loading
        runCatching { repo.deleteMyHealthCard() }
            .onSuccess { _uiState.value = HealthCardUiState.Success("Deleted") }
            .onFailure { _uiState.value = HealthCardUiState.Error(it.message ?: "Deletion error") }
      }

  fun resetUiState() {
    _uiState.value = HealthCardUiState.Idle
  }
}

/** Represents the UI state of the HealthCard screen. */
sealed class HealthCardUiState {
  /** Initial state, no action performed yet */
  data object Idle : HealthCardUiState()

  /** Loading state, operation in progress */
  data object Loading : HealthCardUiState()

  /** Successful operation with an optional message */
  data class Success(val message: String) : HealthCardUiState()

  /** Failed operation with an error message */
  data class Error(val message: String) : HealthCardUiState()
}
