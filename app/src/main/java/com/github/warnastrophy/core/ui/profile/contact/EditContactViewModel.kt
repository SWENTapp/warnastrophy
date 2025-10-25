package com.github.warnastrophy.core.ui.profile.contact

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.repository.ContactRepositoryProvider
import com.github.warnastrophy.core.data.repository.ContactsRepository
import com.github.warnastrophy.core.model.Contact
import com.github.warnastrophy.core.util.isValidPhoneNumber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditContactUIState(
    val fullName: String = "",
    val phoneNumber: String = "",
    val relationship: String = "",
    val errorMsg: String? = null,
    val invalidFullNameMsg: String? = null,
    val invalidPhoneNumberMsg: String? = null,
    val invalidRelationshipMsg: String? = null
) {
  val isValid: Boolean
    get() = fullName.isNotEmpty() && isValidPhoneNumber(phoneNumber) && relationship.isNotEmpty()
}

class EditContactViewModel(
    private val repository: ContactsRepository = ContactRepositoryProvider.repository
) : ViewModel() {
  private val _uiState = MutableStateFlow(EditContactUIState())
  val uiState: StateFlow<EditContactUIState> = _uiState.asStateFlow()
  val _navigateBack = MutableStateFlow(false)
  val navigateBack: StateFlow<Boolean> = _navigateBack

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /**
   * Loads a Contact by its ID and updates the UI state.
   *
   * @param contactId The ID of the Contact to be loaded.
   */
  fun loadContact(contactId: String) {
    viewModelScope.launch {
      val res = repository.getContact(contactId)
      res.fold(
          onSuccess = { contact ->
            _uiState.value =
                EditContactUIState(
                    fullName = contact.fullName,
                    phoneNumber = contact.phoneNumber,
                    relationship = contact.relationship)
          },
          onFailure = { e ->
            Log.e("EditContactViewModel", "Error fetching contacts", e)
            setErrorMsg("Failed to load contacts: ${e.message}")
          })
    }
  }

  // Helper function
  private fun <T> executeRepositoryOperation(
      operation: suspend () -> Result<T>,
      actionName: String // e.g., "edit Contact" or "delete Contact"
  ) {
    viewModelScope.launch {
      val result = operation()

      result
          .onSuccess {
            // SUCCESS: Common boilerplate
            clearErrorMsg()
            _navigateBack.value = true
          }
          .onFailure { exception ->
            // FAILURE: Common boilerplate, customized by actionName
            val logTag = "EditContactViewModel"
            val errorMessage = "Failed to $actionName: ${exception.message ?: "Unknown error"}"

            Log.e(logTag, "Error $actionName", exception)
            setErrorMsg(errorMessage)
          }
    }
  }

  /**
   * Adds a Contact document.
   *
   * @param id The contact document to be added.
   */
  fun editContact(id: String) {
    val state = _uiState.value
    if (!state.isValid) {
      setErrorMsg("At least one field is not valid")
      return
    }
    val newContact =
        Contact(
            phoneNumber = state.phoneNumber,
            fullName = state.fullName,
            relationship = state.relationship,
            id = id)
    executeRepositoryOperation(
        operation = { repository.editContact(id, newContact) }, actionName = "edit contact")
  }

  /**
   * Deletes a Contact document by its ID.
   *
   * @param contactID The ID of the Contact document to be deleted.
   */
  fun deleteContact(contactID: String) {
    executeRepositoryOperation(
        operation = { repository.deleteContact(contactID) }, actionName = "delete contact")
  }

  /*
     Helper function
  */
  private fun updateUiState(
      // This accepts a lambda that operates on the current state (this)
      // and must return the new state.
      updateBlock: EditContactUIState.() -> EditContactUIState
  ) {
    // Executes the lambda, effectively doing: _uiState.value = _uiState.value.updateBlock()
    _uiState.value = _uiState.value.updateBlock()
  }

  // Functions to update the UI state.
  fun setFullName(fullName: String) = updateUiState {
    copy(
        fullName = fullName,
        invalidFullNameMsg = if (fullName.isBlank()) "Full name cannot be empty" else null)
  }

  fun setPhoneNumber(phoneNumber: String) = updateUiState {
    copy(
        phoneNumber = phoneNumber,
        invalidPhoneNumberMsg =
            if (!isValidPhoneNumber(phoneNumber)) "Invalid phone number" else null)
  }

  fun setRelationship(relationship: String) = updateUiState {
    copy(
        relationship = relationship,
        invalidRelationshipMsg =
            if (relationship.isBlank()) "Relationship cannot be empty" else null)
  }

  fun resetNavigation() {
    _navigateBack.value = false
  }
}
