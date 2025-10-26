package com.github.warnastrophy.core.ui.profile.contact

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.repository.ContactRepositoryProvider
import com.github.warnastrophy.core.data.repository.ContactsRepository
import com.github.warnastrophy.core.model.Contact
import com.github.warnastrophy.core.util.isValidPhoneNumber
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the complete UI state for the Add Contact screen, including all input fields,
 * validation messages, and the overall validation status.
 *
 * Moving validation logic into the UI state object keeps the ViewModel focused on data operations.
 *
 * @property fullName The current text input for the contact's full name.
 * @property phoneNumber The current text input for the contact's phone number.
 * @property relationship The current text input for the contact's relationship (e.g., "Family,"
 *   "Work").
 * @property errorMsg A transient error message to be displayed to the user (e.g., via a Toast).
 *   Null if no error.
 * @property invalidFullNameMsg The specific error message if the full name is invalid. Null if
 *   valid.
 * @property invalidPhoneNumberMsg The specific error message if the phone number is invalid. Null
 *   if valid.
 * @property invalidRelationshipMsg The specific error message if the relationship is invalid. Null
 *   if valid.
 */
data class AddContactUIState(
    val fullName: String = "",
    val phoneNumber: String = "",
    val relationship: String = "",
    val errorMsg: String? = null,
    val invalidFullNameMsg: String? = null,
    val invalidPhoneNumberMsg: String? = null,
    val invalidRelationshipMsg: String? = null
) {
  val isValid: Boolean
    get() = fullName.isNotBlank() && isValidPhoneNumber(phoneNumber) && relationship.isNotEmpty()
}

/**
 * ViewModel responsible for managing the state and logic for the Add Contact screen.
 *
 * It handles form input changes, validates fields, and manages the asynchronous operation of
 * persisting a new contact via the repository.
 *
 * @property repository The data source dependency used for contact persistence.
 */
class AddContactViewModel(
    private val repository: ContactsRepository = ContactRepositoryProvider.repository
) : ViewModel() {
  private val _uiState = MutableStateFlow(AddContactUIState())
  val uiState: StateFlow<AddContactUIState> = _uiState.asStateFlow()
  private val _navigateBack = MutableSharedFlow<Unit>(replay = 0)
  var navigateBack: SharedFlow<Unit> = _navigateBack.asSharedFlow()

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /** Adds a Contact document. */
  fun addContact() {
    val state = _uiState.value
    if (!state.isValid) {
      setErrorMsg("At least one field is not valid!")
      return
    }
    addContactToRepository(
        Contact(
            id = repository.getNewUid(),
            fullName = state.fullName,
            phoneNumber = state.phoneNumber,
            relationship = state.relationship))
  }

  private fun addContactToRepository(contact: Contact) {
    viewModelScope.launch {
      val result = repository.addContact(contact)
      result
          .onSuccess {
            clearErrorMsg()
            _navigateBack.emit(Unit)
          }
          .onFailure { exception ->
            Log.e("AddContactViewModel", "Error add Contact", exception)
            setErrorMsg("Failed to add Contact: ${exception.message ?: "Unknown error"}")
          }
    }
  }
  /*
  Helper function
   */
  private fun updateUiState(
      // This accepts a lambda that operates on the current state (this)
      // and must return the new state.
      updateBlock: AddContactUIState.() -> AddContactUIState
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

  fun setRelationShip(relationship: String) = updateUiState {
    copy(
        relationship = relationship,
        invalidRelationshipMsg =
            if (relationship.isBlank()) "Relationship cannot be empty" else null)
  }
}
