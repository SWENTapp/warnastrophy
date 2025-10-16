package com.github.warnastrophy.core.ui.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.model.contact.Contact
import com.github.warnastrophy.core.model.contact.ContactsRepository
import com.github.warnastrophy.core.model.util.ContactRepositoryProvider
import com.github.warnastrophy.core.ui.profile.contact.isValidPhoneNumber
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
  fun isValidPhoneNumber(phone: String): Boolean {
    // Regex for basic validation: optional '+' at start, followed by 10-15 digits
    return phone.matches(Regex("^\\+?[0-9]{10,15}\$"))
  }

  val isValid: Boolean
    get() = fullName.isNotEmpty() && isValidPhoneNumber(phoneNumber) && relationship.isNotEmpty()
}

class EditContactViewModel(
    private val repository: ContactsRepository = ContactRepositoryProvider.repository
) : ViewModel() {
  private val _uiState = MutableStateFlow(EditContactUIState())
  val uiState: StateFlow<EditContactUIState> = _uiState.asStateFlow()

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
      try {
        val contact = repository.getContact(contactId)
        _uiState.value =
            EditContactUIState(
                fullName = contact.fullName,
                phoneNumber = contact.phoneNumber,
                relationship = contact.relationship)
      } catch (e: Exception) {
        Log.e("EditContactViewModel", "Error loading Contact by ID: $contactId", e)
        setErrorMsg("Failed to load Contact: ${e.message}")
      }
    }
  }

  /**
   * Adds a Contact document.
   *
   * @param id The contact document to be added.
   */
  fun editContact(id: String): Boolean {
    val state = _uiState.value
    if (!state.isValid) {
      setErrorMsg("At least one field is not valid")
      return false
    }
    editContactToRepository(
        id = id,
        newContact =
            Contact(
                phoneNumber = state.phoneNumber,
                fullName = state.fullName,
                relationship = state.relationship,
                id = id))
    clearErrorMsg()
    return true
  }

  private fun editContactToRepository(id: String, newContact: Contact) {
    viewModelScope.launch {
      try {
        repository.editContact(id, newContact)
      } catch (e: Exception) {
        Log.e("EditContactViewModel", "Error edit Contact", e)
        setErrorMsg("Failed to edit Contact: ${e.message}")
      }
    }
  }

  /**
   * Deletes a Contact document by its ID.
   *
   * @param contactID The ID of the Contact document to be deleted.
   */
  fun deleteContact(contactID: String) {
    viewModelScope.launch {
      try {
        repository.deleteContact(contactID)
      } catch (e: Exception) {
        Log.e("EditContactViewModel", "Error deleting Contact", e)
        setErrorMsg("Failed to delete Contact: ${e.message}")
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
