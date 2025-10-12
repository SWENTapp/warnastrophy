package com.github.warnastrophy.core.ui.viewModel

import androidx.lifecycle.ViewModel
import com.github.warnastrophy.core.model.contact.ContactUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Contact(
    val id: String,
    val fullName: String,
    val phoneNumber: String,
    val relationship: String
)

class ContactListViewModel() : ViewModel() {
  private val _contactListUiState = MutableStateFlow(ContactUiState())
  val uiState: StateFlow<ContactUiState> = _contactListUiState.asStateFlow()
  // TODO: implement methods to fetch contacts
}
