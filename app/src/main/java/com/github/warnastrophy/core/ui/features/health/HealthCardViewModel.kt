package com.github.warnastrophy.core.ui.features.health

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.local.HealthCardStorage
import com.github.warnastrophy.core.data.local.StorageResult
import com.github.warnastrophy.core.data.repository.HealthCardRepository
import com.github.warnastrophy.core.data.repository.HealthCardRepositoryProvider
import com.github.warnastrophy.core.domain.model.HealthCard
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

  private val _uiState = MutableStateFlow<HealthCardUiState>(HealthCardUiState.Idle)
  val uiState: StateFlow<HealthCardUiState> = _uiState.asStateFlow()

  private val _currentCard = MutableStateFlow<HealthCard?>(null)
  val currentCard: StateFlow<HealthCard?> = _currentCard.asStateFlow()

  /**
   * Loads the health card for a given user from storage.
   *
   * Updates [_currentCard] and [_uiState] accordingly.
   *
   * @param context Context used for storage access
   * @param userId The unique identifier of the user
   */
  fun loadHealthCard(context: Context, userId: String) {
    viewModelScope.launch(dispatcher) {
      _uiState.value = HealthCardUiState.Loading
      when (val result = HealthCardStorage.loadHealthCard(context, userId)) {
        is StorageResult.Success -> {
          _currentCard.value = result.data
          _uiState.value = HealthCardUiState.Success("Health card successfully loaded")
        }
        is StorageResult.Error -> {
          _uiState.value = HealthCardUiState.Error(result.exception.message ?: "Loading error")
        }
      }
    }
  }

  /**
   * Saves a new health card for a user.
   *
   * Updates [_currentCard] and [_uiState] on success or failure.
   *
   * @param context Context used for storage access
   * @param userId The unique identifier of the user
   * @param card The HealthCard to save
   */
  fun saveHealthCard(context: Context, userId: String, card: HealthCard) {
    viewModelScope.launch(dispatcher) {
      _uiState.value = HealthCardUiState.Loading
      when (val result = HealthCardStorage.saveHealthCard(context, userId, card)) {
        is StorageResult.Success -> {
          _currentCard.value = card
          _uiState.value = HealthCardUiState.Success("Health card saved successfully")
        }
        is StorageResult.Error -> {
          _uiState.value = HealthCardUiState.Error(result.exception.message ?: "Saving error")
        }
      }
    }
  }

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

  /**
   * Updates an existing health card.
   *
   * The current card is updated immediately on success to reflect changes in the UI.
   *
   * @param context Context used for storage access
   * @param userId The unique identifier of the user
   * @param newCard The updated HealthCard data
   */
  fun updateHealthCard(context: Context, userId: String, newCard: HealthCard) {
    viewModelScope.launch(dispatcher) {
      _uiState.value = HealthCardUiState.Loading
      when (val result = HealthCardStorage.updateHealthCard(context, userId, newCard)) {
        is StorageResult.Success -> {
          _currentCard.value = newCard
          _uiState.value = HealthCardUiState.Success("Health card updated successfully")
        }
        is StorageResult.Error -> {
          _uiState.value = HealthCardUiState.Error(result.exception.message ?: "Updating error")
        }
      }
    }
  }

  /**
   * Deletes the health card of a user.
   *
   * Clears [_currentCard] and updates [_uiState] accordingly.
   *
   * @param context Context used for storage access
   * @param userId The unique identifier of the user
   */
  fun deleteHealthCard(context: Context, userId: String) {
    viewModelScope.launch(dispatcher) {
      _uiState.value = HealthCardUiState.Loading
      when (val result = HealthCardStorage.deleteHealthCard(context, userId)) {
        is StorageResult.Success -> {
          _currentCard.value = null
          _uiState.value = HealthCardUiState.Success("Health card deleted successfully")
        }
        is StorageResult.Error -> {
          _uiState.value = HealthCardUiState.Error(result.exception.message ?: "Deletion error")
        }
      }
    }
  }

  /**
   * Resets the UI state to [Idle].
   *
   * Useful after showing a success or error message.
   */
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
