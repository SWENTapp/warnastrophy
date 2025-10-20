package com.github.warnastrophy.core.ui.viewModel

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

data class ContactListUIState(
    val contacts: List<Contact> = emptyList(),
    val errorMsg: String? = null
)

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
            Log.e("OverviewViewModel", "Error fetching contacts", e)
            setErrorMsg("Failed to load contacts: ${e.message}")
          })
    }
  }
}
