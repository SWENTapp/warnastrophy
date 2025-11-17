package com.github.warnastrophy.core.ui.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.repository.ContactsRepository
import com.github.warnastrophy.core.domain.model.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardEmergencyContactsStatefulViewModel
@Inject
constructor(private val repository: ContactsRepository) : ViewModel() {
  private val _contactsState = MutableStateFlow<ContactCardState>(ContactCardState.Loading)
  val contactsState: StateFlow<ContactCardState> = _contactsState.asStateFlow()

  init {
    fetchContacts()
  }

  fun refreshUIStates() {
    fetchContacts()
  }

  /** Initiates the asynchronous operation to fetch all contacts from the repository. */
  private fun fetchContacts() {
    viewModelScope.launch {
      repository
          .getAllContacts()
          .onSuccess { contactList -> _contactsState.value = ContactCardState.Success(contactList) }
          .onFailure { _contactsState.value = ContactCardState.Error }
    }
  }
}

sealed interface ContactCardState {
  data object Loading : ContactCardState

  data class Success(val contacts: List<Contact>) : ContactCardState

  data object Error : ContactCardState
}
