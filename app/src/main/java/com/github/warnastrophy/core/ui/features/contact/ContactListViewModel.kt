package com.github.warnastrophy.core.ui.features.contact

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.repository.ContactRepositoryProvider
import com.github.warnastrophy.core.data.repository.ContactsRepository
import com.github.warnastrophy.core.model.Contact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the complete UI state for a screen displaying a list of contacts.
 *
 * This data class is typically managed by a ViewModel and exposed to the View via a StateFlow. It
 * contains all the information needed to correctly render the UI at any given time, including the
 * data itself and any relevant error messages.
 *
 * @property contacts The current list of Contact objects to be displayed. Defaults to an empty
 *   list.
 * @property errorMsg A user-facing message explaining why the contact data might be missing or
 *   failed to load (e.g., "Network connection failed"). Null if no error.
 */
data class ContactListUIState(
    val contacts: List<Contact> = emptyList(),
    val errorMsg: String? = null
)

/**
 * ViewModel responsible for managing the UI state of the Contact List screen.
 *
 * This class acts as a communication bridge between the UI (View) and the data layer (Repository).
 * It loads contact data from the repository, handles asynchronous operations, applies business
 * logic, and exposes the result as an observable [ContactListUIState] that the UI consumes.
 *
 * @property contactsRepository The dependency responsible for fetching, caching, and persisting
 *   contacts data.
 */
class ContactListViewModel(
    private val contactsRepository: ContactsRepository = ContactRepositoryProvider.repository
) : ViewModel() {
  private val _uiState = MutableStateFlow(ContactListUIState())
  val uiState: StateFlow<ContactListUIState> = _uiState.asStateFlow()

  init {
    getAllContacts()
  }

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /** Refreshes the UI state by fetching all Contact items from the repository. */
  fun refreshUIState() {
    getAllContacts()
  }

  private fun getAllContacts() {
    viewModelScope.launch {
      val result = contactsRepository.getAllContacts()

      result.fold(
          onSuccess = { contacts -> _uiState.value = ContactListUIState(contacts = contacts) },
          onFailure = { e ->
            Log.e("ContactListViewModel", "Error fetching contacts", e)
            setErrorMsg("Failed to load contacts: ${e.message}")
          })
    }
  }
}
