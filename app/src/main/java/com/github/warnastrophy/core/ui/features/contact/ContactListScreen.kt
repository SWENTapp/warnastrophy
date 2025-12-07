package com.github.warnastrophy.core.ui.features.contact

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.R
import com.github.warnastrophy.core.model.Contact

object ContactListScreenTestTags {
  const val ADD_CONTACT_BUTTON = "addButton"
  const val CONTACT_LIST = "contactList"

  fun getTestTagForContactItem(contact: Contact): String = "contactItem${contact.id}"
}

/**
 * A composable function that renders a single contact item card, optimized for display within a
 * list (e.g., a LazyColumn).
 *
 * It displays the contact's full name, relationship, and phone number, and is configured to be
 * clickable to navigate to a detail view or initiate an action.
 *
 * @param contact The [Contact] data object to display in this item.
 * @param onContactClick The lambda to be executed when the user taps on the card (e.g., to navigate
 *   to the contact's detail screen).
 */
@Composable
private fun ContactItem(contact: Contact, onContactClick: () -> Unit) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = 4.dp, horizontal = 8.dp)
              .testTag(ContactListScreenTestTags.getTestTagForContactItem(contact = contact))
              .clickable { onContactClick() }) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween) {
              Column(modifier = Modifier.weight(1f)) {
                // Contact Name
                Text(text = contact.fullName, style = MaterialTheme.typography.titleMedium)
                // Relationship
                Text(
                    text =
                        "${stringResource(R.string.emergency_contact_relationship)}: ${contact.relationship}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
              }

              Spacer(modifier = Modifier.width(16.dp))

              // Phone Icon and Number
              Row {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = stringResource(R.string.emergency_contact_phone_number),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp).padding(end = 4.dp))
                Text(text = contact.phoneNumber, style = MaterialTheme.typography.bodyMedium)
              }
            }
      }
}

/**
 * The main screen composable for displaying a list of contacts.
 *
 * This screen observes the [ContactListUIState] from the [ContactListViewModel], handles data
 * loading, displays the list of contacts or a 'No contacts' message, and manages the floating
 * action button for adding new contacts. It also handles the display of error messages using a
 * Toast.
 *
 * @param contactListViewModel The ViewModel providing the data and state for this screen. Defaults
 *   to fetching the ViewModel using the Compose [viewModel()] function.
 * @param onContactClick Lambda executed when a contact item is clicked, typically used for
 *   navigation to a edit screen. It receives the clicked [Contact] object.
 * @param onAddButtonClick Lambda executed when the Floating Action Button ('+') is clicked,
 *   typically used for navigation to the contact list screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    contactListViewModel: ContactListViewModel = viewModel(),
    onContactClick: (Contact) -> Unit = {},
    onAddButtonClick: () -> Unit = {},
) {
  val context = LocalContext.current
  val uiState by contactListViewModel.uiState.collectAsState()
  val contacts = uiState.contacts

  LaunchedEffect(Unit) { contactListViewModel.refreshUIState() }

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { message ->
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      contactListViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      floatingActionButton = {
        FloatingActionButton(
            onClick = { onAddButtonClick() },
            modifier = Modifier.testTag(ContactListScreenTestTags.ADD_CONTACT_BUTTON)) {
              Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_button))
            }
      }) { paddingValues ->
        if (contacts.isEmpty()) {
          Column(
              modifier = Modifier.fillMaxSize().padding(paddingValues),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.emergency_contact_empty_list))
              }
        } else {
          LazyColumn(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(paddingValues)
                      .testTag(ContactListScreenTestTags.CONTACT_LIST)) {
                items(contacts, key = { it.id }) { contact ->
                  ContactItem(contact = contact, onContactClick = { onContactClick(contact) })
                  HorizontalDivider(
                      Modifier,
                      DividerDefaults.Thickness,
                      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
              }
        }
      }
}
