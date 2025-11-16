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

/**
 * A Hilt ViewModel responsible for fetching and managing the state of emergency contacts in order
 * to display on the Dashboard.
 *
 * @property repository The [ContactsRepository] used to fetch contact data from the data layer.
 */
@HiltViewModel
class DashboardEmergencyContactsStatefulViewModel
@Inject
constructor(private val repository: ContactsRepository) : ViewModel() {
  private val _contactsState = MutableStateFlow<ContactCardState>(ContactCardState.Loading)
  val contactsState: StateFlow<ContactCardState> = _contactsState.asStateFlow()

  init {
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

/**
 * A sealed interface representing the distinct states of the emergency contact data that the UI can
 * be in.
 */
sealed interface ContactCardState {
  data object Loading : ContactCardState

  data class Success(val contacts: List<Contact>) : ContactCardState

  data object Error : ContactCardState
}
