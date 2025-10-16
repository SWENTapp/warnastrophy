package com.github.warnastrophy.core.ui.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.model.contact.Contact
import com.github.warnastrophy.core.model.contact.ContactsRepository
import com.github.warnastrophy.core.model.util.ContactRepositoryProvider
import com.github.warnastrophy.core.ui.profile.contact.isValidPhoneNumber
import java.lang.Exception
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddContactUIState(
    val fullName: String = "",
    val phoneNumber: String = "",
    val relationship: String = "",
    val errorMsg: String? = null,
    val invalidFullNameMsg: String? = null,
    val invalidPhoneNumberMsg: String? = null,
    val invalidRelationshipMsg: String? = null
) {
  fun isValidPhoneNumber(phone: String): Boolean {
    // Regex for basic validation: optional '+' at start, followed by 10-15 digits
    return phone.matches(Regex("^\\+?[0-9]{10,15}\$"))
  }

  val isValid: Boolean
    get() = fullName.isNotEmpty() && isValidPhoneNumber(phoneNumber) && relationship.isNotEmpty()
}

class AddContactViewModel(
    private val repository: ContactsRepository = ContactRepositoryProvider.repository
) : ViewModel() {
  private val _uiState = MutableStateFlow(AddContactUIState())
  val uiState: StateFlow<AddContactUIState> = _uiState.asStateFlow()

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /** Adds a Contact document. */
  fun addContact(): Boolean {
    val state = _uiState.value
    if (!state.isValid) {
      setErrorMsg("At least one field is not valid!")
      return false
    }
    addContactToRepository(
        Contact(
            id = repository.getNewUid(),
            fullName = state.fullName,
            phoneNumber = state.phoneNumber,
            relationship = state.relationship))
    clearErrorMsg()
    return true
  }

  private fun addContactToRepository(contact: Contact) {
    viewModelScope.launch {
      try {
        repository.addContact(contact)
      } catch (e: Exception) {
        Log.e("AddContactViewModel", "Error adding Contact", e)
        setErrorMsg("Failed to add Contact: ${e.message}")
      }
    }
  }

  // Functions to update the UI state.
  fun setFullName(fullName: String) {
    _uiState.value =
        _uiState.value.copy(
            fullName = fullName,
            invalidFullNameMsg = if (fullName.isBlank()) "Full name cannot be empty" else null)
  }

  fun setPhoneNumber(phoneNumber: String) {
    _uiState.value =
        _uiState.value.copy(
            phoneNumber = phoneNumber,
            invalidPhoneNumberMsg =
                if (!isValidPhoneNumber(phoneNumber)) "Invalid phone number" else null)
  }

  fun setRelationShip(relationship: String) {
    _uiState.value =
        _uiState.value.copy(
            relationship = relationship,
            invalidRelationshipMsg =
                if (relationship.isBlank()) "Relationship cannot be empty" else null)
  }
}
